package space.blockway.social.velocity.managers;

import org.slf4j.Logger;
import space.blockway.social.velocity.config.VelocityConfig;
import space.blockway.social.velocity.database.LinkRepository;

import java.security.SecureRandom;
import java.util.Optional;
import java.util.UUID;

/**
 * Generates and validates web account link codes.
 *
 * @author Enzonic LLC — blockway.space
 */
public class LinkManager {

    private static final String CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"; // omit confusing chars
    private static final int CODE_LENGTH = 8;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final LinkRepository linkRepo;
    private final VelocityConfig config;
    private final Logger logger;

    public LinkManager(LinkRepository linkRepo, VelocityConfig config, Logger logger) {
        this.linkRepo = linkRepo;
        this.config = config;
        this.logger = logger;
    }

    /** Generate a new link code for a player. Returns the plaintext code. */
    public String generateCode(UUID playerUuid) {
        String code = randomCode();
        long expiresAt = System.currentTimeMillis() + (config.getLinkCodeExpiryMinutes() * 60_000L);
        linkRepo.createLinkCode(code, playerUuid, expiresAt);
        return code;
    }

    /**
     * Verify and consume a link code, binding the Minecraft UUID to the given web account ID.
     *
     * @return the Minecraft UUID string if successful, empty if code is invalid/expired
     */
    public Optional<String> verifyAndConsume(String code, String webAccountId) {
        return linkRepo.findValidCode(code.toUpperCase()).map(record -> {
            linkRepo.deleteCode(record.code());
            linkRepo.createWebLink(record.playerUuid(), webAccountId);
            logger.info("Linked player {} to web account {}", record.playerUuid(), webAccountId);
            return record.playerUuid().toString();
        });
    }

    public boolean isLinked(UUID playerUuid) {
        return linkRepo.isLinked(playerUuid);
    }

    public Optional<String> getWebAccount(UUID playerUuid) {
        return linkRepo.getWebAccountId(playerUuid);
    }

    public boolean unlink(UUID playerUuid) {
        return linkRepo.deleteWebLink(playerUuid);
    }

    public void cleanExpiredCodes() {
        linkRepo.cleanExpiredCodes();
    }

    private String randomCode() {
        StringBuilder sb = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) sb.append(CODE_CHARS.charAt(RANDOM.nextInt(CODE_CHARS.length())));
        return sb.toString();
    }
}
