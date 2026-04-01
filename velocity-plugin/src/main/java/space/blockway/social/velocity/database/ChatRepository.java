package space.blockway.social.velocity.database;

import org.slf4j.Logger;
import space.blockway.social.shared.dto.ChatMessageDto;

import java.sql.*;
import java.util.*;

/**
 * Persists and retrieves chat messages for friend and party channels.
 *
 * <p>Friend channel ID: {@code min(uuidA, uuidB) + "-" + max(uuidA, uuidB)}
 * <p>Party channel ID: the party UUID string
 *
 * @author Enzonic LLC — blockway.space
 */
public class ChatRepository {

    private final DatabaseManager db;
    private final Logger logger;

    public ChatRepository(DatabaseManager db, Logger logger) {
        this.db = db;
        this.logger = logger;
    }

    /** Build a canonical, order-independent channel ID for two friends. */
    public static String friendChannelId(UUID a, UUID b) {
        String sa = a.toString(), sb = b.toString();
        return sa.compareTo(sb) <= 0 ? sa + "-" + sb : sb + "-" + sa;
    }

    public void saveMessage(String channelType, String channelId,
                             UUID senderUuid, String senderName, String message) {
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO bws_chat_messages (channel_type, channel_id, sender_uuid, sender_name, message, sent_at) VALUES (?, ?, ?, ?, ?, ?)")) {
            ps.setString(1, channelType);
            ps.setString(2, channelId);
            ps.setString(3, senderUuid.toString());
            ps.setString(4, senderName);
            ps.setString(5, message);
            ps.setLong(6, System.currentTimeMillis());
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.error("saveMessage failed", e);
        }
    }

    public List<ChatMessageDto> getRecentMessages(String channelType, String channelId, int limit) {
        List<ChatMessageDto> list = new ArrayList<>();
        String sql = "SELECT id, channel_type, channel_id, sender_uuid, sender_name, message, sent_at " +
                     "FROM bws_chat_messages WHERE channel_type=? AND channel_id=? " +
                     "ORDER BY sent_at DESC LIMIT ?";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, channelType); ps.setString(2, channelId); ps.setInt(3, limit);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                ChatMessageDto dto = new ChatMessageDto();
                dto.setId(rs.getLong("id"));
                dto.setChannelType(rs.getString("channel_type"));
                dto.setChannelId(rs.getString("channel_id"));
                dto.setSenderUuid(rs.getString("sender_uuid"));
                dto.setSenderUsername(rs.getString("sender_name"));
                dto.setMessage(rs.getString("message"));
                dto.setSentAt(rs.getLong("sent_at"));
                list.add(dto);
            }
        } catch (SQLException e) {
            logger.error("getRecentMessages failed", e);
        }
        Collections.reverse(list); // Return chronological order
        return list;
    }

    /** Keep only the newest {@code keepCount} messages per channel. */
    public void pruneMessages(String channelType, String channelId, int keepCount) {
        try (Connection conn = db.getConnection()) {
            String subquery = db.isSqlite()
                    ? "DELETE FROM bws_chat_messages WHERE channel_type=? AND channel_id=? AND id NOT IN (SELECT id FROM bws_chat_messages WHERE channel_type=? AND channel_id=? ORDER BY sent_at DESC LIMIT ?)"
                    : "DELETE FROM bws_chat_messages WHERE channel_type=? AND channel_id=? AND id NOT IN (SELECT id FROM (SELECT id FROM bws_chat_messages WHERE channel_type=? AND channel_id=? ORDER BY sent_at DESC LIMIT ?) t)";
            try (PreparedStatement ps = conn.prepareStatement(subquery)) {
                ps.setString(1, channelType); ps.setString(2, channelId);
                ps.setString(3, channelType); ps.setString(4, channelId); ps.setInt(5, keepCount);
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            logger.error("pruneMessages failed", e);
        }
    }
}
