package pbouda.jeffrey.init;

import pbouda.jeffrey.init.model.RemoteProject;
import pbouda.jeffrey.init.model.RepositoryType;
import pbouda.jeffrey.init.model.RemoteSession;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.util.Map;
import java.util.Optional;

public class FileSystemRepository {

    private static final String PROJECT_INFO_FILENAME = ".project-info.json";
    private static final String SESSION_INFO_FILENAME = ".session-info.json";

    private final Clock clock;

    public FileSystemRepository(Clock clock) {
        this.clock = clock;
    }

    public void addProject(
            String projectId,
            String projectName,
            String projectLabel,
            String workspaceId,
            RepositoryType repositoryType,
            Map<String, String> attributes,
            Path projectPath) {
        try {
            RemoteProject project = new RemoteProject(
                    projectId,
                    projectName,
                    projectLabel,
                    workspaceId,
                    clock.instant().toEpochMilli(),
                    repositoryType,
                    attributes);

            Path projectInfoFile = projectPath.resolve(PROJECT_INFO_FILENAME);
            Files.writeString(projectInfoFile, Json.toString(project));
        } catch (IOException e) {
            throw new RuntimeException("Failed to write project info for project: " + projectId, e);
        }
    }

    public void addSession(
            String sessionId,
            String projectId,
            String workspaceId,
            Path sessionRelativePath,
            Path workspacesPath,
            Path sessionPath) {
        try {
            RemoteSession session = new RemoteSession(
                    sessionId,
                    projectId,
                    workspaceId,
                    clock.instant().toEpochMilli(),
                    sessionRelativePath.toString(),
                    workspacesPath != null ? workspacesPath.toString() : null);

            Path sessionInfoFile = sessionPath.resolve(SESSION_INFO_FILENAME);
            Files.writeString(sessionInfoFile, Json.toString(session));
        } catch (IOException e) {
            throw new RuntimeException("Failed to write session info for session: " + sessionId + " in project: " + projectId, e);
        }
    }

    public Optional<RemoteProject> findProject(String projectName, Path workspacePath) {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(workspacePath)) {
            for (Path projectDir : stream) {
                if (Files.isDirectory(projectDir)) {
                    Path projectInfoFile = projectDir.resolve(PROJECT_INFO_FILENAME);
                    if (Files.exists(projectInfoFile)) {
                        try {
                            String jsonContent = Files.readString(projectInfoFile);
                            RemoteProject project = Json.fromString(jsonContent, RemoteProject.class);
                            if (projectName.equals(project.projectName())) {
                                return Optional.of(project);
                            }
                        } catch (Exception e) {
                            throw new RuntimeException("Failed to read project info from: " + projectInfoFile + ", error: " + e.getMessage());
                        }
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to read workspace directory: " + workspacePath + ", error: " + e.getMessage());
        }
        return Optional.empty();
    }

}
