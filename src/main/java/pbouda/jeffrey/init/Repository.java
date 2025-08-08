package pbouda.jeffrey.init;

import com.fasterxml.jackson.databind.JsonNode;
import pbouda.jeffrey.init.model.RepositoryType;

import javax.sql.DataSource;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public class Repository {

    private static final String DATABASE_FILENAME = "workspace.db";

    private static final String SELECT_PROJECT_EXISTS = """
            SELECT 1
            FROM workspace_projects
            WHERE project_id = ?
            """;

    private static final String INSERT_PROJECT = """
            INSERT INTO workspace_projects (project_id, project_name, created_at, attributes)
            VALUES (?, ?, ?, ?)
            """;

    private static final String INSERT_SESSION = """
            INSERT INTO workspace_sessions (session_id, project_id, session_path, created_at)
            VALUES (?, ?, ?, ?)
            """;

    private static final String INSERT_EVENT = """
            INSERT INTO workspace_events (project_id, created_at, event_type, content)
            VALUES (?, ?, ?, ?)
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

    public void addProject(
            String projectId,
            String projectName,
            Path projectPath,
            RepositoryType repositoryType,
            Map<String, String> attributes) {
        JsonNode attributesJson = Json.toTree(attributes);
        long createdAt = clock.millis();

        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);

            try (
                    PreparedStatement projectStmt = conn.prepareStatement(INSERT_PROJECT);
//                    PreparedStatement eventStmt = conn.prepareStatement(INSERT_EVENT)
            ) {

                projectStmt.setString(1, projectId);
                projectStmt.setString(2, projectName);
                projectStmt.setLong(3, createdAt);
                projectStmt.setString(4, attributesJson.toString());
                projectStmt.executeUpdate();

//                ObjectNode eventContent = Json.createObject()
//                        .put("project_id", projectId)
//                        .put("project_name", projectName)
//                        .put("project_path", projectPath.toString())
//                        .put("repository_type", repositoryType.name())
//                        .set("attributes", attributesJson);
//
//                eventStmt.setString(1, projectId);
//                eventStmt.setLong(2, createdAt);
//                eventStmt.setString(3, EventType.PROJECT_CREATED.name());
//                eventStmt.setString(4, eventContent.toString());
//                eventStmt.executeUpdate();

                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to add project: " + projectId, e);
        }
    }

    public void addSession(String projectId, String sessionId, RepositoryType repositoryType, Path newSessionPath) {
        Instant createdAt = clock.instant();

        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);

            try (
                    PreparedStatement sessionStmt = conn.prepareStatement(INSERT_SESSION);
//                    PreparedStatement eventStmt = conn.prepareStatement(INSERT_EVENT)
            ) {

                sessionStmt.setString(1, sessionId);
                sessionStmt.setString(2, projectId);
                sessionStmt.setString(3, newSessionPath.toString());
                sessionStmt.setLong(4, createdAt.toEpochMilli());
                sessionStmt.executeUpdate();

//                ObjectNode eventContent = Json.createObject()
//                        .put("project_id", projectId)
//                        .put("session_id", sessionId)
//                        .put("created_at", createdAt.toString())
//                        .put("session_path", newSessionPath.toString())
//                        .put("repository_type", repositoryType.name());
//
//                eventStmt.setString(1, projectId);
//                eventStmt.setLong(2, createdAt.toEpochMilli());
//                eventStmt.setString(3, EventType.SESSION_CREATED.name());
//                eventStmt.setString(4, eventContent.toString());
//                eventStmt.executeUpdate();

                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to add session: " + sessionId + " for project: " + projectId, e);
        }
    }
}
