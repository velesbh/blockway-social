package space.blockway.social.velocity.managers;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory registry of currently online players on the Velocity proxy.
 * This is the single source of truth for online status — no DB queries needed.
 *
 * <p>All methods are thread-safe (backed by ConcurrentHashMap).
 *
 * @author Enzonic LLC — blockway.space
 */
public class PresenceManager {

    /** UUID → current server name */
    private final Map<UUID, String> playerServers = new ConcurrentHashMap<>();

    /** UUID → username (cached for reverse lookups) */
    private final Map<UUID, String> playerUsernames = new ConcurrentHashMap<>();

    /** username (lowercase) → UUID */
    private final Map<String, UUID> usernameIndex = new ConcurrentHashMap<>();

    public void markOnline(UUID uuid, String username, String serverName) {
        playerServers.put(uuid, serverName);
        playerUsernames.put(uuid, username);
        usernameIndex.put(username.toLowerCase(), uuid);
    }

    public void markOffline(UUID uuid) {
        String username = playerUsernames.remove(uuid);
        if (username != null) usernameIndex.remove(username.toLowerCase());
        playerServers.remove(uuid);
    }

    public void updateServer(UUID uuid, String serverName) {
        if (playerServers.containsKey(uuid)) {
            playerServers.put(uuid, serverName);
        }
    }

    public boolean isOnline(UUID uuid) {
        return playerServers.containsKey(uuid);
    }

    public Optional<String> getServer(UUID uuid) {
        return Optional.ofNullable(playerServers.get(uuid));
    }

    public Optional<String> getUsername(UUID uuid) {
        return Optional.ofNullable(playerUsernames.get(uuid));
    }

    public Optional<UUID> getUuidByUsername(String username) {
        return Optional.ofNullable(usernameIndex.get(username.toLowerCase()));
    }

    public Set<UUID> getOnlinePlayers() {
        return Collections.unmodifiableSet(playerServers.keySet());
    }
}
