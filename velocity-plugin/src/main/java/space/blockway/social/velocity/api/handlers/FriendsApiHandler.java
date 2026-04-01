package space.blockway.social.velocity.api.handlers;

import io.javalin.http.Context;
import io.javalin.http.NotFoundResponse;
import space.blockway.social.shared.dto.FriendDto;
import space.blockway.social.velocity.database.PlayerRepository;
import space.blockway.social.velocity.managers.FriendsManager;
import space.blockway.social.velocity.managers.PresenceManager;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST handlers for friend-related endpoints.
 *
 * @author Enzonic LLC — blockway.space
 */
public class FriendsApiHandler {

    private final FriendsManager friendsManager;
    private final PlayerRepository playerRepository;
    private final PresenceManager presenceManager;

    public FriendsApiHandler(FriendsManager friendsManager, PlayerRepository playerRepository,
                              PresenceManager presenceManager) {
        this.friendsManager = friendsManager;
        this.playerRepository = playerRepository;
        this.presenceManager = presenceManager;
    }

    /**
     * GET /api/v1/player/{username}/friends
     * Returns the friend list for the given player username.
     */
    public void getFriends(Context ctx) {
        String username = ctx.pathParam("username");
        UUID uuid = resolveUuid(username);

        List<FriendDto> friends = friendsManager.getFriendList(uuid);
        ctx.json(Map.of(
                "username", username,
                "uuid", uuid.toString(),
                "friendCount", friends.size(),
                "friends", friends
        ));
    }

    private UUID resolveUuid(String username) {
        return presenceManager.getUuidByUsername(username)
                .or(() -> playerRepository.findByUsername(username).map(r -> r.uuid()))
                .orElseThrow(() -> new NotFoundResponse("Player not found: " + username));
    }
}
