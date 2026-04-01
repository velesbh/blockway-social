package space.blockway.social.paper.messaging;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;
import space.blockway.social.paper.BlockwaySocialPaper;
import space.blockway.social.shared.ChannelMessage;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Receives downstream plugin messages from the Velocity proxy on the {@code blockwaysocial:events} channel.
 * Deserialises them and dispatches to the appropriate in-game handler (chat message, notification, etc.).
 *
 * @author Enzonic LLC — blockway.space
 */
public class PaperMessageHandler implements PluginMessageListener {

    private final BlockwaySocialPaper plugin;
    private final Gson gson = new Gson();
    private final MiniMessage mm = MiniMessage.miniMessage();

    public PaperMessageHandler(BlockwaySocialPaper plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onPluginMessageReceived(String channel, Player carrier, byte[] message) {
        if (!channel.equals(ProxyMessageSender.CHANNEL)) return;
        String json = new String(message, StandardCharsets.UTF_8);
        try {
            ChannelMessage msg = gson.fromJson(json, ChannelMessage.class);
            if (msg == null || msg.getType() == null) return;
            dispatch(msg);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to parse incoming plugin message: " + json);
        }
    }

    private void dispatch(ChannelMessage msg) {
        switch (msg.getType()) {
            case FRIEND_REQUEST_RECEIVED  -> handleFriendRequestReceived(msg);
            case FRIEND_REQUEST_ACCEPTED  -> handleFriendRequestAccepted(msg);
            case FRIEND_REQUEST_DENIED    -> handleFriendRequestDenied(msg);
            case FRIEND_ONLINE            -> handleFriendOnline(msg);
            case FRIEND_OFFLINE           -> handleFriendOffline(msg);
            case FRIEND_CHAT_MESSAGE      -> handleFriendChatMessage(msg);
            case PARTY_INVITE_RECEIVED    -> handlePartyInviteReceived(msg);
            case PARTY_CHAT_MESSAGE       -> handlePartyChatMessage(msg);
            case PARTY_CREATED            -> handlePartyCreated(msg);
            case PARTY_DISBANDED          -> handlePartyDisbanded(msg);
            case PARTY_KICKED             -> handlePartyKicked(msg);
            case PARTY_MEMBER_JOINED      -> handlePartyMemberJoined(msg);
            case PARTY_MEMBER_LEFT        -> handlePartyMemberLeft(msg);
            case PARTY_LEADER_TRANSFERRED -> handlePartyLeaderTransferred(msg);
            case LINK_CODE_GENERATED      -> handleLinkCodeGenerated(msg);
            case LINK_REMOVED             -> handleLinkRemoved(msg);
            case LINK_NOT_FOUND           -> handleLinkNotFound(msg);
            case RESULT                   -> handleResult(msg);
            default -> {} // ignore upstream-only types
        }
    }

    private JsonObject payload(ChannelMessage msg) {
        return JsonParser.parseString(msg.getPayload()).getAsJsonObject();
    }

    private void sendTo(String uuidStr, net.kyori.adventure.text.Component component) {
        if (uuidStr == null) return;
        try {
            Player p = plugin.getServer().getPlayer(UUID.fromString(uuidStr));
            if (p != null) p.sendMessage(component);
        } catch (IllegalArgumentException ignored) {}
    }

    private void handleFriendRequestReceived(ChannelMessage msg) {
        JsonObject p = payload(msg);
        String senderName = p.get("senderUsername").getAsString();
        sendTo(msg.getTargetUuid(), plugin.getBwsConfig().getPrefixedMessage("friend-request-received", "player", senderName));
    }

    private void handleFriendRequestAccepted(ChannelMessage msg) {
        JsonObject p = payload(msg);
        String friendName = p.get("friendUsername").getAsString();
        sendTo(msg.getTargetUuid(), plugin.getBwsConfig().getPrefixedMessage("friend-added", "player", friendName));
    }

    private void handleFriendRequestDenied(ChannelMessage msg) {
        // Optionally notify the sender that their request was denied
        // Keeping minimal — no config message for this by default
    }

    private void handleFriendOnline(ChannelMessage msg) {
        JsonObject p = payload(msg);
        String username = p.get("username").getAsString();
        String server = p.get("serverName").getAsString();
        sendTo(msg.getTargetUuid(), plugin.getBwsConfig().getPrefixedMessage("friend-online", "player", username, "server", server));
    }

    private void handleFriendOffline(ChannelMessage msg) {
        JsonObject p = payload(msg);
        String username = p.get("username").getAsString();
        sendTo(msg.getTargetUuid(), plugin.getBwsConfig().getPrefixedMessage("friend-offline", "player", username));
    }

    private void handleFriendChatMessage(ChannelMessage msg) {
        JsonObject p = payload(msg);
        String senderName = p.get("senderUsername").getAsString();
        String message = p.get("message").getAsString();
        sendTo(msg.getTargetUuid(),
                mm.deserialize(plugin.getBwsConfig().getMessage("friend-chat-format",
                        "player", senderName, "message", message).toString()));
        // Note: We parse the format directly from config
        Player target = msg.getTargetUuid() != null
                ? plugin.getServer().getPlayer(UUID.fromString(msg.getTargetUuid())) : null;
        if (target != null) {
            target.sendMessage(plugin.getBwsConfig().getMessage("friend-chat-format", "player", senderName, "message", message));
        }
    }

    private void handlePartyInviteReceived(ChannelMessage msg) {
        JsonObject p = payload(msg);
        String leaderName = p.get("leaderUsername").getAsString();
        sendTo(msg.getTargetUuid(), plugin.getBwsConfig().getPrefixedMessage("party-invite-received", "player", leaderName));
    }

    private void handlePartyChatMessage(ChannelMessage msg) {
        JsonObject p = payload(msg);
        String senderName = p.get("senderUsername").getAsString();
        String message = p.get("message").getAsString();
        sendTo(msg.getTargetUuid(), plugin.getBwsConfig().getMessage("party-chat-format", "player", senderName, "message", message));
    }

    private void handlePartyCreated(ChannelMessage msg) {
        sendTo(msg.getTargetUuid(), plugin.getBwsConfig().getPrefixedMessage("party-created"));
    }

    private void handlePartyDisbanded(ChannelMessage msg) {
        sendTo(msg.getTargetUuid(), plugin.getBwsConfig().getPrefixedMessage("party-disbanded"));
    }

    private void handlePartyKicked(ChannelMessage msg) {
        sendTo(msg.getTargetUuid(), plugin.getBwsConfig().getPrefixedMessage("party-kicked"));
    }

    private void handlePartyMemberJoined(ChannelMessage msg) {
        JsonObject p = payload(msg);
        String playerName = p.get("username").getAsString();
        sendTo(msg.getTargetUuid(), plugin.getBwsConfig().getPrefixedMessage("party-member-joined", "player", playerName));
    }

    private void handlePartyMemberLeft(ChannelMessage msg) {
        JsonObject p = payload(msg);
        String playerName = p.get("username").getAsString();
        sendTo(msg.getTargetUuid(), plugin.getBwsConfig().getPrefixedMessage("party-member-left", "player", playerName));
    }

    private void handlePartyLeaderTransferred(ChannelMessage msg) {
        sendTo(msg.getTargetUuid(), plugin.getBwsConfig().getPrefixedMessage("party-leader-transferred"));
    }

    private void handleLinkCodeGenerated(ChannelMessage msg) {
        JsonObject p = payload(msg);
        String code = p.get("code").getAsString();
        String minutes = p.has("expiryMinutes") ? p.get("expiryMinutes").getAsString() : "10";
        sendTo(msg.getTargetUuid(), plugin.getBwsConfig().getPrefixedMessage("link-code", "code", code, "minutes", minutes));
    }

    private void handleLinkRemoved(ChannelMessage msg) {
        sendTo(msg.getTargetUuid(), plugin.getBwsConfig().getPrefixedMessage("unlinked"));
    }

    private void handleLinkNotFound(ChannelMessage msg) {
        sendTo(msg.getTargetUuid(), plugin.getBwsConfig().getPrefixedMessage("not-linked"));
    }

    private void handleResult(ChannelMessage msg) {
        // Generic result handler — shows a message based on the result code
        JsonObject p = payload(msg);
        String result = p.get("result").getAsString();
        String target = p.has("target") ? p.get("target").getAsString() : "";
        String configKey = switch (result) {
            case "SUCCESS"               -> null; // handled by specific messages
            case "PLAYER_NOT_FOUND"      -> "friend-not-found";
            case "ALREADY_FRIENDS"       -> "friend-already";
            case "REQUEST_ALREADY_SENT"  -> "friend-request-pending";
            case "NOT_FRIENDS"           -> "not-friends";
            case "CANNOT_ADD_SELF"       -> "cannot-add-self";
            case "MAX_FRIENDS_REACHED"   -> "max-friends";
            case "REQUESTS_DISABLED"     -> "requests-disabled";
            case "SERVER_RESTRICTED"     -> "friend-server-restricted";
            case "NOT_IN_PARTY"          -> "party-not-in-party";
            case "ALREADY_IN_PARTY"      -> "party-already-in-party";
            case "NOT_LEADER"            -> "party-not-leader";
            case "PARTY_FULL"            -> "party-full";
            case "ALREADY_LINKED"        -> "already-linked";
            default                      -> null;
        };
        if (configKey != null) {
            sendTo(msg.getTargetUuid(), plugin.getBwsConfig().getPrefixedMessage(configKey, "player", target));
        }
    }
}
