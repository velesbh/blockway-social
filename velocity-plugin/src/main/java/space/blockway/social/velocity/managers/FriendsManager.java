package space.blockway.social.velocity.managers;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import org.slf4j.Logger;
import space.blockway.social.shared.ChannelMessage;
import space.blockway.social.shared.MessageType;
import space.blockway.social.shared.dto.FriendDto;
import space.blockway.social.velocity.config.VelocityConfig;
import space.blockway.social.velocity.database.FriendsRepository;
import space.blockway.social.velocity.database.PlayerRepository;
import space.blockway.social.velocity.database.PlayerSettingsRepository;
import space.blockway.social.velocity.messaging.MessageSender;

import java.util.*;

/**
 * Orchestrates all friend-related business logic across servers.
 *
 * @author Enzonic LLC — blockway.space
 */
public class FriendsManager {

    public enum FriendResult {
        SUCCESS, ALREADY_FRIENDS, REQUEST_ALREADY_SENT, REQUEST_NOT_FOUND,
        NOT_FRIENDS, PLAYER_NOT_FOUND, CANNOT_ADD_SELF, MAX_FRIENDS_REACHED,
        SERVER_RESTRICTED, REQUESTS_DISABLED
    }

    private final FriendsRepository friendsRepo;
    private final PlayerRepository playerRepo;
    private final PlayerSettingsRepository settingsRepo;
    private final PresenceManager presence;
    private final MessageSender messageSender;
    private final ProxyServer proxy;
    private final VelocityConfig config;
    private final Logger logger;

    public FriendsManager(FriendsRepository friendsRepo, PlayerRepository playerRepo,
                          PlayerSettingsRepository settingsRepo, PresenceManager presence,
                          MessageSender messageSender, ProxyServer proxy,
                          VelocityConfig config, Logger logger) {
        this.friendsRepo = friendsRepo;
        this.playerRepo = playerRepo;
        this.settingsRepo = settingsRepo;
        this.presence = presence;
        this.messageSender = messageSender;
        this.proxy = proxy;
        this.config = config;
        this.logger = logger;
    }

    public FriendResult sendFriendRequest(UUID senderUuid, String senderName, String targetName) {
        if (senderName.equalsIgnoreCase(targetName)) return FriendResult.CANNOT_ADD_SELF;

        // Resolve target UUID
        Optional<PlayerRepository.PlayerRecord> targetOpt = presence.getUuidByUsername(targetName)
                .map(uuid -> playerRepo.findByUuid(uuid).orElse(null))
                .or(() -> playerRepo.findByUsername(targetName));
        if (targetOpt.isEmpty()) return FriendResult.PLAYER_NOT_FOUND;

        UUID targetUuid = targetOpt.get().uuid();
        if (friendsRepo.areFriends(senderUuid, targetUuid)) return FriendResult.ALREADY_FRIENDS;
        if (friendsRepo.requestExists(senderUuid, targetUuid)) return FriendResult.REQUEST_ALREADY_SENT;
        if (friendsRepo.countFriends(senderUuid) >= config.getMaxFriends()) return FriendResult.MAX_FRIENDS_REACHED;

        PlayerSettingsRepository.PlayerSettings targetSettings = settingsRepo.getSettings(targetUuid);
        if (!targetSettings.acceptRequests()) return FriendResult.REQUESTS_DISABLED;

        friendsRepo.createFriendRequest(senderUuid, targetUuid);

        // Notify receiver if online
        if (presence.isOnline(targetUuid)) {
            String payload = String.format("{\"senderUuid\":\"%s\",\"senderUsername\":\"%s\"}",
                    senderUuid, senderName);
            messageSender.sendToPlayer(targetUuid,
                    new ChannelMessage(MessageType.FRIEND_REQUEST_RECEIVED, payload, targetUuid.toString()));
        }
        return FriendResult.SUCCESS;
    }

    public FriendResult acceptFriendRequest(UUID accepterUuid, String accepterName, String senderName) {
        Optional<UUID> senderUuidOpt = presence.getUuidByUsername(senderName)
                .or(() -> playerRepo.findByUsername(senderName).map(r -> r.uuid()));
        if (senderUuidOpt.isEmpty()) return FriendResult.PLAYER_NOT_FOUND;

        UUID senderUuid = senderUuidOpt.get();
        if (!friendsRepo.requestExists(senderUuid, accepterUuid)) return FriendResult.REQUEST_NOT_FOUND;

        friendsRepo.deleteRequest(senderUuid, accepterUuid);
        friendsRepo.addFriendship(accepterUuid, senderUuid);

        // Notify both parties
        String payloadToSender = String.format("{\"friendUuid\":\"%s\",\"friendUsername\":\"%s\"}",
                accepterUuid, accepterName);
        if (presence.isOnline(senderUuid)) {
            messageSender.sendToPlayer(senderUuid,
                    new ChannelMessage(MessageType.FRIEND_REQUEST_ACCEPTED, payloadToSender, senderUuid.toString()));
        }
        return FriendResult.SUCCESS;
    }

