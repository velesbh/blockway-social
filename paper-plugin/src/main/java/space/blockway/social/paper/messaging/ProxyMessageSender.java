package space.blockway.social.paper.messaging;

import com.google.gson.Gson;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import space.blockway.social.shared.ChannelMessage;
import space.blockway.social.shared.MessageType;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Sends {@link ChannelMessage} objects upstream to the Velocity proxy via plugin messaging.
 *
 * <p>Uses Gson (bundled in Paper) for serialisation — no extra dependencies required.
 *
 * <p><strong>Important:</strong> A {@link Player} must be provided as the channel conduit.
 * Plugin messages from a backend server always require an online player as the carrier.
 *
 * @author Enzonic LLC — blockway.space
 */
public class ProxyMessageSender {

    public static final String CHANNEL = "blockwaysocial:events";
    private static final Gson GSON = new Gson();

    private final Plugin plugin;

    public ProxyMessageSender(Plugin plugin) {
        this.plugin = plugin;
    }

    public void send(Player player, ChannelMessage message) {
        byte[] data = GSON.toJson(message).getBytes(StandardCharsets.UTF_8);
        player.sendPluginMessage(plugin, CHANNEL, data);
    }

    // ── Convenience senders ───────────────────────────────────────────────────

    public void sendPlayerJoin(Player player, String serverName) {
        String payload = buildPayload("uuid", player.getUniqueId().toString(),
                "username", player.getName(), "serverName", serverName);
        send(player, new ChannelMessage(MessageType.PLAYER_JOIN_SERVER, payload));
    }

    public void sendPlayerLeave(Player player, String serverName) {
        String payload = buildPayload("uuid", player.getUniqueId().toString(),
                "username", player.getName(), "serverName", serverName);
        send(player, new ChannelMessage(MessageType.PLAYER_LEAVE_SERVER, payload));
    }

    public void sendFriendRequest(Player sender, String targetUsername) {
        String payload = buildPayload("senderUuid", sender.getUniqueId().toString(),
                "senderUsername", sender.getName(), "targetUsername", targetUsername);
        send(sender, new ChannelMessage(MessageType.FRIEND_REQUEST_RELAY, payload));
    }

    public void sendFriendAccept(Player accepter, String senderUsername) {
        String payload = buildPayload("accepterUuid", accepter.getUniqueId().toString(),
                "accepterUsername", accepter.getName(), "senderUsername", senderUsername);
        send(accepter, new ChannelMessage(MessageType.FRIEND_ACCEPT_RELAY, payload));
    }

    public void sendFriendDeny(Player denier, String senderUsername) {
        String payload = buildPayload("denierUuid", denier.getUniqueId().toString(),
                "senderUsername", senderUsername);
        send(denier, new ChannelMessage(MessageType.FRIEND_DENY_RELAY, payload));
    }

    public void sendFriendRemove(Player player, String targetUsername) {
        String payload = buildPayload("playerUuid", player.getUniqueId().toString(),
                "targetUsername", targetUsername);
        send(player, new ChannelMessage(MessageType.FRIEND_REMOVE_RELAY, payload));
    }

    public void sendFriendJoin(Player requester, String friendUsername) {
        String payload = buildPayload("requesterUuid", requester.getUniqueId().toString(),
                "friendUsername", friendUsername);
        send(requester, new ChannelMessage(MessageType.FRIEND_JOIN_RELAY, payload));
    }

    public void sendFriendChat(Player sender, String message) {
        String payload = buildPayload("senderUuid", sender.getUniqueId().toString(),
                "senderUsername", sender.getName(), "message", escapeJson(message));
        send(sender, new ChannelMessage(MessageType.FRIEND_CHAT_SEND, payload));
    }

    public void sendPartyCreate(Player leader) {
        String payload = buildPayload("leaderUuid", leader.getUniqueId().toString(),
                "leaderUsername", leader.getName());
        send(leader, new ChannelMessage(MessageType.PARTY_CREATE_RELAY, payload));
    }

    public void sendPartyInvite(Player leader, String targetUsername) {
        String payload = buildPayload("leaderUuid", leader.getUniqueId().toString(),
                "leaderUsername", leader.getName(), "targetUsername", targetUsername);
        send(leader, new ChannelMessage(MessageType.PARTY_INVITE_RELAY, payload));
    }

    public void sendPartyAccept(Player player, String leaderUsername) {
        String payload = buildPayload("playerUuid", player.getUniqueId().toString(),
                "playerUsername", player.getName(), "leaderUsername", leaderUsername);
        send(player, new ChannelMessage(MessageType.PARTY_ACCEPT_RELAY, payload));
    }

    public void sendPartyLeave(Player player) {
        String payload = buildPayload("playerUuid", player.getUniqueId().toString(),
                "playerUsername", player.getName());
        send(player, new ChannelMessage(MessageType.PARTY_LEAVE_RELAY, payload));
    }

    public void sendPartyKick(Player leader, String targetUsername) {
        String payload = buildPayload("leaderUuid", leader.getUniqueId().toString(),
                "targetUsername", targetUsername);
        send(leader, new ChannelMessage(MessageType.PARTY_KICK_RELAY, payload));
    }

    public void sendPartyDisband(Player leader) {
        String payload = buildPayload("leaderUuid", leader.getUniqueId().toString());
        send(leader, new ChannelMessage(MessageType.PARTY_DISBAND_RELAY, payload));
    }

    public void sendPartyWarp(Player leader, String serverName) {
        String payload = buildPayload("leaderUuid", leader.getUniqueId().toString(),
                "serverName", serverName);
        send(leader, new ChannelMessage(MessageType.PARTY_WARP_RELAY, payload));
    }

    public void sendPartyChat(Player sender, String message) {
        String payload = buildPayload("senderUuid", sender.getUniqueId().toString(),
                "senderUsername", sender.getName(), "message", escapeJson(message));
        send(sender, new ChannelMessage(MessageType.PARTY_CHAT_SEND, payload));
    }

    public void sendLinkGenerate(Player player) {
        String payload = buildPayload("playerUuid", player.getUniqueId().toString());
        send(player, new ChannelMessage(MessageType.LINK_GENERATE, payload));
    }

    public void sendLinkRemove(Player player) {
        String payload = buildPayload("playerUuid", player.getUniqueId().toString());
        send(player, new ChannelMessage(MessageType.LINK_REMOVE, payload));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Build a simple JSON object string from alternating key/value pairs. */
    private String buildPayload(String... keyValuePairs) {
        StringBuilder sb = new StringBuilder("{");
        for (int i = 0; i + 1 < keyValuePairs.length; i += 2) {
            if (i > 0) sb.append(",");
            sb.append("\"").append(keyValuePairs[i]).append("\":\"")
              .append(escapeJson(keyValuePairs[i + 1])).append("\"");
        }
        sb.append("}");
        return sb.toString();
    }

    private String escapeJson(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }
}
