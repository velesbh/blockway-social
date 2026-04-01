package space.blockway.social.velocity.database;

import org.slf4j.Logger;
import space.blockway.social.shared.dto.ApiKeyDto;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages REST API keys. Keys are stored as SHA-256 hashes; the plaintext is never persisted.
 *
 * @author Enzonic LLC — blockway.space
 */
public class ApiKeyRepository {

    private final DatabaseManager db;
    private final Logger logger;

    public ApiKeyRepository(DatabaseManager db, Logger logger) {
        this.db = db;
        this.logger = logger;
    }

    public void createApiKey(String keyHash, String label) {
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO bws_api_keys (key_hash, label, created_at) VALUES (?, ?, ?)")) {
            ps.setString(1, keyHash); ps.setString(2, label);
            ps.setLong(3, System.currentTimeMillis());
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.error("createApiKey failed for label={}", label, e);
        }
    }

    /** Returns true if the key hash exists, and updates last_used timestamp. */
    public boolean validateKey(String keyHash) {
        try (Connection conn = db.getConnection()) {
            try (PreparedStatement check = conn.prepareStatement(
                    "SELECT 1 FROM bws_api_keys WHERE key_hash=? LIMIT 1")) {
                check.setString(1, keyHash);
                if (!check.executeQuery().next()) return false;
            }
            try (PreparedStatement update = conn.prepareStatement(
                    "UPDATE bws_api_keys SET last_used=? WHERE key_hash=?")) {
                update.setLong(1, System.currentTimeMillis()); update.setString(2, keyHash);
                update.executeUpdate();
            }
            return true;
        } catch (SQLException e) {
            logger.error("validateKey failed", e);
        }
        return false;
    }

    public boolean labelExists(String label) {
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT 1 FROM bws_api_keys WHERE label=? LIMIT 1")) {
            ps.setString(1, label);
            return ps.executeQuery().next();
        } catch (SQLException e) {
            logger.error("labelExists failed", e);
        }
        return false;
    }

    public void revokeKey(String label) {
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM bws_api_keys WHERE label=?")) {
            ps.setString(1, label);
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.error("revokeKey failed for label={}", label, e);
        }
    }

    public List<ApiKeyDto> listKeys() {
        List<ApiKeyDto> list = new ArrayList<>();
        try (Connection conn = db.getConnection();
             Statement s = conn.createStatement();
             ResultSet rs = s.executeQuery("SELECT label, created_at, last_used FROM bws_api_keys ORDER BY created_at ASC")) {
            while (rs.next()) {
                ApiKeyDto dto = new ApiKeyDto();
                dto.setLabel(rs.getString("label"));
                dto.setCreatedAt(rs.getLong("created_at"));
                long lu = rs.getLong("last_used");
                dto.setLastUsed(rs.wasNull() ? null : lu);
                list.add(dto);
            }
        } catch (SQLException e) {
            logger.error("listKeys failed", e);
        }
        return list;
    }
}