    public FriendResult denyFriendRequest(UUID denierUuid, String senderName) {
        Optional<UUID> senderUuidOpt = presence.getUuidByUsername(senderName)
                .or(() -> playerRepo.findByUsername(senderName).map(r -> r.uuid()));
        if (senderUuidOpt.isEmpty()) return FriendResult.PLAYER_NOT_FOUND;

        UUID senderUuid = senderUuidOpt.get();
        if (!friendsRepo.requestExists(senderUuid, denierUuid)) return FriendResult.REQUEST_NOT_FOUND;

        friendsRepo.deleteRequest(senderUuid, denierUuid);

        String payload = String.format("{\"denierUuid\":\"%s\"}", denierUuid);
        if (presence.isOnline(senderUuid)) {
            messageSender.sendToPlayer(senderUuid,
                    new ChannelMessage(MessageType.FRIEND_REQUEST_DENIED, payload, senderUuid.toString()));
        }
        return FriendResult.SUCCESS;
    }

    public FriendResult removeFriend(UUID playerUuid, String targetName) {
        Optional<UUID> targetOpt = presence.getUuidByUsername(targetName)
                .or(() -> playerRepo.findByUsername(targetName).map(r -> r.uuid()));
        if (targetOpt.isEmpty()) return FriendResult.PLAYER_NOT_FOUND;

        UUID targetUuid = targetOpt.get();
        if (!friendsRepo.areFriends(playerUuid, targetUuid)) return FriendResult.NOT_FRIENDS;

        friendsRepo.removeFriendship(playerUuid, targetUuid);
        return FriendResult.SUCCESS;
    }

    public FriendResult joinFriend(UUID requesterUuid, String friendName) {
        Optional<UUID> friendUuidOpt = presence.getUuidByUsername(friendName)
                .or(() -> playerRepo.findByUsername(friendName).map(r -> r.uuid()));
        if (friendUuidOpt.isEmpty()) return FriendResult.PLAYER_NOT_FOUND;

        UUID friendUuid = friendUuidOpt.get();
        if (!friendsRepo.areFriends(requesterUuid, friendUuid)) return FriendResult.NOT_FRIENDS;
        if (!presence.isOnline(friendUuid)) return FriendResult.PLAYER_NOT_FOUND;

        PlayerSettingsRepository.PlayerSettings friendSettings = settingsRepo.getSettings(friendUuid);
        if (friendSettings.privateServer()) return FriendResult.SERVER_RESTRICTED;

        Optional<String> serverOpt = presence.getServer(friendUuid);
        if (serverOpt.isEmpty()) return FriendResult.PLAYER_NOT_FOUND;

        // Move the requester via Velocity's server connection API
        proxy.getPlayer(requesterUuid).ifPresent(player ->
            proxy.getServer(serverOpt.get()).ifPresent(server ->
                player.createConnectionRequest(server).fireAndForget()
            )
        );
        return FriendResult.SUCCESS;
    }

    public List<FriendDto> getFriendList(UUID playerUuid) {
        List<FriendsRepository.FriendRecord> records = friendsRepo.getFriends(playerUuid);
        List<FriendDto> list = new ArrayList<>();
        for (FriendsRepository.FriendRecord r : records) {
            PlayerRepository.PlayerRecord pr = playerRepo.findByUuid(r.friendUuid()).orElse(null);
            if (pr == null) continue;
            FriendDto dto = new FriendDto();
            dto.setUuid(r.friendUuid().toString());
            dto.setUsername(pr.username());
            dto.setOnline(presence.isOnline(r.friendUuid()));
            dto.setOnlineServer(presence.getServer(r.friendUuid()).orElse(null));
            dto.setFriendedAt(r.createdAt());
            dto.setLastSeen(pr.lastSeen());
            list.add(dto);
        }
        return list;
    }

    public void broadcastFriendOnline(UUID playerUuid, String username, String serverName) {
        String payload = String.format("{\"uuid\":\"%s\",\"username\":\"%s\",\"serverName\":\"%s\"}",
                playerUuid, username, serverName);
        ChannelMessage msg = new ChannelMessage(MessageType.FRIEND_ONLINE, payload);
        for (FriendsRepository.FriendRecord r : friendsRepo.getFriends(playerUuid)) {
            if (presence.isOnline(r.friendUuid())) {
                PlayerSettingsRepository.PlayerSettings settings = settingsRepo.getSettings(r.friendUuid());
                if (settings.notifications()) {
                    messageSender.sendToPlayer(r.friendUuid(), msg);
                }
            }
        }
    }

    public void broadcastFriendOffline(UUID playerUuid, String username) {
        String payload = String.format("{\"uuid\":\"%s\",\"username\":\"%s\"}", playerUuid, username);
        ChannelMessage msg = new ChannelMessage(MessageType.FRIEND_OFFLINE, payload);
        for (FriendsRepository.FriendRecord r : friendsRepo.getFriends(playerUuid)) {
            if (presence.isOnline(r.friendUuid())) {
                messageSender.sendToPlayer(r.friendUuid(), msg);
            }
        }
    }

    public List<UUID> getIncomingRequests(UUID playerUuid) {
        return friendsRepo.getIncomingRequestSenders(playerUuid);
    }

    public boolean areFriends(UUID a, UUID b) {
        return friendsRepo.areFriends(a, b);
    }
}
