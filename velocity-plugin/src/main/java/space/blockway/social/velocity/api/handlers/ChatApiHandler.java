package space.blockway.social.velocity.api.handlers;

import io.javalin.http.BadRequestResponse;
import io.javalin.http.Context;
import io.javalin.http.NotFoundResponse;
import space.blockway.social.shared.ChannelMessage;
import space.blockway.social.shared.MessageType;
import space.blockway.social.shared.dto.ChatMessageDto;
import space.blockway.social.velocity.config.VelocityConfig;
import space.blockway.social.velocity.database.ChatRepository;
import space.blockway.social.velocity.database.PlayerRepository;
import space.blockway.social.velocity.managers.PartyManager;
import space.blockway.social.velocity.managers.PresenceManager;
import space.blockway.social.velocity.messaging.MessageSender;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST handlers for chat message endpoints (friend and party channels).
 *
 * @author Enzonic LLC — blockway.space
 */
public class ChatApiHandler {

    private final ChatRepository chatRepository;
    private final PartyManager partyManager;
    private final PlayerRepository playerRepository;
    private final PresenceManager presenceManager;
    private final MessageSender messageSender;
    private final VelocityConfig config;

    public ChatApiHandler(ChatRepository chatRepository, PartyManager partyManager,
                           PlayerRepository playerRepository, PresenceManager presenceManager,
                           MessageSender messageSender, VelocityConfig config) {
        this.chatRepository = chatRepository;
        this.partyManager = partyManager;
        this.playerRepository = playerRepository;
        this.presenceManager = presenceManager;
        this.messageSender = messageSender;
        this.config = config;
    }

    /**
     * POST /api/v1/party/chat
     * Body: { "partyId": "...", "senderUsername": "Dashboard", "message": "..." }
     * Sends a message into a party chat from the dashboard.
     */
    public void sendPartyChat(Context ctx) {
        Map<String, Object> body = ctx.bodyAsClass(Map.class);
        String partyId = (String) body.get("partyId");
        String senderUsername = (String) body.getOrDefault("senderUsername", "Dashboard");
        String message = (String) body.get("message");

        if (partyId == null || message == null || message.isBlank()) {
            throw new BadRequestResponse("partyId and message are required");
        }

        // Use a sentinel UUID for dashboard-originated messages
        UUID dashboardUuid = new UUID(0, 0);
        partyManager.sendPartyChat(dashboardUuid, "[" + senderUsername + "]", message, partyId);
        ctx.json(Map.of("success", true));
    }

    /**
     * GET /api/v1/party/{partyId}/messages?limit=50
     * Returns recent party chat messages, oldest first.
     */
    public void getPartyMessages(Context ctx) {
        String partyId = ctx.pathParam("partyId");
        int limit = Math.min(ctx.queryParamAsClass("limit", Integer.class).getOrDefault(50), 100);
        List<ChatMessageDto> messages = chatRepository.getRecentMessages("PARTY", partyId, limit);
        ctx.json(Map.of("partyId", partyId, "messages", messages));
    }

    /**
     * POST /api/v1/friend/chat
     * Body: { "senderUuid": "...", "receiverUuid": "...", "senderUsername": "Dashboard", "message": "..." }
     * Sends a message into a friend chat channel from the dashboard.
     */
    public void sendFriendChat(Context ctx) {
        Map<String, Object> body = ctx.bodyAsClass(Map.class);
        String senderUuidStr = (String) body.get("senderUuid");
        String receiverUuidStr = (String) body.get("receiverUuid");
        String senderUsername = (String) body.getOrDefault("senderUsername", "Dashboard");
        String message = (String) body.get("message");

        if (senderUuidStr == null || receiverUuidStr == null || message == null || message.isBlank()) {
            throw new BadRequestResponse("senderUuid, receiverUuid, and message are required");
        }

        UUID senderUuid, receiverUuid;
        try {
            senderUuid = UUID.fromString(senderUuidStr);
            receiverUuid = UUID.fromString(receiverUuidStr);
        } catch (IllegalArgumentException e) {
            throw new BadRequestResponse("Invalid UUID format");
        }

        String channelId = ChatRepository.friendChannelId(senderUuid, receiverUuid);
        chatRepository.saveMessage("FRIEND", channelId, senderUuid, senderUsername, message);
        chatRepository.pruneMessages("FRIEND", channelId, config.getChatHistoryLimit());

        // Deliver to online participants
        String payload = String.format(
                "{\"senderUuid\":\"%s\",\"senderUsername\":\"%s\",\"message\":\"%s\",\"channelId\":\"%s\"}",
                senderUuid, escapeJson(senderUsername), escapeJson(message), channelId);
        ChannelMessage msg = new ChannelMessage(MessageType.FRIEND_CHAT_MESSAGE, payload, null);

        if (presenceManager.isOnline(senderUuid)) messageSender.sendToPlayer(senderUuid, msg);
        if (presenceManager.isOnline(receiverUuid)) messageSender.sendToPlayer(receiverUuid, msg);

        ctx.json(Map.of("success", true, "channelId", channelId));
    }

    /**
     * GET /api/v1/friend/{uuid}/messages?withUuid=...&limit=50
     * Returns recent friend chat messages between two players.
     */
    public void getFriendMessages(Context ctx) {
        String uuidStr = ctx.pathParam("uuid");
        String withUuidStr = ctx.queryParam("withUuid");
        int limit = Math.min(ctx.queryParamAsClass("limit", Integer.class).getOrDefault(50), 100);

        if (withUuidStr == null) throw new BadRequestResponse("withUuid query parameter is required");

        UUID uuid, withUuid;
        try {
            uuid = UUID.fromString(uuidStr);
            withUuid = UUID.fromString(withUuidStr);
        } catch (IllegalArgumentException e) {
            throw new BadRequestResponse("Invalid UUID format");
        }

        String channelId = ChatRepository.friendChannelId(uuid, withUuid);
        List<ChatMessageDto> messages = chatRepository.getRecentMessages("FRIEND", channelId, limit);
        ctx.json(Map.of("channelId", channelId, "messages", messages));
    }

    private String escapeJson(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
