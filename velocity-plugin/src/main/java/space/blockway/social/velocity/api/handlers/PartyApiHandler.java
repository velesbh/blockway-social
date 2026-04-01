package space.blockway.social.velocity.api.handlers;

import io.javalin.http.Context;
import io.javalin.http.NotFoundResponse;
import space.blockway.social.shared.dto.PartyDto;
import space.blockway.social.velocity.database.PlayerRepository;
import space.blockway.social.velocity.managers.PartyManager;
import space.blockway.social.velocity.managers.PresenceManager;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * REST handlers for party-related endpoints.
 *
 * @author Enzonic LLC — blockway.space
 */
public class PartyApiHandler {

    private final PartyManager partyManager;
    private final PlayerRepository playerRepository;
    private final PresenceManager presenceManager;

    public PartyApiHandler(PartyManager partyManager, PlayerRepository playerRepository,
                            PresenceManager presenceManager) {
        this.partyManager = partyManager;
        this.playerRepository = playerRepository;
        this.presenceManager = presenceManager;
    }

    /**
     * GET /api/v1/player/{username}/party
     * Returns the current party for the given player, or null if not in one.
     */
    public void getPlayerParty(Context ctx) {
        String username = ctx.pathParam("username");
        UUID uuid = presenceManager.getUuidByUsername(username)
                .or(() -> playerRepository.findByUsername(username).map(r -> r.uuid()))
                .orElseThrow(() -> new NotFoundResponse("Player not found: " + username));

        Optional<PartyDto> party = partyManager.getParty(uuid);
        ctx.json(Map.of(
                "username", username,
                "uuid", uuid.toString(),
                "inParty", party.isPresent(),
                "party", party.orElse(null)
        ));
    }

    /**
     * GET /api/v1/party/{partyId}
     * Returns party details by party ID.
     */
    public void getPartyById(Context ctx) {
        String partyId = ctx.pathParam("partyId");
        PartyDto party = partyManager.getPartyById(partyId)
                .orElseThrow(() -> new NotFoundResponse("Party not found: " + partyId));
        ctx.json(party);
    }
}
