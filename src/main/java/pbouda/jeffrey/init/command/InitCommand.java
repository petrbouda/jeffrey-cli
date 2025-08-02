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
    private static final String JEFFREY_HOME_DIR_PROP = "JEFFREY_HOME_DIR";
    private static final String JEFFREY_REPOSITORIES_DIR_PROP = "JEFFREY_REPOSITORIES_DIR";
    private static final String JEFFREY_SESSION_DIR_PROP = "JEFFREY_CURRENT_SESSION_DIR";
    private static final String JEFFREY_PROJECT_DIR_PROP = "JEFFREY_CURRENT_PROJECT_DIR";
    private static final String JEFFREY_FILE_PROP = "JEFFREY_FILE";

    @Option(names = {"--silent"}, description = "Suppress output. Only create the variable without printing the output for sourcing.")
    private boolean silent = false;

    @Option(names = {"--jeffrey-home-dir"}, description = "Jeffrey HOME directory path. Automatically creates repositories dir in Jeffrey home (Otherwise, repositories-dir must be provided).")
    private String jeffreyHomeDir;

    @Option(names = {"--repositories-dir"}, description = "Repositories directory path. It's taken as a directory for storing projects' sessions data (Otherwise, jeffrey-home-dir must be provided).")
    private String repositoriesDir;

    @Option(names = {"--project-name"}, description = "Project name", required = true)
    private String projectName;

    @Override
    public void run() {
        validateArguments();

        boolean useJeffreyHomeDir = jeffreyHomeDir != null;
        
        Path jeffreyDir;
        Path repositoriesDirPath;

        if (useJeffreyHomeDir) {
            jeffreyDir = createDirectories(Path.of(jeffreyHomeDir));
            repositoriesDirPath = createDirectories(jeffreyDir.resolve(REPOSITORIES_DIR_NAME));
        } else {
            repositoriesDirPath = createDirectories(Path.of(repositoriesDir));
            jeffreyDir = null; // Will not be used when repositories_dir is specified
        }

        if (!repositoriesDirPath.toFile().exists()) {
            System.err.println("[ERROR] Cannot create parent directories: " + repositoriesDirPath);
            System.exit(1);
        }

        try {
            Path projectDir = createDirectories(repositoriesDirPath.resolve(projectName));
            Path newSessionDir = createNewSessionDir(projectDir);
            String variables = variables(jeffreyDir, repositoriesDirPath, projectDir, newSessionDir, useJeffreyHomeDir);
            Path envFile = createEnvFile(projectDir, variables);
            if (!silent) {
                System.out.println("# ENV file to with variables to source: ");
                System.out.println("# " + envFile);
                System.out.println(Files.readString(envFile));
            }
        } catch (Exception e) {
            System.err.println("[ERROR] Cannot create a new directory and env-file: " + repositoriesDirPath + " error=" + e.getMessage());
            System.exit(1);
        }
    }

    private void validateArguments() {
        if (jeffreyHomeDir == null && repositoriesDir == null) {
            System.err.println("[ERROR] Either --jeffrey-home-dir or --repositories-dir must be specified");
            System.exit(1);
        }

        if (jeffreyHomeDir != null && repositoriesDir != null) {
            System.err.println("[ERROR] Cannot specify both --jeffrey-home-dir and --repositories-dir");
            System.exit(1);
        }
    }

    private static Path createNewSessionDir(Path projectDir) {
        Instant currentTimestamp = Instant.now();
        String sessionName = currentTimestamp.atZone(ZoneOffset.UTC).format(DATETIME_FORMATTER);
        return createDirectories(projectDir.resolve(sessionName));
    }

    private static Path createEnvFile(Path projectDir, String variables) {
        Path envFilePath = projectDir.resolve(ENV_FILE_NAME);
        try {
            return Files.writeString(envFilePath, variables);
        } catch (IOException e) {
            System.err.println("[ERROR] Cannot create an ENV file: path=" + envFilePath + " error=" + e.getMessage());
            System.exit(1);
            return null; // Unreachable, but required for compilation
        }
    }

    private static String variables(
            Path jeffreyDir, Path repositoriesDir, Path projectDir, Path sessionDir, boolean useJeffreyHomeDir) {

        String output = "";
        if (useJeffreyHomeDir) {
            output += var(JEFFREY_HOME_DIR_PROP, jeffreyDir);
        }
        output += var(JEFFREY_REPOSITORIES_DIR_PROP, repositoriesDir);
        output += var(JEFFREY_PROJECT_DIR_PROP, projectDir);
        output += var(JEFFREY_SESSION_DIR_PROP, sessionDir);
        output += var(JEFFREY_FILE_PROP, sessionDir.resolve(DEFAULT_FILE_TEMPLATE), false);
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
