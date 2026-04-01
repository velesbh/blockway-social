package space.blockway.social.velocity.api.handlers;

import io.javalin.http.BadRequestResponse;
import io.javalin.http.Context;
import io.javalin.http.NotFoundResponse;
import space.blockway.social.velocity.database.LinkRepository;
import space.blockway.social.velocity.managers.LinkManager;

import java.util.Map;
import java.util.UUID;

/**
 * REST handlers for the web account link endpoints.
 *
 * <p>Link flow:
 * <ol>
 *   <li>Player runs {@code /link} in-game → Velocity generates a code → Paper displays it</li>
 *   <li>Dashboard calls {@code POST /api/v1/link/verify} with the code + web account ID</li>
 *   <li>Velocity validates code, creates the link, returns the player UUID</li>
 * </ol>
 *
 * @author Enzonic LLC — blockway.space
 */
public class LinkApiHandler {

    private final LinkManager linkManager;
    private final LinkRepository linkRepository;

    public LinkApiHandler(LinkManager linkManager, LinkRepository linkRepository) {
        this.linkManager = linkManager;
        this.linkRepository = linkRepository;
    }

    /**
     * POST /api/v1/link/verify
     * Body: { "code": "ABCD1234", "webAccountId": "user_abc123" }
     * Response: { "playerUuid": "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee" }
     */
    public void verifyCode(Context ctx) {
        Map<String, Object> body = ctx.bodyAsClass(Map.class);
        String code = (String) body.get("code");
        String webAccountId = (String) body.get("webAccountId");

        if (code == null || code.isBlank()) throw new BadRequestResponse("code is required");
        if (webAccountId == null || webAccountId.isBlank()) throw new BadRequestResponse("webAccountId is required");

        String playerUuid = linkManager.verifyAndConsume(code.trim().toUpperCase(), webAccountId.trim())
                .orElseThrow(() -> new BadRequestResponse("Invalid or expired link code"));

        ctx.status(200).json(Map.of("playerUuid", playerUuid, "webAccountId", webAccountId));
    }

    /**
     * GET /api/v1/link/{uuid}
     * Returns the linked web account ID for a given Minecraft UUID.
     */
    public void getLinkStatus(Context ctx) {
        String uuidStr = ctx.pathParam("uuid");
        UUID uuid;
        try { uuid = UUID.fromString(uuidStr); }
        catch (IllegalArgumentException e) { throw new BadRequestResponse("Invalid UUID format"); }

        String webAccountId = linkManager.getWebAccount(uuid)
                .orElseThrow(() -> new NotFoundResponse("No web account linked for UUID: " + uuidStr));

        ctx.json(Map.of("playerUuid", uuidStr, "webAccountId", webAccountId, "linked", true));
    }

    /**
     * DELETE /api/v1/link/{uuid}
     * Removes the web account link for a given Minecraft UUID.
     */
    public void unlink(Context ctx) {
        String uuidStr = ctx.pathParam("uuid");
        UUID uuid;
        try { uuid = UUID.fromString(uuidStr); }
        catch (IllegalArgumentException e) { throw new BadRequestResponse("Invalid UUID format"); }

        boolean removed = linkManager.unlink(uuid);
        if (!removed) throw new NotFoundResponse("No web account linked for UUID: " + uuidStr);
        ctx.json(Map.of("success", true, "playerUuid", uuidStr));
    }
}
