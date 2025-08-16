package pbouda.jeffrey.init.model;

import java.util.Map;

public record ProjectCreatedEvent(
        String projectName,
        RepositoryType repositoryType,
        Map<String, String> attributes) {
}
