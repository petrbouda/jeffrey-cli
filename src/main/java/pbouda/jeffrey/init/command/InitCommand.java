package pbouda.jeffrey.init.command;

import pbouda.jeffrey.init.Repository;
import pbouda.jeffrey.init.model.ProjectAttribute;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Command(
        name = InitCommand.COMMAND_NAME,
        description = "Initialize Jeffrey project structure and generate environment variables for sourcing.",
        mixinStandardHelpOptions = true)
public class InitCommand implements Runnable {

    private static final Clock CLOCK = Clock.systemUTC();

    public static final String COMMAND_NAME = "init";

    private static final DateTimeFormatter DATETIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd-HHmmss");

    private static final String DEFAULT_FILE_TEMPLATE = "profile-%t.jfr";
    private static final String ENV_FILE_NAME = ".env";
    private static final String WORKSPACES_DIR_NAME = "workspaces";
    private static final String JEFFREY_HOME_PROP = "JEFFREY_HOME";
    private static final String JEFFREY_WORKSPACES_PROP = "JEFFREY_WORKSPACES";
    private static final String JEFFREY_WORKSPACE_PROP = "JEFFREY_CURRENT_WORKSPACE";
    private static final String JEFFREY_SESSION_PROP = "JEFFREY_CURRENT_SESSION";
    private static final String JEFFREY_PROJECT_PROP = "JEFFREY_CURRENT_PROJECT";
    private static final String JEFFREY_FILE_PATTERN_PROP = "JEFFREY_FILE_PATTERN";

    @Option(names = {"--silent"}, description = "Suppress output. Only create the variable without printing the output for sourcing.")
    private boolean silent = false;

    @Option(names = {"--jeffrey-home"}, description = "Jeffrey HOME directory path. Automatically creates 'workspaces' directory in Jeffrey home (Otherwise, --workspaces must be provided).")
    private String jeffreyHomePath;

    @Option(names = {"--workspaces-dir"}, description = "Workspaces directory path. It's taken as a directory for storing projects' sessions data (Otherwise, --jeffrey-home must be provided).")
    private String workspacesPath;

    @Option(names = {"--workspace"}, description = "Workspace name, where the project belongs to.", required = true)
    private String workspace;

    @Option(names = {"--project-id"}, description = "Project ID should be a unique identifier for the given project to know that a deployment belongs to the particular service", required = true)
    private String projectId;

    @Option(names = {"--project-name"}, description = "Human-readable name of the project", required = true)
    private String projectName;

    @Option(names = {"--attribute"}, description = "Key-value pair attributes delimited by slash ('/') to be added to the project. Can be specified multiple times.")
    private String[] attributes;

    @Override
    public void run() {
        validateArguments();

        boolean useJeffreyHome = jeffreyHomePath != null;

        Path jeffreyHome;
        Path workspacesPath;

        if (useJeffreyHome) {
            jeffreyHome = createDirectories(Path.of(this.jeffreyHomePath));
            workspacesPath = createDirectories(jeffreyHome.resolve(WORKSPACES_DIR_NAME));
        } else {
            workspacesPath = createDirectories(Path.of(this.workspacesPath));
            jeffreyHome = null; // Will not be used when repositories_dir is specified
        }

        if (!workspacesPath.toFile().exists()) {
            System.err.println("[ERROR] Cannot create parent directories: " + workspacesPath);
            System.exit(1);
        }

        try {
            Path workspacePath = createDirectories(workspacesPath.resolve(workspace));
            Path projectPath = createDirectories(workspacePath.resolve(projectId));

            String sessionId = generateSessionId();
            Path newSessionPath = projectPath.resolve(sessionId);

            // Initialize repository and manage project/session data
            Path dbPath = workspacePath.resolve("workspace.db");
            Repository repository = new Repository(dbPath, CLOCK);
            repository.initialize();

            // Parse attributes
            List<ProjectAttribute> projectAttributes = parseAttributes(attributes);

            // Add project if it doesn't exist
            if (!repository.projectExists(projectId)) {
                repository.addProject(projectId, projectName, projectPath, projectAttributes);
            }

            // Add session
            repository.addSession(projectId, sessionId, newSessionPath);

            String variables = variables(
                    jeffreyHome,
                    workspacesPath,
                    workspacePath,
                    projectPath,
                    newSessionPath,
                    useJeffreyHome);

            Path envFile = createEnvFile(projectPath, variables);
            if (!silent) {
                System.out.println("# ENV file to with variables to source: ");
                System.out.println("# " + envFile);
                System.out.println(Files.readString(envFile));
            }
        } catch (Exception e) {
            System.err.println("[ERROR] Cannot create a new directory and env-file: " + workspacesPath + " error=" + e.getMessage());
            System.exit(1);
        }
    }

    private void validateArguments() {
        if (jeffreyHomePath == null && workspacesPath == null) {
            System.err.println("[ERROR] Either --jeffrey-home or --workspaces must be specified");
            System.exit(1);
        }

        if (jeffreyHomePath != null && workspacesPath != null) {
            System.err.println("[ERROR] Cannot specify both --jeffrey-home and --workspaces");
            System.exit(1);
        }
    }

    private static Path createEnvFile(Path projectPath, String variables) {
        Path envFilePath = projectPath.resolve(ENV_FILE_NAME);
        try {
            return Files.writeString(envFilePath, variables);
        } catch (IOException e) {
            System.err.println("[ERROR] Cannot create an ENV file: path=" + envFilePath + " error=" + e.getMessage());
            System.exit(1);
            return null; // Unreachable, but required for compilation
        }
    }

    private static String variables(
            Path jeffreyHome,
            Path workspacesPath,
            Path workspacePath,
            Path projectPath,
            Path sessionPath,
            boolean useJeffreyHome) {

        String output = "";
        if (useJeffreyHome) {
            output += var(JEFFREY_HOME_PROP, jeffreyHome);
        }
        output += var(JEFFREY_WORKSPACES_PROP, workspacesPath);
        output += var(JEFFREY_WORKSPACE_PROP, workspacePath);
        output += var(JEFFREY_PROJECT_PROP, projectPath);
        output += var(JEFFREY_SESSION_PROP, sessionPath);
        output += var(JEFFREY_FILE_PATTERN_PROP, sessionPath.resolve(DEFAULT_FILE_TEMPLATE), false);
        return output;
    }

    private static String var(String name, Path value) {
        return var(name, value, true);
    }

    private static String var(String name, Path value, boolean addNewLine) {
        return "export " + name + "=" + value + (addNewLine ? "\n" : "");
    }

    private static Path createDirectories(Path path) {
        try {
            return Files.exists(path) ? path : Files.createDirectories(path);
        } catch (IOException e) {
            System.err.println("[ERROR] Cannot create a parent directories: " + path + " error=" + e.getMessage());
            System.exit(1);
            return null; // Unreachable, but required for compilation
        }
    }

    private static List<ProjectAttribute> parseAttributes(String[] attributes) {
        List<ProjectAttribute> result = new ArrayList<>();
        if (attributes != null) {
            for (String attribute : attributes) {
                String[] parts = attribute.split("/", 2);
                if (parts.length == 2) {
                    result.add(new ProjectAttribute(parts[0], parts[1]));
                } else {
                    System.err.println("[WARNING] Invalid attribute format: " + attribute + " (expected: key/value)");
                }
            }
        }
        return result;
    }

    private static String generateSessionId() {
        return CLOCK.instant().atZone(ZoneOffset.UTC).format(DATETIME_FORMATTER);
    }
}
