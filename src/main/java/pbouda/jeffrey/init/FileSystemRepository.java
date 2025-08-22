package pbouda.jeffrey.init;

import pbouda.jeffrey.init.model.ProjectCreatedEvent;
import pbouda.jeffrey.init.model.RepositoryType;
import pbouda.jeffrey.init.model.SessionCreatedEvent;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.util.Map;

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
            String workspaceId,
            RepositoryType repositoryType,
            Map<String, String> attributes,
            Path projectPath) {
        try {
            ProjectCreatedEvent projectEvent = new ProjectCreatedEvent(
                    IDGenerator.generate(),
                    projectId,
                    workspaceId,
                    clock.instant().toEpochMilli(),
                    projectName,
                    repositoryType,
                    attributes);

            Path projectInfoFile = projectPath.resolve(PROJECT_INFO_FILENAME);
            Files.writeString(projectInfoFile, Json.toString(projectEvent));
        } catch (IOException e) {
            throw new RuntimeException("Failed to write project info for project: " + projectId, e);
        }
    }

    public void addSession(
            String projectId,
            String sessionId,
            String workspaceId,
            Path sessionRelativePath,
            Path workspacesPath,
            Path sessionPath) {
        try {
            SessionCreatedEvent sessionEvent = new SessionCreatedEvent(
                    IDGenerator.generate(),
                    projectId,
                    workspaceId,
                    clock.instant().toEpochMilli(),
                    sessionId,
                    sessionRelativePath.toString(),
                    workspacesPath != null ? workspacesPath.toString() : null);

            Path sessionInfoFile = sessionPath.resolve(SESSION_INFO_FILENAME);
            Files.writeString(sessionInfoFile, Json.toString(sessionEvent));
        } catch (IOException e) {
            throw new RuntimeException("Failed to write session info for session: " + sessionId + " in project: " + projectId, e);
        }
    }

}
