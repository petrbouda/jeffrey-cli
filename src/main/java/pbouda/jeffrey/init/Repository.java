package pbouda.jeffrey.init;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import pbouda.jeffrey.init.model.ProjectAttribute;

import javax.sql.DataSource;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Clock;
import java.util.List;

public class Repository {

    private static final String SELECT_PROJECT_EXISTS = """
            SELECT 1
            FROM workspace_projects
            WHERE project_id = ?
            """;

    private static final String INSERT_PROJECT = """
            INSERT INTO workspace_projects (project_id, project_name, created_at, attributes)
            VALUES (?, ?, ?, ?)
            """;

    private static final String INSERT_EVENT = """
            INSERT INTO workspace_events (project_id, created_at, event_type, content)
            VALUES (?, ?, ?, ?)
            """;

    private final DataSource dataSource;
    private final Clock clock;

    public Repository(Path dbPath, Clock clock) {
        this.dataSource = DatabaseUtils.notPooled(dbPath);
        this.clock = clock;
    }

    public void initialize() {
        DatabaseUtils.migrate(dataSource);
    }

    public boolean projectExists(String projectId) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT_PROJECT_EXISTS)) {
            stmt.setString(1, projectId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to check if project exists: " + projectId, e);
        }
    }

    public void addProject(String projectId, String projectName, Path projectPath, List<ProjectAttribute> attributes) {
        JsonNode attributesJson = Json.toTree(attributes);
        long createdAt = clock.millis();

        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);

            try (PreparedStatement projectStmt = conn.prepareStatement(INSERT_PROJECT);
                 PreparedStatement eventStmt = conn.prepareStatement(INSERT_EVENT)) {

                projectStmt.setString(1, projectId);
                projectStmt.setString(2, projectName);
                projectStmt.setLong(3, createdAt);
                projectStmt.setString(4, attributesJson.toString());
                projectStmt.executeUpdate();

                ObjectNode eventContent = Json.createObject()
                        .put("project_id", projectId)
                        .put("project_name", projectName)
                        .put("project_path", projectPath.toString())
                        .set("attributes", attributesJson);

                eventStmt.setString(1, projectId);
                eventStmt.setLong(2, createdAt);
                eventStmt.setString(3, EventType.PROJECT_CREATED.name());
                eventStmt.setString(4, Json.toPrettyString(eventContent));
                eventStmt.executeUpdate();

                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to add project: " + projectId, e);
        }
    }

    public void addSession(String projectId, String sessionId, Path newSessionPath) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(INSERT_EVENT)) {

            ObjectNode eventContent = Json.createObject()
                    .put("project_id", projectId)
                    .put("session_id", sessionId)
                    .put("session_path", newSessionPath.toString());

            stmt.setString(1, projectId);
            stmt.setLong(2, clock.millis());
            stmt.setString(3, EventType.SESSION_CREATED.name());
            stmt.setString(4, eventContent.toString());
            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Failed to add session: " + sessionId + " for project: " + projectId, e);
        }
    }
}
