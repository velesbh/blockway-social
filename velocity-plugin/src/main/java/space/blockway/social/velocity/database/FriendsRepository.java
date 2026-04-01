package space.blockway.social.velocity.database;

import org.slf4j.Logger;

import java.sql.*;
import java.util.*;

/**
 * CRUD operations for {@code bws_friendships} and {@code bws_friend_requests}.
 *
 * <p>Friendships are stored as two rows per pair (A→B and B→A) for O(1) lookup.
 *
 * @author Enzonic LLC — blockway.space
 */
public class FriendsRepository {

    private final DatabaseManager db;
    private final Logger logger;

    public FriendsRepository(DatabaseManager db, Logger logger) {
        this.db = db;
        this.logger = logger;
    }

    /** Insert both directions of a friendship. */
    public void addFriendship(UUID a, UUID b) {
        long now = System.currentTimeMillis();
        String sql = db.isSqlite()
                ? "INSERT OR IGNORE INTO bws_friendships (player_uuid, friend_uuid, created_at) VALUES (?, ?, ?)"
                : "INSERT IGNORE INTO bws_friendships (player_uuid, friend_uuid, created_at) VALUES (?, ?, ?)";
        try (Connection conn = db.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, a.toString()); ps.setString(2, b.toString()); ps.setLong(3, now);
                ps.executeUpdate();
                ps.setString(1, b.toString()); ps.setString(2, a.toString()); ps.setLong(3, now);
                ps.executeUpdate();
            }
            conn.commit();
        } catch (SQLException e) {
            logger.error("addFriendship failed for {} <-> {}", a, b, e);
        }
    }

    /** Remove both directions of a friendship. */
    public void removeFriendship(UUID a, UUID b) {
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "DELETE FROM bws_friendships WHERE (player_uuid=? AND friend_uuid=?) OR (player_uuid=? AND friend_uuid=?)")) {
            ps.setString(1, a.toString()); ps.setString(2, b.toString());
            ps.setString(3, b.toString()); ps.setString(4, a.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.error("removeFriendship failed for {} <-> {}", a, b, e);
        }
    }

    public boolean areFriends(UUID a, UUID b) {
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT 1 FROM bws_friendships WHERE player_uuid=? AND friend_uuid=? LIMIT 1")) {
            ps.setString(1, a.toString()); ps.setString(2, b.toString());
            return ps.executeQuery().next();
        } catch (SQLException e) {
            logger.error("areFriends failed", e);
        }
        return false;
    }

    public record FriendRecord(UUID friendUuid, long createdAt) {}

    public List<FriendRecord> getFriends(UUID playerUuid) {
        List<FriendRecord> list = new ArrayList<>();
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT friend_uuid, created_at FROM bws_friendships WHERE player_uuid=?")) {
            ps.setString(1, playerUuid.toString());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(new FriendRecord(UUID.fromString(rs.getString("friend_uuid")), rs.getLong("created_at")));
            }
        } catch (SQLException e) {
            logger.error("getFriends failed for {}", playerUuid, e);
        }
        return list;
    }

    public int countFriends(UUID playerUuid) {
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT COUNT(*) FROM bws_friendships WHERE player_uuid=?")) {
            ps.setString(1, playerUuid.toString());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            logger.error("countFriends failed", e);
        }
        return 0;
    }

    // ── Friend Requests ───────────────────────────────────────────────────────

    public void createFriendRequest(UUID sender, UUID receiver) {
        long now = System.currentTimeMillis();
        String sql = db.isSqlite()
                ? "INSERT OR IGNORE INTO bws_friend_requests (sender_uuid, receiver_uuid, sent_at) VALUES (?, ?, ?)"
                : "INSERT IGNORE INTO bws_friend_requests (sender_uuid, receiver_uuid, sent_at) VALUES (?, ?, ?)";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, sender.toString()); ps.setString(2, receiver.toString()); ps.setLong(3, now);
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.error("createFriendRequest failed", e);
        }
    }

    public boolean requestExists(UUID sender, UUID receiver) {
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT 1 FROM bws_friend_requests WHERE sender_uuid=? AND receiver_uuid=? LIMIT 1")) {
            ps.setString(1, sender.toString()); ps.setString(2, receiver.toString());
            return ps.executeQuery().next();
        } catch (SQLException e) {
            logger.error("requestExists failed", e);
        }
        return false;
    }

    public void deleteRequest(UUID sender, UUID receiver) {
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "DELETE FROM bws_friend_requests WHERE sender_uuid=? AND receiver_uuid=?")) {
            ps.setString(1, sender.toString()); ps.setString(2, receiver.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.error("deleteRequest failed", e);
        }
    }

    public List<UUID> getIncomingRequestSenders(UUID receiver) {
        List<UUID> list = new ArrayList<>();
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT sender_uuid FROM bws_friend_requests WHERE receiver_uuid=?")) {
            ps.setString(1, receiver.toString());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(UUID.fromString(rs.getString("sender_uuid")));
        } catch (SQLException e) {
            logger.error("getIncomingRequestSenders failed", e);
        }
        return list;
    }
}
