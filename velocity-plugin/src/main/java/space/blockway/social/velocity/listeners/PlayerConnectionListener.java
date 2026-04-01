package space.blockway.social.velocity.listeners;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.proxy.Player;
import org.slf4j.Logger;
import space.blockway.social.velocity.database.PlayerRepository;
import space.blockway.social.velocity.managers.FriendsManager;
import space.blockway.social.velocity.managers.PresenceManager;

import java.util.UUID;

/**
 * Handles Velocity-native player connection lifecycle events.
 *
 * <p>On first server connection: marks player online, broadcasts FRIEND_ONLINE to all online friends.
 * On server switch: updates the current server in PresenceManager.
 * On disconnect: marks player offline, broadcasts FRIEND_OFFLINE.
 *
 * @author Enzonic LLC — blockway.space
 */
public class PlayerConnectionListener {

    private final PresenceManager presenceManager;
    private final FriendsManager friendsManager;
    private final PlayerRepository playerRepository;
    private final Logger logger;

    public PlayerConnectionListener(PresenceManager presenceManager, FriendsManager friendsManager,
                                     PlayerRepository playerRepository, Logger logger) {
        this.presenceManager = presenceManager;
        this.friendsManager = friendsManager;
        this.playerRepository = playerRepository;
        this.logger = logger;
    }

    @Subscribe
    public void onPostLogin(PostLoginEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        String username = player.getUsername();
        // Upsert player record (update username on every login)
        playerRepository.upsertPlayer(uuid, username);
    }

    @Subscribe
    public void onServerConnected(ServerConnectedEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        String username = player.getUsername();
        String serverName = event.getServer().getServerInfo().getName();

        boolean firstConnection = event.getPreviousServer().isEmpty();

        if (firstConnection) {
            presenceManager.markOnline(uuid, username, serverName);
            playerRepository.updateLastSeen(uuid, System.currentTimeMillis());
            playerRepository.updateLastServer(uuid, serverName);
            // Broadcast to online friends
            friendsManager.broadcastFriendOnline(uuid, username, serverName);
        } else {
            presenceManager.updateServer(uuid, serverName);
            playerRepository.updateLastServer(uuid, serverName);
        }
    }

    @Subscribe
    public void onDisconnect(DisconnectEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        String username = player.getUsername();

        if (presenceManager.isOnline(uuid)) {
            presenceManager.markOffline(uuid);
            playerRepository.updateLastSeen(uuid, System.currentTimeMillis());
            friendsManager.broadcastFriendOffline(uuid, username);
            logger.debug("Player {} disconnected from proxy", username);
        }
    }
}
