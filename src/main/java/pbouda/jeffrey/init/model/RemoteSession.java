package pbouda.jeffrey.init.model;

public record RemoteSession(
        String sessionId,
        String projectId,
        String workspaceId,
        long createdAt,
        String relativePath,
        String workspacesPath) {
}
