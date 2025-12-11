package pbouda.jeffrey.init;

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
    private static final String WORKSPACE_SETTINGS_DIR = ".settings";

    private static final DateTimeFormatter TIMESTAMP_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HHmmssSSSSSS").withZone(ZoneOffset.UTC);

    private static final Comparator<Path> TIMESTAMP_FILE_COMPARATOR =
            Comparator.comparing((Path path) -> {
                String filename = path.toString();
                String substring = filename.substring(filename.indexOf('-') + 1, filename.lastIndexOf('.'));
                return Instant.from(TIMESTAMP_FORMATTER.parse(substring));
            }).reversed();

    public String resolve(
            String profilerPath,
            String profilerConfig,
            Path workspacePath,
            String projectName,
            Path currentSessionPath,
            String features) {

        String config;
        // Directly provided ProfilerConfig has priority over workspace settings
        if (profilerConfig != null && !profilerConfig.isBlank()) {
            config = profilerConfig;
        } else {
            config = resolveJeffreyProfilerConfig(workspacePath, projectName);
        }

        return replacePlaceholders(config, profilerPath, currentSessionPath) + " " + features;
    }

    private static String replacePlaceholders(String config, String profilerPath, Path sessionPath) {
        if (config == null) {
            return null;
        }

        return config
                .replace(Replacements.PROFILER_PATH, profilerPath == null ? "" : profilerPath)
                .replace(Replacements.CURRENT_SESSION, sessionPath.toString());
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
