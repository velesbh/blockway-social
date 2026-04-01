package space.blockway.social.velocity.api.handlers;

import io.javalin.http.BadRequestResponse;
import io.javalin.http.Context;
import io.javalin.http.ForbiddenResponse;
import space.blockway.social.shared.dto.ApiKeyDto;
import space.blockway.social.velocity.api.ApiKeyAuthFilter;
import space.blockway.social.velocity.database.ApiKeyRepository;

import java.security.SecureRandom;
import java.util.List;
import java.util.Map;

/**
 * REST handlers for admin-only API key management.
 * All endpoints in this handler require the master key.
 *
 * @author Enzonic LLC — blockway.space
 */
public class AdminApiHandler {

    private static final String KEY_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final int KEY_RANDOM_LENGTH = 32;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final ApiKeyRepository apiKeyRepository;
    private final String masterKey;

    public AdminApiHandler(ApiKeyRepository apiKeyRepository, String masterKey) {
        this.apiKeyRepository = apiKeyRepository;
        this.masterKey = masterKey;
    }

    /**
     * GET /api/v1/apikey/generate?label=my-dashboard
     * Requires master key. Generates a new API key, stores its hash, returns plaintext once.
     */
    public void generateApiKey(Context ctx) {
        requireMasterKey(ctx);

        String label = ctx.queryParam("label");
        if (label == null || label.isBlank()) throw new BadRequestResponse("label query parameter is required");
        if (apiKeyRepository.labelExists(label)) throw new BadRequestResponse("A key with label '" + label + "' already exists");

        String plaintext = "bws_" + randomString(KEY_RANDOM_LENGTH);
        String hash = ApiKeyAuthFilter.sha256(plaintext);
        apiKeyRepository.createApiKey(hash, label);

        ctx.status(201).json(Map.of(
                "label", label,
                "key", plaintext,
                "note", "Store this key securely — it will not be shown again."
        ));
    }

    /**
     * DELETE /api/v1/apikey/{label}
     * Requires master key. Revokes an API key by label.
     */
    public void revokeApiKey(Context ctx) {
        requireMasterKey(ctx);
        String label = ctx.pathParam("label");
        if (!apiKeyRepository.labelExists(label)) throw new BadRequestResponse("No key found with label: " + label);
        apiKeyRepository.revokeKey(label);
        ctx.json(Map.of("success", true, "label", label));
    }

    /**
     * GET /api/v1/apikey/list
     * Requires master key. Lists all active API key labels (no hashes or plaintext).
     */
    public void listApiKeys(Context ctx) {
        requireMasterKey(ctx);
        List<ApiKeyDto> keys = apiKeyRepository.listKeys();
        ctx.json(Map.of("keys", keys, "count", keys.size()));
    }

    private void requireMasterKey(Context ctx) {
        Boolean isMaster = ctx.attribute("isMasterKey");
        if (!Boolean.TRUE.equals(isMaster)) {
            throw new ForbiddenResponse("This endpoint requires the master API key");
        }
    }

    private String randomString(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) sb.append(KEY_CHARS.charAt(RANDOM.nextInt(KEY_CHARS.length())));
        return sb.toString();
    }
}
