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

    public static void main(String[] args) {
        if (args.length == 0) {
            System.err.println("[ERROR] Repository folder is not provided");
            System.exit(1);
        }
        if (args.length > 2) {
            System.err.println("[ERROR] Too many arguments, expected: <repository-folder> [--silent]");
            System.exit(1);
        }

        boolean silent = args.length == 2 && "--silent".equals(args[1]);
        Path repositoryFolder = createDirectories(Path.of(args[0]));
        if (!repositoryFolder.toFile().exists()) {
            System.err.println("[ERROR] Cannot create parent directories: " + repositoryFolder);
            System.exit(1);
        }

        try {
            Path newFolder = createNewProjectDir(repositoryFolder);
            Path envFile = createEnvFile(repositoryFolder, newFolder);
            setEnvironmentVariables(repositoryFolder, newFolder);
            if (!silent) {
                System.out.printf(
                        """
                                Jeffrey directory and env file prepared:
                                JEFFREY_REPOSITORY_DIR=%s
                                JEFFREY_PROFILE_DIR=%s
                                JEFFREY_PROFILE_FILE=%s
                                ENV_FILE=%s%n""",
                        System.getProperty("JEFFREY_REPOSITORY_DIR"),
                        System.getProperty("JEFFREY_PROFILE_DIR"),
                        System.getProperty("JEFFREY_PROFILE_FILE"),
                        envFile
                );
            }
        } catch (Exception e) {
            System.err.println("[ERROR] Cannot create a new directory and env-file: " + repositoryFolder);
            System.exit(1);
        }
    }

    private static Path createNewProjectDir(Path repositoryFolder) {
        Instant currenTimestamp = Instant.now();
        String folderName = currenTimestamp.atZone(ZoneOffset.UTC).format(DATETIME_FORMATTER);
        return createDirectories(repositoryFolder.resolve(folderName));
    }

    private static Path createEnvFile(Path repositoryFolder, Path newFolder) {
        String content = """
                export JEFFREY_REPOSITORY_DIR=%s
                export JEFFREY_PROFILE_DIR=%s
                export JEFFREY_PROFILE_FILE=%s
                """.formatted(
                repositoryFolder,
                newFolder,
                newFolder.resolve(DEFAULT_FILE_TEMPLATE));

        Path envFilePath = repositoryFolder.resolve(ENV_FILE_NAME);
        try {
            return Files.writeString(envFilePath, content);
        } catch (IOException e) {
            System.err.println("[ERROR] Cannot create an ENV file: path=" + envFilePath + " error=" + e.getMessage());
            System.exit(1);
            return null; // Unreachable, but required for compilation
        }
    }

    private static void setEnvironmentVariables(Path repositoryFolder, Path newFolder) {
        System.setProperty("JEFFREY_REPOSITORY_DIR", repositoryFolder.toString());
        System.setProperty("JEFFREY_PROFILE_DIR", newFolder.toString());
        System.setProperty("JEFFREY_PROFILE_FILE", newFolder.resolve(DEFAULT_FILE_TEMPLATE).toString());
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
