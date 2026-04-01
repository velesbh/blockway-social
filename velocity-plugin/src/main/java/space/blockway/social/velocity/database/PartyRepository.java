package space.blockway.social.velocity.database;

import org.slf4j.Logger;

import java.sql.*;
import java.util.*;

/**
 * CRUD for {@code bws_parties} and {@code bws_party_members}.
 *
 * @author Enzonic LLC — blockway.space
 */
public class PartyRepository {

    private final DatabaseManager db;
    private final Logger logger;

    public PartyRepository(DatabaseManager db, Logger logger) {
        this.db = db;
        this.logger = logger;
    }

    public record PartyRecord(String partyId, UUID leaderUuid, long createdAt) {}

    public void createParty(String partyId, UUID leaderUuid) {
        long now = System.currentTimeMillis();
        try (Connection conn = db.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO bws_parties (party_id, leader_uuid, created_at) VALUES (?, ?, ?)")) {
                ps.setString(1, partyId); ps.setString(2, leaderUuid.toString()); ps.setLong(3, now);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO bws_party_members (party_id, player_uuid, joined_at) VALUES (?, ?, ?)")) {
                ps.setString(1, partyId); ps.setString(2, leaderUuid.toString()); ps.setLong(3, now);
                ps.executeUpdate();
            }
            conn.commit();
        } catch (SQLException e) {
            logger.error("createParty failed", e);
        }
    }

    public void deleteParty(String partyId) {
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM bws_parties WHERE party_id=?")) {
            ps.setString(1, partyId);
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.error("deleteParty failed for {}", partyId, e);
        }
    }

    public Optional<PartyRecord> findById(String partyId) {
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT party_id, leader_uuid, created_at FROM bws_parties WHERE party_id=?")) {
            ps.setString(1, partyId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return Optional.of(new PartyRecord(rs.getString("party_id"),
                    UUID.fromString(rs.getString("leader_uuid")), rs.getLong("created_at")));
        } catch (SQLException e) {
            logger.error("findById failed for {}", partyId, e);
        }
        return Optional.empty();
    }

    public Optional<String> findPartyIdByMember(UUID playerUuid) {
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT party_id FROM bws_party_members WHERE player_uuid=? LIMIT 1")) {
            ps.setString(1, playerUuid.toString());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return Optional.of(rs.getString("party_id"));
        } catch (SQLException e) {
            logger.error("findPartyIdByMember failed for {}", playerUuid, e);
        }
        return Optional.empty();
    }

    public void addMember(String partyId, UUID playerUuid) {
        String sql = db.isSqlite()
                ? "INSERT OR IGNORE INTO bws_party_members (party_id, player_uuid, joined_at) VALUES (?, ?, ?)"
                : "INSERT IGNORE INTO bws_party_members (party_id, player_uuid, joined_at) VALUES (?, ?, ?)";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, partyId); ps.setString(2, playerUuid.toString());
            ps.setLong(3, System.currentTimeMillis());
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.error("addMember failed", e);
        }
    }

    public void removeMember(String partyId, UUID playerUuid) {
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "DELETE FROM bws_party_members WHERE party_id=? AND player_uuid=?")) {
            ps.setString(1, partyId); ps.setString(2, playerUuid.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.error("removeMember failed", e);
        }
    }

    public record MemberRecord(UUID playerUuid, long joinedAt) {}

    public List<MemberRecord> getMembers(String partyId) {
        List<MemberRecord> list = new ArrayList<>();
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT player_uuid, joined_at FROM bws_party_members WHERE party_id=? ORDER BY joined_at ASC")) {
            ps.setString(1, partyId);
            ResultSet rs = ps.executeQuery();
            while (rs.next())
                list.add(new MemberRecord(UUID.fromString(rs.getString("player_uuid")), rs.getLong("joined_at")));
        } catch (SQLException e) {
            logger.error("getMembers failed for {}", partyId, e);
        }
        return list;
    }

    public int countMembers(String partyId) {
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT COUNT(*) FROM bws_party_members WHERE party_id=?")) {
            ps.setString(1, partyId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            logger.error("countMembers failed", e);
        }
        return 0;
    }

    public void updateLeader(String partyId, UUID newLeaderUuid) {
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "UPDATE bws_parties SET leader_uuid=? WHERE party_id=?")) {
            ps.setString(1, newLeaderUuid.toString()); ps.setString(2, partyId);
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.error("updateLeader failed", e);
        }
    }
}
