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
    private static final String PROJECTS_DIR_NAME = "projects";
    private static final String JEFFREY_DIR_PROP = "JEFFREY_DIR";
    private static final String JEFFREY_PROJECTS_DIR_PROP = "JEFFREY_PROJECTS_DIR";
    private static final String JEFFREY_SESSION_DIR_PROP = "JEFFREY_SESSION_DIR";
    private static final String JEFFREY_FILE_PROP = "JEFFREY_FILE";
    private static final String SILENT_FLAG = "--silent";

    public static void main(String[] args) {
        if (args.length == 0) {
            System.err.println("[ERROR] Jeffrey directory is not provided");
            System.exit(1);
        }
        if (args.length > 2) {
            System.err.println("[ERROR] Too many arguments, expected: <repository-folder> [--silent]");
            System.exit(1);
        }

        boolean silent = args.length == 2 && SILENT_FLAG.equals(args[1]);
        Path jeffreyDir = createDirectories(Path.of(args[0]));
        if (!jeffreyDir.toFile().exists()) {
            System.err.println("[ERROR] Cannot create parent directories: " + jeffreyDir);
            System.exit(1);
        }

        try {
            Path projectsDir = createDirectories(jeffreyDir.resolve(PROJECTS_DIR_NAME));
            Path newSessionDir = createNewSessionDir(jeffreyDir);
            Path envFile = createEnvFile(jeffreyDir, projectsDir, newSessionDir);
            setEnvironmentVariables(jeffreyDir, projectsDir, newSessionDir);
            if (!silent) {
                String output = """
                        Jeffrey directory and env file prepared:
                        %s=%s
                        %s=%s
                        %s=%s
                        %s=%s
                        ENV_FILE=%s""".formatted(
                        JEFFREY_DIR_PROP, System.getProperty(JEFFREY_DIR_PROP),
                        JEFFREY_PROJECTS_DIR_PROP, System.getProperty(JEFFREY_PROJECTS_DIR_PROP),
                        JEFFREY_SESSION_DIR_PROP, System.getProperty(JEFFREY_SESSION_DIR_PROP),
                        JEFFREY_FILE_PROP, System.getProperty(JEFFREY_FILE_PROP),
                        envFile
                );
                System.out.println(output);
            }
        } catch (Exception e) {
            System.err.println("[ERROR] Cannot create a new directory and env-file: " + jeffreyDir + " error=" + e.getMessage());
            System.exit(1);
        }
    }

    private static Path createNewSessionDir(Path projectsDir) {
        Instant currenTimestamp = Instant.now();
        String sessionName = currenTimestamp.atZone(ZoneOffset.UTC).format(DATETIME_FORMATTER);
        return createDirectories(projectsDir.resolve(sessionName));
    }

    private static Path createEnvFile(Path jeffreyDir, Path projectsDir, Path sessionDir) {
        String content = """
                export %s=%s
                export %s=%s
                export %s=%s
                export %s=%s
                """.formatted(
                JEFFREY_DIR_PROP,
                JEFFREY_PROJECTS_DIR_PROP,
                JEFFREY_SESSION_DIR_PROP,
                JEFFREY_FILE_PROP,
                jeffreyDir,
                projectsDir,
                sessionDir,
                sessionDir.resolve(DEFAULT_FILE_TEMPLATE));

        Path envFilePath = projectsDir.resolve(ENV_FILE_NAME);
        try {
            return Files.writeString(envFilePath, content);
        } catch (IOException e) {
            System.err.println("[ERROR] Cannot create an ENV file: path=" + envFilePath + " error=" + e.getMessage());
            System.exit(1);
            return null; // Unreachable, but required for compilation
        }
    }

    private static void setEnvironmentVariables(Path jeffreyDir, Path projectsDir, Path sessionDir) {
        System.setProperty(JEFFREY_DIR_PROP, jeffreyDir.toString());
        System.setProperty(JEFFREY_PROJECTS_DIR_PROP, projectsDir.toString());
        System.setProperty(JEFFREY_SESSION_DIR_PROP, sessionDir.toString());
        System.setProperty(JEFFREY_FILE_PROP, sessionDir.resolve(DEFAULT_FILE_TEMPLATE).toString());
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
