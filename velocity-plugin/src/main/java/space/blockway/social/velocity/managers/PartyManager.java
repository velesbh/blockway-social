package space.blockway.social.velocity.managers;

import com.velocitypowered.api.proxy.ProxyServer;
import org.slf4j.Logger;
import space.blockway.social.shared.ChannelMessage;
import space.blockway.social.shared.MessageType;
import space.blockway.social.shared.dto.FriendDto;
import space.blockway.social.shared.dto.PartyDto;
import space.blockway.social.velocity.config.VelocityConfig;
import space.blockway.social.velocity.database.*;
import space.blockway.social.velocity.messaging.MessageSender;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Cross-server party management. All party state is authoritative in the database.
 *
 * @author Enzonic LLC — blockway.space
 */
public class PartyManager {

    public enum PartyResult {
        SUCCESS, NOT_IN_PARTY, ALREADY_IN_PARTY, NOT_LEADER,
        PLAYER_NOT_FOUND, PARTY_FULL, SERVER_NOT_FOUND, ALREADY_LEADER,
        CANNOT_KICK_SELF, PLAYER_NOT_IN_PARTY
    }

    private final PartyRepository partyRepo;
    private final PlayerRepository playerRepo;
    private final ChatRepository chatRepo;
    private final PresenceManager presence;
    private final MessageSender messageSender;
    private final ProxyServer proxy;
    private final VelocityConfig config;
    private final Logger logger;

    public PartyManager(PartyRepository partyRepo, PlayerRepository playerRepo,
                        ChatRepository chatRepo, PresenceManager presence,
                        MessageSender messageSender, ProxyServer proxy,
                        VelocityConfig config, Logger logger) {
        this.partyRepo = partyRepo;
        this.playerRepo = playerRepo;
        this.chatRepo = chatRepo;
        this.presence = presence;
        this.messageSender = messageSender;
        this.proxy = proxy;
        this.config = config;
        this.logger = logger;
    }

    public PartyResult createParty(UUID leaderUuid) {
        if (partyRepo.findPartyIdByMember(leaderUuid).isPresent()) return PartyResult.ALREADY_IN_PARTY;
        String partyId = UUID.randomUUID().toString();
        partyRepo.createParty(partyId, leaderUuid);
        // Notify leader
        String payload = String.format("{\"partyId\":\"%s\"}", partyId);
        messageSender.sendToPlayer(leaderUuid, new ChannelMessage(MessageType.PARTY_CREATED, payload, leaderUuid.toString()));
        return PartyResult.SUCCESS;
    }

    public PartyResult disbandParty(UUID leaderUuid) {
        Optional<String> partyIdOpt = partyRepo.findPartyIdByMember(leaderUuid);
        if (partyIdOpt.isEmpty()) return PartyResult.NOT_IN_PARTY;
        String partyId = partyIdOpt.get();
        Optional<PartyRepository.PartyRecord> partyOpt = partyRepo.findById(partyId);
        if (partyOpt.isEmpty() || !partyOpt.get().leaderUuid().equals(leaderUuid)) return PartyResult.NOT_LEADER;

        List<PartyRepository.MemberRecord> members = partyRepo.getMembers(partyId);
        partyRepo.deleteParty(partyId);

        ChannelMessage msg = new ChannelMessage(MessageType.PARTY_DISBANDED, "{}", null);
        for (PartyRepository.MemberRecord m : members) {
            if (presence.isOnline(m.playerUuid())) {
                messageSender.sendToPlayer(m.playerUuid(), msg);
            }
        }
        return PartyResult.SUCCESS;
    }

    public PartyResult invitePlayer(UUID leaderUuid, String leaderName, String targetName) {
        Optional<String> partyIdOpt = partyRepo.findPartyIdByMember(leaderUuid);
        if (partyIdOpt.isEmpty()) return PartyResult.NOT_IN_PARTY;
        String partyId = partyIdOpt.get();
        if (!partyRepo.findById(partyId).map(p -> p.leaderUuid().equals(leaderUuid)).orElse(false))
            return PartyResult.NOT_LEADER;

        if (partyRepo.countMembers(partyId) >= config.getMaxPartySize()) return PartyResult.PARTY_FULL;

        Optional<UUID> targetOpt = presence.getUuidByUsername(targetName)
                .or(() -> playerRepo.findByUsername(targetName).map(r -> r.uuid()));
        if (targetOpt.isEmpty()) return PartyResult.PLAYER_NOT_FOUND;
        UUID targetUuid = targetOpt.get();

        if (partyRepo.findPartyIdByMember(targetUuid).isPresent()) return PartyResult.ALREADY_IN_PARTY;

        String payload = String.format("{\"leaderUuid\":\"%s\",\"leaderUsername\":\"%s\",\"partyId\":\"%s\"}",
                leaderUuid, leaderName, partyId);
        messageSender.sendToPlayer(targetUuid,
                new ChannelMessage(MessageType.PARTY_INVITE_RECEIVED, payload, targetUuid.toString()));
        return PartyResult.SUCCESS;
    }

