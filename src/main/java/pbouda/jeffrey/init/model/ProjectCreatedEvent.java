package pbouda.jeffrey.init.model;

import java.util.Map;

public record ProjectCreatedEvent(
        String projectId,
        String projectName,
        String projectLabel,
        String workspaceId,
        long createdAt,
        RepositoryType repositoryType,
        Map<String, String> attributes) {
}
