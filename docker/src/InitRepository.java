import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

public class InitRepository {
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
    private static final String SILENT_FLAG = "--silent";

    public static void main(String[] args) {
        if (args.length == 0) {
            System.err.println("[ERROR] Arguments not provided");
            System.exit(1);
        }

        ArgumentParser parser = new ArgumentParser(args);
        boolean silent = parser.isSilent();
        boolean useJeffreyHomeDir = parser.hasJeffreyHomeDir();

        Path jeffreyDir;
        Path repositoriesDir;

        if (useJeffreyHomeDir) {
            jeffreyDir = createDirectories(Path.of(parser.getJeffreyHomeDir()));
            repositoriesDir = createDirectories(jeffreyDir.resolve(REPOSITORIES_DIR_NAME));
        } else {
            repositoriesDir = createDirectories(Path.of(parser.getRepositoriesDir()));
            jeffreyDir = null; // Will not be used when repositories_dir is specified
        }

        if (!repositoriesDir.toFile().exists()) {
            System.err.println("[ERROR] Cannot create parent directories: " + repositoriesDir);
            System.exit(1);
        }

        try {
            Path projectDir = createDirectories(repositoriesDir.resolve(parser.getProjectName()));
            Path newSessionDir = createNewSessionDir(projectDir);
            String variables = variables(jeffreyDir, repositoriesDir, projectDir, newSessionDir, useJeffreyHomeDir);
            Path envFile = createEnvFile(repositoriesDir, variables);
            if (!silent) {
                System.out.println("ENV file to with variables to source: ");
                System.out.println(envFile);
                System.out.println("Content of the ENV file:");
                System.out.println(Files.readString(envFile));
            }
        } catch (Exception e) {
            System.err.println("[ERROR] Cannot create a new directory and env-file: " + repositoriesDir + " error=" + e.getMessage());
            System.exit(1);
        }
    }

    private static Path createNewSessionDir(Path repositoriesDir) {
        Instant currenTimestamp = Instant.now();
        String sessionName = currenTimestamp.atZone(ZoneOffset.UTC).format(DATETIME_FORMATTER);
        return createDirectories(repositoriesDir.resolve(sessionName));
    }

    private static Path createEnvFile(Path repositoriesDir, String variables) {
        Path envFilePath = repositoriesDir.resolve(ENV_FILE_NAME);
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
        output += var(JEFFREY_FILE_PROP, sessionDir.resolve(DEFAULT_FILE_TEMPLATE));
        return output;
    }

    private static String var(String name, Path value) {
        return "export " + name + "=" + value + "\n";
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

    private static class ArgumentParser {
        private boolean silent = false;
        private boolean hasJeffreyHomeDir = false;
        private String jeffreyHomeDir;
        private String repositoriesDir;
        private String projectName;

        public ArgumentParser(String[] args) {
            parseArguments(args);
        }

        private void parseArguments(String[] args) {
            for (String arg : args) {
                if (arg.equals(SILENT_FLAG)) {
                    this.silent = true;
                } else if (arg.startsWith("--jeffrey-home-dir=")) {
                    this.hasJeffreyHomeDir = true;
                    this.jeffreyHomeDir = arg.substring("--jeffrey-home-dir=".length());
                    if (this.jeffreyHomeDir.isEmpty()) {
                        System.err.println("[ERROR] --jeffrey-home-dir requires a value");
                        System.exit(1);
                    }
                } else if (arg.startsWith("--repositories-dir=")) {
                    this.repositoriesDir = arg.substring("--repositories-dir=".length());
                    if (this.repositoriesDir.isEmpty()) {
                        System.err.println("[ERROR] --repositories-dir requires a value");
                        System.exit(1);
                    }
                } else if (arg.startsWith("--project-name=")) {
                    this.projectName = arg.substring("--project-name=".length());
                    if (this.projectName.isEmpty()) {
                        System.err.println("[ERROR] --project-name requires a value");
                        System.exit(1);
                    }
                } else {
                    System.err.println("[ERROR] Unknown argument: " + arg);
                    System.exit(1);
                }
            }

            if (projectName == null) {
                System.err.println("[ERROR] --project-name is required");
                System.exit(1);
            }

            if (!hasJeffreyHomeDir && repositoriesDir == null) {
                System.err.println("[ERROR] Either --jeffrey-home-dir or --repositories-dir must be specified");
                System.exit(1);
            }

            if (hasJeffreyHomeDir && repositoriesDir != null) {
                System.err.println("[ERROR] Cannot specify both --jeffrey-home-dir and --repositories-dir");
                System.exit(1);
            }
        }

        public boolean isSilent() {
            return silent;
        }

        public boolean hasJeffreyHomeDir() {
            return hasJeffreyHomeDir;
        }

        public String getJeffreyHomeDir() {
            return jeffreyHomeDir;
        }

        public String getRepositoriesDir() {
            return repositoriesDir;
        }

        public String getProjectName() {
            return projectName;
        }
    }
}
