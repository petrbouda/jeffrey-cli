package pbouda.jeffrey.init.model;

import java.util.Map;

public record ProjectCreatedEvent(
        String eventId,
        String projectId,
        String workspaceId,
        long createdAt,
        String projectName,
        RepositoryType repositoryType,
        Map<String, String> attributes) {
}