    public PartyResult acceptInvite(UUID playerUuid, String playerName, String leaderName) {
        Optional<UUID> leaderOpt = presence.getUuidByUsername(leaderName)
                .or(() -> playerRepo.findByUsername(leaderName).map(r -> r.uuid()));
        if (leaderOpt.isEmpty()) return PartyResult.PLAYER_NOT_FOUND;
        UUID leaderUuid = leaderOpt.get();

        Optional<String> partyIdOpt = partyRepo.findPartyIdByMember(leaderUuid);
        if (partyIdOpt.isEmpty()) return PartyResult.NOT_IN_PARTY;
        String partyId = partyIdOpt.get();

        if (partyRepo.findPartyIdByMember(playerUuid).isPresent()) return PartyResult.ALREADY_IN_PARTY;
        if (partyRepo.countMembers(partyId) >= config.getMaxPartySize()) return PartyResult.PARTY_FULL;

        partyRepo.addMember(partyId, playerUuid);

        // Notify all members
        String payload = String.format("{\"uuid\":\"%s\",\"username\":\"%s\",\"partyId\":\"%s\"}",
                playerUuid, playerName, partyId);
        ChannelMessage msg = new ChannelMessage(MessageType.PARTY_MEMBER_JOINED, payload, null);
        for (PartyRepository.MemberRecord m : partyRepo.getMembers(partyId)) {
            if (presence.isOnline(m.playerUuid())) messageSender.sendToPlayer(m.playerUuid(), msg);
        }
        return PartyResult.SUCCESS;
    }

    public PartyResult leaveParty(UUID playerUuid, String playerName) {
        Optional<String> partyIdOpt = partyRepo.findPartyIdByMember(playerUuid);
        if (partyIdOpt.isEmpty()) return PartyResult.NOT_IN_PARTY;
        String partyId = partyIdOpt.get();

        Optional<PartyRepository.PartyRecord> partyOpt = partyRepo.findById(partyId);
        if (partyOpt.isEmpty()) return PartyResult.NOT_IN_PARTY;

        partyRepo.removeMember(partyId, playerUuid);

        List<PartyRepository.MemberRecord> remaining = partyRepo.getMembers(partyId);
        if (remaining.isEmpty()) {
            partyRepo.deleteParty(partyId);
            return PartyResult.SUCCESS;
        }

        // Transfer leadership if leader left
        if (partyOpt.get().leaderUuid().equals(playerUuid)) {
            UUID newLeader = remaining.get(0).playerUuid();
            partyRepo.updateLeader(partyId, newLeader);
            String ltPayload = String.format("{\"partyId\":\"%s\"}", partyId);
            if (presence.isOnline(newLeader))
                messageSender.sendToPlayer(newLeader, new ChannelMessage(MessageType.PARTY_LEADER_TRANSFERRED, ltPayload, newLeader.toString()));
        }

        String payload = String.format("{\"uuid\":\"%s\",\"username\":\"%s\",\"partyId\":\"%s\"}",
                playerUuid, playerName, partyId);
        ChannelMessage msg = new ChannelMessage(MessageType.PARTY_MEMBER_LEFT, payload, null);
        for (PartyRepository.MemberRecord m : remaining) {
            if (presence.isOnline(m.playerUuid())) messageSender.sendToPlayer(m.playerUuid(), msg);
        }
        return PartyResult.SUCCESS;
    }

