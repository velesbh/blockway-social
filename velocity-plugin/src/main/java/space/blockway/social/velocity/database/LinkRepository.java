package space.blockway.social.velocity.database;

import org.slf4j.Logger;

import java.sql.*;
import java.util.Optional;
import java.util.UUID;

/**
 * Manages web account link codes and confirmed web links.
 *
 * @author Enzonic LLC — blockway.space
 */
public class LinkRepository {

    private final DatabaseManager db;
    private final Logger logger;

    public LinkRepository(DatabaseManager db, Logger logger) {
        this.db = db;
        this.logger = logger;
    }

    public void createLinkCode(String code, UUID playerUuid, long expiresAt) {
        String sql = db.isSqlite()
                ? "INSERT OR REPLACE INTO bws_link_codes (code, player_uuid, created_at, expires_at) VALUES (?, ?, ?, ?)"
                : "INSERT INTO bws_link_codes (code, player_uuid, created_at, expires_at) VALUES (?, ?, ?, ?) ON DUPLICATE KEY UPDATE player_uuid=VALUES(player_uuid), created_at=VALUES(created_at), expires_at=VALUES(expires_at)";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, code); ps.setString(2, playerUuid.toString());
            ps.setLong(3, System.currentTimeMillis()); ps.setLong(4, expiresAt);
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.error("createLinkCode failed", e);
        }
    }

    public record LinkCodeRecord(String code, UUID playerUuid, long expiresAt) {}

    /** Returns the record only if it is still valid (not expired). */
    public Optional<LinkCodeRecord> findValidCode(String code) {
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT code, player_uuid, expires_at FROM bws_link_codes WHERE code=? AND expires_at > ?")) {
            ps.setString(1, code); ps.setLong(2, System.currentTimeMillis());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return Optional.of(new LinkCodeRecord(
                    rs.getString("code"), UUID.fromString(rs.getString("player_uuid")), rs.getLong("expires_at")));
        } catch (SQLException e) {
            logger.error("findValidCode failed for code={}", code, e);
        }
        return Optional.empty();
    }

    public void deleteCode(String code) {
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM bws_link_codes WHERE code=?")) {
            ps.setString(1, code);
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.error("deleteCode failed", e);
        }
    }

    public void cleanExpiredCodes() {
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM bws_link_codes WHERE expires_at < ?")) {
            ps.setLong(1, System.currentTimeMillis());
            int deleted = ps.executeUpdate();
            if (deleted > 0) logger.debug("Cleaned {} expired link codes", deleted);
        } catch (SQLException e) {
            logger.error("cleanExpiredCodes failed", e);
        }
    }

    public void createWebLink(UUID playerUuid, String webAccountId) {
        String sql = db.isSqlite()
                ? "INSERT OR REPLACE INTO bws_web_links (player_uuid, web_account_id, linked_at) VALUES (?, ?, ?)"
                : "INSERT INTO bws_web_links (player_uuid, web_account_id, linked_at) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE web_account_id=VALUES(web_account_id), linked_at=VALUES(linked_at)";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, playerUuid.toString()); ps.setString(2, webAccountId);
            ps.setLong(3, System.currentTimeMillis());
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.error("createWebLink failed", e);
        }
    }

    public boolean deleteWebLink(UUID playerUuid) {
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM bws_web_links WHERE player_uuid=?")) {
            ps.setString(1, playerUuid.toString());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("deleteWebLink failed", e);
        }
        return false;
    }

    public Optional<String> getWebAccountId(UUID playerUuid) {
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT web_account_id FROM bws_web_links WHERE player_uuid=?")) {
            ps.setString(1, playerUuid.toString());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return Optional.of(rs.getString("web_account_id"));
        } catch (SQLException e) {
            logger.error("getWebAccountId failed", e);
        }
        return Optional.empty();
    }

    public Optional<UUID> getPlayerUuidByWebAccount(String webAccountId) {
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT player_uuid FROM bws_web_links WHERE web_account_id=? LIMIT 1")) {
            ps.setString(1, webAccountId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return Optional.of(UUID.fromString(rs.getString("player_uuid")));
        } catch (SQLException e) {
            logger.error("getPlayerUuidByWebAccount failed", e);
        }
        return Optional.empty();
    }

    public boolean isLinked(UUID playerUuid) {
        return getWebAccountId(playerUuid).isPresent();
    }
}
