package pbouda.jeffrey.init.model;

import java.nio.file.Path;

public record SessionCreatedEvent(
        String sessionId,
        Path relativePath,
        Path workspacesPath) {
}
