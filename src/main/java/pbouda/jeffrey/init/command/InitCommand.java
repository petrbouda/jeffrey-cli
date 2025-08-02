package pbouda.jeffrey.init.command;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@Command(
        name = InitCommand.COMMAND_NAME,
        description = "Initialize Jeffrey project structure and generate environment variables for sourcing.",
        mixinStandardHelpOptions = true)
public class InitCommand implements Runnable {

    public static final String COMMAND_NAME = "init";

    private static final DateTimeFormatter DATETIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd-HHmmss");

    private static final String DEFAULT_FILE_TEMPLATE = "profile-%t.jfr";
    private static final String ENV_FILE_NAME = ".env";
    private static final String REPOSITORIES_DIR_NAME = "repositories";
    private static final String JEFFREY_HOME_PROP = "JEFFREY_HOME";
    private static final String JEFFREY_REPOSITORIES_PROP = "JEFFREY_REPOSITORIES";
    private static final String JEFFREY_SESSION_PROP = "JEFFREY_CURRENT_SESSION";
    private static final String JEFFREY_PROJECT_PROP = "JEFFREY_CURRENT_PROJECT";
    private static final String JEFFREY_FILE_PROP = "JEFFREY_FILE";

    @Option(names = {"--silent"}, description = "Suppress output. Only create the variable without printing the output for sourcing.")
    private boolean silent = false;

    @Option(names = {"--jeffrey-home"}, description = "Jeffrey HOME directory path. Automatically creates repositories directory in Jeffrey home (Otherwise, --repositories must be provided).")
    private String jeffreyHomePath;

    @Option(names = {"--repositories"}, description = "Repositories directory path. It's taken as a directory for storing projects' sessions data (Otherwise, --jeffrey-home must be provided).")
    private String repositoriesPath;

    @Option(names = {"--project"}, description = "Project that will be used to a new generate directories for repository and a new session", required = true)
    private String projectName;

    @Override
    public void run() {
        validateArguments();

        boolean useJeffreyHome = jeffreyHomePath != null;
        
        Path jeffreyHome;
        Path repositoriesPath;

        if (useJeffreyHome) {
            jeffreyHome = createDirectories(Path.of(this.jeffreyHomePath));
            repositoriesPath = createDirectories(jeffreyHome.resolve(REPOSITORIES_DIR_NAME));
        } else {
            repositoriesPath = createDirectories(Path.of(this.repositoriesPath));
            jeffreyHome = null; // Will not be used when repositories_dir is specified
        }

        if (!repositoriesPath.toFile().exists()) {
            System.err.println("[ERROR] Cannot create parent directories: " + repositoriesPath);
            System.exit(1);
        }

        try {
            Path projectPath = createDirectories(repositoriesPath.resolve(projectName));
            Path newSessionPath = createNewSessionPath(projectPath);
            String variables = variables(jeffreyHome, repositoriesPath, projectPath, newSessionPath, useJeffreyHome);
            Path envFile = createEnvFile(projectPath, variables);
            if (!silent) {
                System.out.println("# ENV file to with variables to source: ");
                System.out.println("# " + envFile);
                System.out.println(Files.readString(envFile));
            }
        } catch (Exception e) {
            System.err.println("[ERROR] Cannot create a new directory and env-file: " + repositoriesPath + " error=" + e.getMessage());
            System.exit(1);
        }
    }

    private void validateArguments() {
        if (jeffreyHomePath == null && repositoriesPath == null) {
            System.err.println("[ERROR] Either --jeffrey-home or --repositories must be specified");
            System.exit(1);
        }

        if (jeffreyHomePath != null && repositoriesPath != null) {
            System.err.println("[ERROR] Cannot specify both --jeffrey-home and --repositories");
            System.exit(1);
        }
    }

    private static Path createNewSessionPath(Path projectPath) {
        Instant currentTimestamp = Instant.now();
        String sessionName = currentTimestamp.atZone(ZoneOffset.UTC).format(DATETIME_FORMATTER);
        return createDirectories(projectPath.resolve(sessionName));
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
            Path jeffreyHome, Path repositoriesPath, Path projectPath, Path sessionPath, boolean useJeffreyHome) {

        String output = "";
        if (useJeffreyHome) {
            output += var(JEFFREY_HOME_PROP, jeffreyHome);
        }
        output += var(JEFFREY_REPOSITORIES_PROP, repositoriesPath);
        output += var(JEFFREY_PROJECT_PROP, projectPath);
        output += var(JEFFREY_SESSION_PROP, sessionPath);
        output += var(JEFFREY_FILE_PROP, sessionPath.resolve(DEFAULT_FILE_TEMPLATE), false);
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
}
