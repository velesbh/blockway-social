package space.blockway.social.velocity.database;

import org.slf4j.Logger;

import java.sql.*;
import java.util.UUID;

/**
 * Per-player social privacy settings stored in {@code bws_player_settings}.
 *
 * @author Enzonic LLC — blockway.space
 */
public class PlayerSettingsRepository {

    private final DatabaseManager db;
    private final Logger logger;

    public PlayerSettingsRepository(DatabaseManager db, Logger logger) {
        this.db = db;
        this.logger = logger;
    }

    public record PlayerSettings(boolean acceptRequests, boolean notifications, boolean privateServer) {
        public static PlayerSettings defaults() { return new PlayerSettings(true, true, false); }
    }

    public PlayerSettings getSettings(UUID playerUuid) {
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT accept_requests, notifications, private_server FROM bws_player_settings WHERE player_uuid=?")) {
            ps.setString(1, playerUuid.toString());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return new PlayerSettings(rs.getInt("accept_requests") == 1,
                        rs.getInt("notifications") == 1, rs.getInt("private_server") == 1);
            }
        } catch (SQLException e) {
            logger.error("getSettings failed for {}", playerUuid, e);
        }
        return PlayerSettings.defaults();
    }

    public void upsertSettings(UUID playerUuid, PlayerSettings settings) {
        String sql = db.isSqlite()
                ? "INSERT OR REPLACE INTO bws_player_settings (player_uuid, accept_requests, notifications, private_server) VALUES (?, ?, ?, ?)"
                : "INSERT INTO bws_player_settings (player_uuid, accept_requests, notifications, private_server) VALUES (?, ?, ?, ?) ON DUPLICATE KEY UPDATE accept_requests=VALUES(accept_requests), notifications=VALUES(notifications), private_server=VALUES(private_server)";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, playerUuid.toString());
            ps.setInt(2, settings.acceptRequests() ? 1 : 0);
            ps.setInt(3, settings.notifications() ? 1 : 0);
            ps.setInt(4, settings.privateServer() ? 1 : 0);
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.error("upsertSettings failed for {}", playerUuid, e);
        }
    }
}
