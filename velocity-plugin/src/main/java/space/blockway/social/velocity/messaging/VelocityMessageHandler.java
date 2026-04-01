package space.blockway.social.velocity.messaging;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.proxy.ServerConnection;
import org.slf4j.Logger;
import space.blockway.social.shared.ChannelMessage;
import space.blockway.social.shared.MessageType;
import space.blockway.social.velocity.managers.*;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Receives plugin messages from Paper backend servers on the {@code blockwaysocial:events} channel.
 *
 * <p><strong>Security:</strong> Only messages originating from a {@link ServerConnection}
 * (i.e., a backend server) are processed. Messages from players (clients) are rejected.
 * All handled messages are marked as {@link PluginMessageEvent.ForwardResult#handled()} to
 * prevent them from being forwarded to the client.
 *
 * @author Enzonic LLC — blockway.space
 */
public class VelocityMessageHandler {

    private final FriendsManager friendsManager;
    private final PartyManager partyManager;
    private final PresenceManager presenceManager;
    private final LinkManager linkManager;
    private final MessageSender messageSender;
    private final Logger logger;
    private final Gson gson = new Gson();

    public VelocityMessageHandler(FriendsManager friendsManager, PartyManager partyManager,
                                   PresenceManager presenceManager, LinkManager linkManager,
                                   MessageSender messageSender, Logger logger) {
        this.friendsManager = friendsManager;
        this.partyManager = partyManager;
        this.presenceManager = presenceManager;
        this.linkManager = linkManager;
        this.messageSender = messageSender;
        this.logger = logger;
    }

    @Subscribe
    public void onPluginMessage(PluginMessageEvent event) {
        // Only accept messages from backend servers, never from clients
        if (!(event.getSource() instanceof ServerConnection)) return;
        if (!event.getIdentifier().equals(MessageSender.CHANNEL)) return;

        // Mark handled so Velocity does not forward it downstream to the player
        event.setResult(PluginMessageEvent.ForwardResult.handled());

        String json = new String(event.getData(), StandardCharsets.UTF_8);
        try {
            ChannelMessage msg = gson.fromJson(json, ChannelMessage.class);
            dispatch(msg);
        } catch (Exception e) {
            logger.warn("Failed to parse plugin message: {}", json, e);
        }
    }

    private void dispatch(ChannelMessage msg) {
        if (msg.getType() == null) return;
        switch (msg.getType()) {
            case PLAYER_JOIN_SERVER    -> handlePlayerJoin(msg);
            case PLAYER_LEAVE_SERVER   -> handlePlayerLeave(msg);
            case FRIEND_REQUEST_RELAY  -> handleFriendRequest(msg);
            case FRIEND_ACCEPT_RELAY   -> handleFriendAccept(msg);
            case FRIEND_DENY_RELAY     -> handleFriendDeny(msg);
            case FRIEND_REMOVE_RELAY   -> handleFriendRemove(msg);
            case FRIEND_JOIN_RELAY     -> handleFriendJoin(msg);
            case PARTY_CREATE_RELAY    -> handlePartyCreate(msg);
            case PARTY_INVITE_RELAY    -> handlePartyInvite(msg);
            case PARTY_ACCEPT_RELAY    -> handlePartyAccept(msg);
            case PARTY_LEAVE_RELAY     -> handlePartyLeave(msg);
            case PARTY_KICK_RELAY      -> handlePartyKick(msg);
            case PARTY_DISBAND_RELAY   -> handlePartyDisband(msg);
            case PARTY_WARP_RELAY      -> handlePartyWarp(msg);
            case FRIEND_CHAT_SEND      -> handleFriendChat(msg);
            case PARTY_CHAT_SEND       -> handlePartyChat(msg);
            case LINK_GENERATE         -> handleLinkGenerate(msg);
            case LINK_REMOVE           -> handleLinkRemove(msg);
            default -> logger.debug("Unhandled upstream message type: {}", msg.getType());
        }
    }

    private JsonObject payload(ChannelMessage msg) {
        return JsonParser.parseString(msg.getPayload()).getAsJsonObject();
    }

    private void handlePlayerJoin(ChannelMessage msg) {
        JsonObject p = payload(msg);
        UUID uuid = UUID.fromString(p.get("uuid").getAsString());
        String username = p.get("username").getAsString();
        String server = p.get("serverName").getAsString();
        presenceManager.markOnline(uuid, username, server);
    }

    private void handlePlayerLeave(ChannelMessage msg) {
        JsonObject p = payload(msg);
        UUID uuid = UUID.fromString(p.get("uuid").getAsString());
        String username = p.get("username").getAsString();
        presenceManager.markOffline(uuid);
        friendsManager.broadcastFriendOffline(uuid, username);
    }

    private void handleFriendRequest(ChannelMessage msg) {
        JsonObject p = payload(msg);
        UUID senderUuid = UUID.fromString(p.get("senderUuid").getAsString());
        String senderName = p.get("senderUsername").getAsString();
        String targetName = p.get("targetUsername").getAsString();
        FriendsManager.FriendResult result = friendsManager.sendFriendRequest(senderUuid, senderName, targetName);
        // Send result back to sender
        String resultPayload = String.format("{\"result\":\"%s\",\"target\":\"%s\"}", result.name(), targetName);
        messageSender.sendToPlayer(senderUuid, new ChannelMessage(MessageType.RESULT, resultPayload, senderUuid.toString()));
    }

    private void handleFriendAccept(ChannelMessage msg) {
        JsonObject p = payload(msg);
        UUID accepterUuid = UUID.fromString(p.get("accepterUuid").getAsString());
        String accepterName = p.get("accepterUsername").getAsString();
        String senderName = p.get("senderUsername").getAsString();
        FriendsManager.FriendResult result = friendsManager.acceptFriendRequest(accepterUuid, accepterName, senderName);
        String resultPayload = String.format("{\"result\":\"%s\",\"target\":\"%s\"}", result.name(), senderName);
        messageSender.sendToPlayer(accepterUuid, new ChannelMessage(MessageType.RESULT, resultPayload, accepterUuid.toString()));
    }

    private void handleFriendDeny(ChannelMessage msg) {
        JsonObject p = payload(msg);
        UUID denierUuid = UUID.fromString(p.get("denierUuid").getAsString());
        String senderName = p.get("senderUsername").getAsString();
        friendsManager.denyFriendRequest(denierUuid, senderName);
    }

    private void handleFriendRemove(ChannelMessage msg) {
        JsonObject p = payload(msg);
        UUID playerUuid = UUID.fromString(p.get("playerUuid").getAsString());
        String targetName = p.get("targetUsername").getAsString();
        FriendsManager.FriendResult result = friendsManager.removeFriend(playerUuid, targetName);
        String resultPayload = String.format("{\"result\":\"%s\",\"target\":\"%s\"}", result.name(), targetName);
        messageSender.sendToPlayer(playerUuid, new ChannelMessage(MessageType.RESULT, resultPayload, playerUuid.toString()));
    }

    private void handleFriendJoin(ChannelMessage msg) {
        JsonObject p = payload(msg);
        UUID requesterUuid = UUID.fromString(p.get("requesterUuid").getAsString());
        String friendName = p.get("friendUsername").getAsString();
        FriendsManager.FriendResult result = friendsManager.joinFriend(requesterUuid, friendName);
        String resultPayload = String.format("{\"result\":\"%s\",\"target\":\"%s\"}", result.name(), friendName);
        messageSender.sendToPlayer(requesterUuid, new ChannelMessage(MessageType.RESULT, resultPayload, requesterUuid.toString()));
    }

    private void handlePartyCreate(ChannelMessage msg) {
        JsonObject p = payload(msg);
        UUID leaderUuid = UUID.fromString(p.get("leaderUuid").getAsString());
        partyManager.createParty(leaderUuid);
    }

    private void handlePartyInvite(ChannelMessage msg) {
        JsonObject p = payload(msg);
        UUID leaderUuid = UUID.fromString(p.get("leaderUuid").getAsString());
        String leaderName = p.get("leaderUsername").getAsString();
        String targetName = p.get("targetUsername").getAsString();
        PartyManager.PartyResult result = partyManager.invitePlayer(leaderUuid, leaderName, targetName);
        String resultPayload = String.format("{\"result\":\"%s\",\"target\":\"%s\"}", result.name(), targetName);
        messageSender.sendToPlayer(leaderUuid, new ChannelMessage(MessageType.RESULT, resultPayload, leaderUuid.toString()));
    }

    private void handlePartyAccept(ChannelMessage msg) {
        JsonObject p = payload(msg);
        UUID playerUuid = UUID.fromString(p.get("playerUuid").getAsString());
        String playerName = p.get("playerUsername").getAsString();
        String leaderName = p.get("leaderUsername").getAsString();
        partyManager.acceptInvite(playerUuid, playerName, leaderName);
    }

    private void handlePartyLeave(ChannelMessage msg) {
        JsonObject p = payload(msg);
        UUID playerUuid = UUID.fromString(p.get("playerUuid").getAsString());
        String playerName = p.get("playerUsername").getAsString();
        partyManager.leaveParty(playerUuid, playerName);
    }

    private void handlePartyKick(ChannelMessage msg) {
        JsonObject p = payload(msg);
        UUID leaderUuid = UUID.fromString(p.get("leaderUuid").getAsString());
        String targetName = p.get("targetUsername").getAsString();
        partyManager.kickMember(leaderUuid, targetName);
    }

    private void handlePartyDisband(ChannelMessage msg) {
        JsonObject p = payload(msg);
        UUID leaderUuid = UUID.fromString(p.get("leaderUuid").getAsString());
        partyManager.disbandParty(leaderUuid);
    }

    private void handlePartyWarp(ChannelMessage msg) {
        JsonObject p = payload(msg);
        UUID leaderUuid = UUID.fromString(p.get("leaderUuid").getAsString());
        String serverName = p.get("serverName").getAsString();
        partyManager.warpParty(leaderUuid, serverName);
    }

    private void handleFriendChat(ChannelMessage msg) {
        JsonObject p = payload(msg);
        UUID senderUuid = UUID.fromString(p.get("senderUuid").getAsString());
        String senderName = p.get("senderUsername").getAsString();
        String message = p.get("message").getAsString();
        String escaped = message.replace("\\", "\\\\").replace("\"", "\\\"");
        String chatPayload = String.format(
                "{\"senderUuid\":\"%s\",\"senderUsername\":\"%s\",\"message\":\"%s\"}",
                senderUuid, senderName, escaped);
        for (space.blockway.social.shared.dto.FriendDto friend : friendsManager.getFriendList(senderUuid)) {
            if (friend.isOnline()) {
                UUID friendUuid = UUID.fromString(friend.getUuid());
                messageSender.sendToPlayer(friendUuid,
                        new ChannelMessage(MessageType.FRIEND_CHAT_MESSAGE, chatPayload, friendUuid.toString()));
            }
        }
    }

    private void handlePartyChat(ChannelMessage msg) {
        JsonObject p = payload(msg);
        UUID senderUuid = UUID.fromString(p.get("senderUuid").getAsString());
        String senderName = p.get("senderUsername").getAsString();
        String message = p.get("message").getAsString();
        String partyId = p.has("partyId") ? p.get("partyId").getAsString() : null;
        if (partyId == null) return;
        partyManager.sendPartyChat(senderUuid, senderName, message, partyId);
    }

    private void handleLinkGenerate(ChannelMessage msg) {
        JsonObject p = payload(msg);
        UUID playerUuid = UUID.fromString(p.get("playerUuid").getAsString());
        if (linkManager.isLinked(playerUuid)) {
            messageSender.sendToPlayer(playerUuid,
                    new ChannelMessage(MessageType.RESULT,
                            "{\"result\":\"ALREADY_LINKED\"}", playerUuid.toString()));
            return;
        }
        String code = linkManager.generateCode(playerUuid);
        String responsePayload = String.format("{\"code\":\"%s\",\"expiryMinutes\":%d}",
                code, /* config not injected here, use 10 */ 10);
        messageSender.sendToPlayer(playerUuid,
                new ChannelMessage(MessageType.LINK_CODE_GENERATED, responsePayload, playerUuid.toString()));
    }

    private void handleLinkRemove(ChannelMessage msg) {
        JsonObject p = payload(msg);
        UUID playerUuid = UUID.fromString(p.get("playerUuid").getAsString());
        boolean removed = linkManager.unlink(playerUuid);
        MessageType responseType = removed ? MessageType.LINK_REMOVED : MessageType.LINK_NOT_FOUND;
        messageSender.sendToPlayer(playerUuid,
                new ChannelMessage(responseType, "{}", playerUuid.toString()));
    }
}
