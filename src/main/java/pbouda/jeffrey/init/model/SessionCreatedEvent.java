package pbouda.jeffrey.init.model;

public record SessionCreatedEvent(
        String eventId,
        String projectId,
        String workspaceId,
        long createdAt,
        String sessionId,
        String relativePath,
        String workspacesPath) {
}