    public PartyResult kickMember(UUID leaderUuid, String targetName) {
        Optional<String> partyIdOpt = partyRepo.findPartyIdByMember(leaderUuid);
        if (partyIdOpt.isEmpty()) return PartyResult.NOT_IN_PARTY;
        String partyId = partyIdOpt.get();
        if (!partyRepo.findById(partyId).map(p -> p.leaderUuid().equals(leaderUuid)).orElse(false))
            return PartyResult.NOT_LEADER;

        Optional<UUID> targetOpt = presence.getUuidByUsername(targetName)
                .or(() -> playerRepo.findByUsername(targetName).map(r -> r.uuid()));
        if (targetOpt.isEmpty()) return PartyResult.PLAYER_NOT_FOUND;
        UUID targetUuid = targetOpt.get();
        if (targetUuid.equals(leaderUuid)) return PartyResult.CANNOT_KICK_SELF;

        if (partyRepo.findPartyIdByMember(targetUuid).map(id -> !id.equals(partyId)).orElse(true))
            return PartyResult.PLAYER_NOT_IN_PARTY;

        partyRepo.removeMember(partyId, targetUuid);

        String kickedPayload = String.format("{\"partyId\":\"%s\"}", partyId);
        if (presence.isOnline(targetUuid))
            messageSender.sendToPlayer(targetUuid, new ChannelMessage(MessageType.PARTY_KICKED, kickedPayload, targetUuid.toString()));

        String payload = String.format("{\"uuid\":\"%s\",\"username\":\"%s\",\"partyId\":\"%s\"}",
                targetUuid, targetName, partyId);
        ChannelMessage leaveMsg = new ChannelMessage(MessageType.PARTY_MEMBER_LEFT, payload, null);
        for (PartyRepository.MemberRecord m : partyRepo.getMembers(partyId)) {
            if (presence.isOnline(m.playerUuid())) messageSender.sendToPlayer(m.playerUuid(), leaveMsg);
        }
        return PartyResult.SUCCESS;
    }

    public PartyResult sendPartyChat(UUID senderUuid, String senderName, String message, String partyId) {
        chatRepo.saveMessage("PARTY", partyId, senderUuid, senderName, message);
        chatRepo.pruneMessages("PARTY", partyId, config.getChatHistoryLimit());

        String payload = String.format("{\"senderUuid\":\"%s\",\"senderUsername\":\"%s\",\"message\":\"%s\",\"partyId\":\"%s\"}",
                senderUuid, escapeJson(senderName), escapeJson(message), partyId);
        ChannelMessage msg = new ChannelMessage(MessageType.PARTY_CHAT_MESSAGE, payload, null);
        for (PartyRepository.MemberRecord m : partyRepo.getMembers(partyId)) {
            if (presence.isOnline(m.playerUuid())) messageSender.sendToPlayer(m.playerUuid(), msg);
        }
        return PartyResult.SUCCESS;
    }

    public PartyResult warpParty(UUID leaderUuid, String serverName) {
        Optional<String> partyIdOpt = partyRepo.findPartyIdByMember(leaderUuid);
        if (partyIdOpt.isEmpty()) return PartyResult.NOT_IN_PARTY;
        String partyId = partyIdOpt.get();
        if (!partyRepo.findById(partyId).map(p -> p.leaderUuid().equals(leaderUuid)).orElse(false))
            return PartyResult.NOT_LEADER;
        if (proxy.getServer(serverName).isEmpty()) return PartyResult.SERVER_NOT_FOUND;

        var server = proxy.getServer(serverName).get();
        for (PartyRepository.MemberRecord m : partyRepo.getMembers(partyId)) {
            proxy.getPlayer(m.playerUuid()).ifPresent(p -> p.createConnectionRequest(server).fireAndForget());
        }
        return PartyResult.SUCCESS;
    }

    public Optional<PartyDto> getParty(UUID playerUuid) {
        return partyRepo.findPartyIdByMember(playerUuid).flatMap(this::getPartyById);
    }

    public Optional<PartyDto> getPartyById(String partyId) {
        return partyRepo.findById(partyId).map(pr -> {
            List<PartyRepository.MemberRecord> members = partyRepo.getMembers(partyId);
            PartyDto dto = new PartyDto();
            dto.setPartyId(partyId);
            dto.setLeaderUuid(pr.leaderUuid().toString());
            dto.setLeaderUsername(presence.getUsername(pr.leaderUuid())
                    .or(() -> playerRepo.findByUuid(pr.leaderUuid()).map(p -> p.username())).orElse("Unknown"));
            dto.setMemberUuids(members.stream().map(m -> m.playerUuid().toString()).collect(Collectors.toList()));
            dto.setMemberCount(members.size());
            dto.setCreatedAt(pr.createdAt());
            return dto;
        });
    }

    private String escapeJson(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
