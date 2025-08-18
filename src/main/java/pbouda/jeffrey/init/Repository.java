package pbouda.jeffrey.init;

import pbouda.jeffrey.init.model.EventType;
import pbouda.jeffrey.init.model.ProjectCreatedEvent;
import pbouda.jeffrey.init.model.RepositoryType;
import pbouda.jeffrey.init.model.SessionCreatedEvent;

import javax.sql.DataSource;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Clock;
import java.util.Map;

public class Repository {

    private static final String DATABASE_FILENAME = "workspace.db";

    private static final String INSERT_EVENT = """
            INSERT INTO workspace_events (event_id, project_id, workspace_id, event_type, content, created_at)
            VALUES (?, ?, ?, ?, ?, ?)
            """;

    private final DataSource dataSource;
    private final Clock clock;

    public Repository(Path dbPath, Clock clock) {
        Path workspaceDb = dbPath.resolve(DATABASE_FILENAME);
        this.dataSource = DatabaseUtils.notPooled(workspaceDb);
        this.clock = clock;
    }

    public void initialize() {
        DatabaseUtils.migrate(dataSource);
    }

    public void addProject(
            String projectId,
            String projectName,
            String workspaceId,
            RepositoryType repositoryType,
            Map<String, String> attributes) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement eventStmt = conn.prepareStatement(INSERT_EVENT)) {
            ProjectCreatedEvent event = new ProjectCreatedEvent(
                    projectName, repositoryType, attributes);

            execute(eventStmt, EventType.PROJECT_CREATED, projectId, workspaceId, Json.toString(event), clock);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to add project: " + projectId, e);
        }
    }

    public void addSession(
            String projectId,
            String sessionId,
            String workspaceId,
            Path sessionRelativePath,
            Path workspacesPath) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement eventStmt = conn.prepareStatement(INSERT_EVENT)) {
            SessionCreatedEvent event = new SessionCreatedEvent(
                    sessionId, sessionRelativePath.toString(), workspacesPath != null ? workspacesPath.toString() : null);

            execute(eventStmt, EventType.SESSION_CREATED, projectId, workspaceId, Json.toString(event), clock);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to add session: " + sessionId + " for project: " + projectId, e);
        }
    }

    private static void execute(
            PreparedStatement eventStmt,
            EventType eventType,
            String projectId,
            String workspaceId,
            String content,
            Clock clock) throws SQLException {

        eventStmt.setString(1, IDGenerator.generate());
        eventStmt.setString(2, projectId);
        eventStmt.setString(3, workspaceId);
        eventStmt.setString(4, eventType.name());
        eventStmt.setString(5, content);
        eventStmt.setLong(6, clock.instant().toEpochMilli());
        eventStmt.executeUpdate();
    }
}
