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

    private static final String INSERT_SESSION = """
            INSERT INTO workspace_sessions (session_id, project_id, session_path, created_at)
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

        try (Connection conn = dataSource.getConnection();
             PreparedStatement projectStmt = conn.prepareStatement(INSERT_PROJECT)) {

            projectStmt.setString(1, projectId);
            projectStmt.setString(2, projectName);
            projectStmt.setLong(3, createdAt);
            projectStmt.setString(4, attributesJson.toString());
            projectStmt.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Failed to add project: " + projectId, e);
        }
    }

    public void addSession(String projectId, String sessionId, Path newSessionPath) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(INSERT_SESSION)) {

            stmt.setString(1, sessionId);
            stmt.setString(2, projectId);
            stmt.setString(3, newSessionPath.toString());
            stmt.setLong(4, clock.millis());
            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Failed to add session: " + sessionId + " for project: " + projectId, e);
        }
    }
}
