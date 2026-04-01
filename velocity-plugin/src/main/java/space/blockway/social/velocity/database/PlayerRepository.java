package space.blockway.social.velocity.database;

import org.slf4j.Logger;

import java.sql.*;
import java.util.Optional;
import java.util.UUID;

/**
 * CRUD operations for the {@code bws_players} table.
 * All methods are synchronous — call from async threads only.
 *
 * @author Enzonic LLC — blockway.space
 */
public class PlayerRepository {

    private final DatabaseManager db;
    private final Logger logger;

    public PlayerRepository(DatabaseManager db, Logger logger) {
        this.db = db;
        this.logger = logger;
    }

    /** Insert or replace a player record (upsert). */
    public void upsertPlayer(UUID uuid, String username) {
        String sql = db.isSqlite()
                ? "INSERT OR REPLACE INTO bws_players (uuid, username, last_seen) VALUES (?, ?, ?)"
                : "INSERT INTO bws_players (uuid, username, last_seen) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE username=VALUES(username), last_seen=VALUES(last_seen)";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            ps.setString(2, username);
            ps.setLong(3, System.currentTimeMillis());
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.error("upsertPlayer failed for {}", uuid, e);
        }
    }

    public void updateLastSeen(UUID uuid, long timestamp) {
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "UPDATE bws_players SET last_seen=? WHERE uuid=?")) {
            ps.setLong(1, timestamp);
            ps.setString(2, uuid.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.error("updateLastSeen failed for {}", uuid, e);
        }
    }

    public void updateLastServer(UUID uuid, String serverName) {
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "UPDATE bws_players SET last_server=? WHERE uuid=?")) {
            ps.setString(1, serverName);
            ps.setString(2, uuid.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.error("updateLastServer failed for {}", uuid, e);
        }
    }

    public record PlayerRecord(UUID uuid, String username, long lastSeen, String lastServer) {}

    public Optional<PlayerRecord> findByUuid(UUID uuid) {
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT uuid, username, last_seen, last_server FROM bws_players WHERE uuid=?")) {
            ps.setString(1, uuid.toString());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return Optional.of(new PlayerRecord(
                        UUID.fromString(rs.getString("uuid")),
                        rs.getString("username"),
                        rs.getLong("last_seen"),
                        rs.getString("last_server")
                ));
            }
        } catch (SQLException e) {
            logger.error("findByUuid failed for {}", uuid, e);
        }
        return Optional.empty();
    }

    public Optional<PlayerRecord> findByUsername(String username) {
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT uuid, username, last_seen, last_server FROM bws_players WHERE LOWER(username)=LOWER(?) LIMIT 1")) {
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return Optional.of(new PlayerRecord(
                        UUID.fromString(rs.getString("uuid")),
                        rs.getString("username"),
                        rs.getLong("last_seen"),
                        rs.getString("last_server")
                ));
            }
        } catch (SQLException e) {
            logger.error("findByUsername failed for {}", username, e);
        }
        return Optional.empty();
    }
}
