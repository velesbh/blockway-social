package space.blockway.social.velocity.api.handlers;

import io.javalin.http.Context;
import io.javalin.http.NotFoundResponse;
import space.blockway.social.shared.dto.PlayerStatusDto;
import space.blockway.social.velocity.database.LinkRepository;
import space.blockway.social.velocity.database.PlayerRepository;
import space.blockway.social.velocity.managers.PresenceManager;

import java.util.UUID;

/**
 * REST handlers for player status endpoints.
 *
 * @author Enzonic LLC — blockway.space
 */
public class PlayerApiHandler {

    private final PlayerRepository playerRepository;
    private final PresenceManager presenceManager;
    private final LinkRepository linkRepository;

    public PlayerApiHandler(PlayerRepository playerRepository, PresenceManager presenceManager,
                             LinkRepository linkRepository) {
        this.playerRepository = playerRepository;
        this.presenceManager = presenceManager;
        this.linkRepository = linkRepository;
    }

    /**
     * GET /api/v1/player/{username}/status
     * Returns online status, current server, last seen, and link status for a player.
     */
    public void getPlayerStatus(Context ctx) {
        String username = ctx.pathParam("username");

        // Try online first (fast path), then DB fallback
        UUID uuid = presenceManager.getUuidByUsername(username)
                .or(() -> playerRepository.findByUsername(username).map(r -> r.uuid()))
                .orElseThrow(() -> new NotFoundResponse("Player not found: " + username));

        PlayerStatusDto dto = new PlayerStatusDto();
        dto.setUuid(uuid.toString());
        dto.setUsername(username);
        dto.setOnline(presenceManager.isOnline(uuid));
        dto.setCurrentServer(presenceManager.getServer(uuid).orElse(null));
        dto.setLinked(linkRepository.isLinked(uuid));

        // Enrich with DB last_seen
        playerRepository.findByUuid(uuid).ifPresent(r -> dto.setLastSeen(r.lastSeen()));

        ctx.json(dto);
    }
}
