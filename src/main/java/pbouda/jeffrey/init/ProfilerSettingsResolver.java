package pbouda.jeffrey.init;

import pbouda.jeffrey.init.model.ProfilerMode;
import pbouda.jeffrey.init.model.ProfilerSettings;
import pbouda.jeffrey.init.model.RemoteWorkspaceSettings;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;

public class ProfilerSettingsResolver {

    private static final String WORKSPACE_SETTINGS_PREFIX = "settings-";
    private static final String WORKSPACE_SETTINGS_FILE_PATTERN = WORKSPACE_SETTINGS_PREFIX + "<<timestamp>>.json";
    private static final String WORKSPACE_SETTINGS_DIR = ".settings";

    private static final String JEFFREY_PROFILER_PATH_PLACEHOLDER = "<<JEFFREY_PROFILER_PATH>>";
    private static final String JEFFREY_JEFFREY_CURRENT_SESSION = "<<JEFFREY_CURRENT_SESSION>>";

    private static final DateTimeFormatter TIMESTAMP_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HHmmssSSSSSS").withZone(ZoneOffset.UTC);

    private static final Comparator<Path> TIMESTAMP_FILE_COMPARATOR =
            Comparator.comparing((Path path) -> {
                String filename = path.toString();
                String substring = filename.substring(filename.indexOf('-') + 1, filename.lastIndexOf('.'));
                return Instant.from(TIMESTAMP_FORMATTER.parse(substring));
            }).reversed();

    public String resolve(
            ProfilerMode profilerMode,
            String profilerCustomPath,
            String profilerCustomConfig,
            Path workspacePath,
            String projectName,
            Path currentSessionPath) {

        String config = switch (profilerMode) {
            case DIRECT -> null;
            case CUSTOM_PATH -> resolveJeffreyProfilerConfig(workspacePath, projectName);
            case CUSTOM_CONFIG -> profilerCustomConfig;
        };

        return replacePlaceholders(config, profilerCustomPath, currentSessionPath);
    }

    private static String replacePlaceholders(String config, String profilerPath, Path sessionPath) {
        if (config == null) {
            return null;
        }

        return config
                .replace(JEFFREY_PROFILER_PATH_PLACEHOLDER, profilerPath == null ? "" : profilerPath)
                .replace(JEFFREY_JEFFREY_CURRENT_SESSION, sessionPath.toString());
    }

    private static String resolveJeffreyProfilerConfig(Path workspacePath, String projectName) {
        try {
            Path settingsDir = Files.createDirectories(workspacePath.resolve(WORKSPACE_SETTINGS_DIR));
            List<Path> settingsFiles = getSettingsFiles(settingsDir);
            if (!settingsFiles.isEmpty()) {
                RemoteWorkspaceSettings settings = readSettings(settingsFiles.getFirst());
                ProfilerSettings profilerSettings = settings.profiler();
                return profilerSettings.projectSettings()
                        .getOrDefault(projectName, profilerSettings.defaultSettings());
            } else {
                throw new RuntimeException("No profiler settings files found in workspace: " + workspacePath);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static RemoteWorkspaceSettings readSettings(Path settingsFile) {
        try {
            String content = Files.readString(settingsFile);
            return Json.fromString(content, RemoteWorkspaceSettings.class);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read profiler settings file: " + settingsFile, e);
        }
    }

    private static List<Path> getSettingsFiles(Path settingsDir) {
        return FileSystemUtils.allFilesInDirectory(settingsDir).stream()
                .filter(path -> {
                    String filename = path.getFileName().toString();
                    return filename.startsWith(WORKSPACE_SETTINGS_PREFIX) && filename.endsWith(".json");
                })
                .sorted(TIMESTAMP_FILE_COMPARATOR)
                .toList();
    }
}
