package space.blockway.social.velocity.api;

import io.javalin.http.Context;
import io.javalin.http.Handler;
import io.javalin.http.UnauthorizedResponse;
import space.blockway.social.velocity.database.ApiKeyRepository;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Javalin before-filter that enforces Bearer token authentication on all /api/v1 routes.
 *
 * <p>Tokens are validated against SHA-256 hashes stored in {@code bws_api_keys}.
 * The master key (for admin endpoints) is checked inline inside {@link AdminApiHandler}.
 *
 * @author Enzonic LLC — blockway.space
 */
public class ApiKeyAuthFilter implements Handler {

    private final ApiKeyRepository apiKeyRepository;
    private final String masterKey;

    public ApiKeyAuthFilter(ApiKeyRepository apiKeyRepository, String masterKey) {
        this.apiKeyRepository = apiKeyRepository;
        this.masterKey = masterKey;
    }

    @Override
    public void handle(Context ctx) throws Exception {
        String authHeader = ctx.header("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new UnauthorizedResponse("Missing or invalid Authorization header. Use: Authorization: Bearer <api_key>");
        }
        String providedKey = authHeader.substring(7).trim();

        // Master key bypasses DB lookup (admin endpoints validate it separately)
        if (providedKey.equals(masterKey)) {
            ctx.attribute("isMasterKey", true);
            ctx.attribute("apiKey", providedKey);
            return;
        }

        // Validate regular API key
        String keyHash = sha256(providedKey);
        if (!apiKeyRepository.validateKey(keyHash)) {
            throw new UnauthorizedResponse("Invalid API key");
        }
        ctx.attribute("isMasterKey", false);
        ctx.attribute("apiKey", providedKey);
    }

    public static String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) hex.append(String.format("%02x", b));
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}
