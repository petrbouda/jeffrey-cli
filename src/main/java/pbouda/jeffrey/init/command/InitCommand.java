package pbouda.jeffrey.init.command;

import pbouda.jeffrey.init.FileSystemRepository;
import pbouda.jeffrey.init.IDGenerator;
import pbouda.jeffrey.init.ProfilerSettingsResolver;
import pbouda.jeffrey.init.model.*;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Command(
        name = InitCommand.COMMAND_NAME,
        description = "Initialize Jeffrey project and current session. Creates a ENV file with variables to source in the shell.",
        mixinStandardHelpOptions = true)
public class InitCommand implements Runnable {

    private static final Clock CLOCK = Clock.systemUTC();

    public static final String COMMAND_NAME = "init";

    private static final String DEFAULT_FILE_TEMPLATE = "profile-%t.jfr";
    private static final String ENV_FILE_NAME = ".env";
    private static final String WORKSPACES_DIR_NAME = "workspaces";
    private static final String JEFFREY_HOME_PROP = "JEFFREY_HOME";
    private static final String JEFFREY_WORKSPACES_PROP = "JEFFREY_WORKSPACES";
    private static final String JEFFREY_WORKSPACE_PROP = "JEFFREY_CURRENT_WORKSPACE";
    private static final String JEFFREY_SESSION_PROP = "JEFFREY_CURRENT_SESSION";
    private static final String JEFFREY_PROJECT_PROP = "JEFFREY_CURRENT_PROJECT";
    private static final String JEFFREY_FILE_PATTERN_PROP = "JEFFREY_FILE_PATTERN";
    private static final String JEFFREY_PROFILER_CONFIG_PROP = "JEFFREY_PROFILER_CONFIG";

    private static final ProfilerSettingsResolver PROFILER_SETTINGS_RESOLVER = new ProfilerSettingsResolver();

    @Option(names = {"--silent"}, description = "Suppress output. Only create the variable without printing the output for sourcing.")
    private boolean silent = false;

    @Option(names = {"--jeffrey-home"}, description = "Jeffrey HOME directory path. Automatically creates 'workspaces' directory in Jeffrey home (Otherwise, --workspaces-dir must be provided).")
    private String jeffreyHomePath;

    @Option(names = {"--workspaces-dir"}, description = "Workspaces directory path. It's taken as a directory for storing projects' sessions data (Otherwise, --jeffrey-home must be provided).")
    private String workspacesDir;

    @Option(names = {"--workspace-id"}, description = "Workspace ID, where the project belongs to.", required = true)
    private String workspaceId;

    @Option(names = {"--project-name"}, description = "Project Name should be a unique identifier for the given project to know that a deployment belongs to the particular service", required = true)
    private String projectName;

    @Option(names = {"--project-label"}, description = "Human-readable label of the project", required = true)
    private String projectLabel;

    @Option(names = {"--attribute"}, description = "Key-value pair attributes delimited by slash ('/') to be added to the project. Can be specified multiple times.")
    private String[] attributes;

    @Option(names = {"--profiler-mode"}, description = "Mode of providing path and configuration of profiler", defaultValue = "DIRECT", converter = ProfilerModeConverter.class)
    private ProfilerMode profilerMode;

    @Option(names = {"--profiler-custom-path"}, description = "Custom path to the profiler agent (used only with PROFILER_MODE=CUSTOM_PATH)")
    private String profilerCustomPath;

    @Option(names = {"--profiler-custom-config"}, description = "Custom profiler configuration (used only with PROFILER_MODE=CUSTOM_CONFIG)")
    private String profilerCustomConfig;

    @Option(names = {"--repository-type"}, description = "Type of repository for the project (ASPROF or JDK)", defaultValue = "ASPROF", converter = RepositoryTypeConverter.class)
    private RepositoryType repositoryType;

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
            workspacesPath = createDirectories(Path.of(this.workspacesDir));
            jeffreyHome = null; // Will not be used when workspacesPath is specified
        }

        if (!workspacesPath.toFile().exists()) {
            System.err.println("[ERROR] Cannot create parent directories: " + workspacesPath);
            System.exit(1);
        }

        try {
            Path workspacePath = createDirectories(workspacesPath.resolve(workspaceId));

            // Initialize filesystem repository for managing project/session data
            FileSystemRepository repository = new FileSystemRepository(CLOCK);

            String projectId;
            Path projectPath = workspacePath.resolve(projectName);

            Optional<RemoteProject> projectOpt = repository.findProject(projectPath);
            if (projectOpt.isPresent()) {
                projectId = projectOpt.get().projectId();
            } else {
                // Create new project
                projectId = IDGenerator.generate();

                createDirectories(projectPath);
                repository.addProject(
                        projectId, projectName, projectLabel, workspaceId,
                        repositoryType, parseAttributes(attributes), projectPath);
            }

            String sessionId = IDGenerator.generate();
            Path newSessionPath = createDirectories(projectPath.resolve(sessionId));

            String profilerSettings = PROFILER_SETTINGS_RESOLVER.resolve(
                    profilerMode,
                    profilerCustomPath,
                    profilerCustomConfig,
                    workspacePath,
                    projectName,
                    newSessionPath);

            // Add session
            repository.addSession(
                    sessionId,
                    projectId,
                    workspaceId,
                    workspacePath.relativize(newSessionPath),
                    useJeffreyHome ? null : workspacesPath,
                    newSessionPath,
                    profilerSettings);

            String variables = variables(
                    jeffreyHome,
                    workspacesPath,
                    workspacePath,
                    projectPath,
                    newSessionPath,
                    profilerSettings,
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
        if (jeffreyHomePath == null && workspacesDir == null) {
            System.err.println("[ERROR] Either --jeffrey-home or --workspaces must be specified");
            System.exit(1);
        }

        if (jeffreyHomePath != null && workspacesDir != null) {
            System.err.println("[ERROR] Cannot specify both --jeffrey-home and --workspaces");
            System.exit(1);
        }

        if (projectName != null && !projectName.matches("^[a-zA-Z0-9_-]+$")) {
            System.err.println("[ERROR] Project name can only contain alphanumeric characters, underscores, and dashes");
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
            String profilerSettings,
            boolean useJeffreyHome) {

        String output = "";
        if (useJeffreyHome) {
            output += var(JEFFREY_HOME_PROP, jeffreyHome);
        }
        output += var(JEFFREY_WORKSPACES_PROP, workspacesPath);
        output += var(JEFFREY_WORKSPACE_PROP, workspacePath);
        output += var(JEFFREY_PROJECT_PROP, projectPath);
        output += var(JEFFREY_SESSION_PROP, sessionPath);
        output += var(JEFFREY_FILE_PATTERN_PROP, sessionPath.resolve(DEFAULT_FILE_TEMPLATE));
        if (profilerSettings != null && !profilerSettings.isEmpty()) {
            output += var(JEFFREY_PROFILER_CONFIG_PROP, profilerSettings, false);
        }
        return output;
    }

    private static String var(String name, Path value) {
        return var(name, value.toString(), true);
    }

    private static String var(String name, String value, boolean addNewLine) {
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

    private static Map<String, String> parseAttributes(String[] keyValuePairs) {
        Map<String, String> attributes = new HashMap<>();
        if (keyValuePairs != null) {
            for (String attribute : keyValuePairs) {
                String[] parts = attribute.split("/", 2);
                if (parts.length == 2) {
                    attributes.put(parts[0], parts[1]);
                } else {
                    System.err.println("[WARNING] Invalid attribute format: " + attribute + " (expected: key/value)");
                }
            }
        }
        return attributes;
    }
}
