package space.blockway.social.velocity.commands;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.slf4j.Logger;
import space.blockway.social.velocity.api.ApiKeyAuthFilter;
import space.blockway.social.velocity.config.VelocityConfig;
import space.blockway.social.velocity.database.ApiKeyRepository;
import space.blockway.social.velocity.database.DatabaseManager;
import space.blockway.social.shared.dto.ApiKeyDto;

import java.security.SecureRandom;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Proxy-level admin command: {@code /bwsocial}.
 *
 * <p>Sub-commands:
 * <ul>
 *   <li>{@code reload} — reload config from disk</li>
 *   <li>{@code apikey generate <label>} — create a new REST API key</li>
 *   <li>{@code apikey revoke <label>} — revoke a REST API key</li>
 *   <li>{@code apikey list} — list all active REST API keys</li>
 * </ul>
 *
 * <p>Permission: {@code blockwaysocial.admin}
 *
 * @author Enzonic LLC — blockway.space
 */
public class AdminCommand implements SimpleCommand {

    private static final String KEY_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final VelocityConfig config;
    private final ApiKeyRepository apiKeyRepository;
    private final Logger logger;

    public AdminCommand(VelocityConfig config, DatabaseManager databaseManager, Logger logger) {
        this.config = config;
        this.apiKeyRepository = new ApiKeyRepository(databaseManager, logger);
        this.logger = logger;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource src = invocation.source();
        String[] args = invocation.arguments();

        if (!src.hasPermission("blockwaysocial.admin")) {
            src.sendMessage(Component.text("You do not have permission to use this command.", NamedTextColor.RED));
            return;
        }

        if (args.length == 0) {
            sendHelp(src);
            return;
        }

        switch (args[0].toLowerCase()) {
            case "reload" -> {
                config.reload();
                src.sendMessage(Component.text("[BlockwaySocial] Configuration reloaded.", NamedTextColor.GREEN));
            }
            case "apikey" -> handleApiKey(src, args);
            default -> sendHelp(src);
        }
    }

    private void handleApiKey(CommandSource src, String[] args) {
        if (args.length < 2) {
            src.sendMessage(Component.text("Usage: /bwsocial apikey <generate|revoke|list> [label]", NamedTextColor.YELLOW));
            return;
        }
        switch (args[1].toLowerCase()) {
            case "generate" -> {
                if (args.length < 3) {
                    src.sendMessage(Component.text("Usage: /bwsocial apikey generate <label>", NamedTextColor.YELLOW));
                    return;
                }
                String label = args[2];
                if (apiKeyRepository.labelExists(label)) {
                    src.sendMessage(Component.text("A key with label '" + label + "' already exists.", NamedTextColor.RED));
                    return;
                }
                String plaintext = "bws_" + randomString(32);
                String hash = ApiKeyAuthFilter.sha256(plaintext);
                apiKeyRepository.createApiKey(hash, label);
                src.sendMessage(Component.text("[BlockwaySocial] Generated API key for label: " + label, NamedTextColor.GREEN));
                src.sendMessage(Component.text("Key: " + plaintext, NamedTextColor.YELLOW));
                src.sendMessage(Component.text("Store this key securely — it will not be shown again.", NamedTextColor.GRAY));
                logger.info("Admin generated API key for label: {}", label);
            }
            case "revoke" -> {
                if (args.length < 3) {
                    src.sendMessage(Component.text("Usage: /bwsocial apikey revoke <label>", NamedTextColor.YELLOW));
                    return;
                }
                String label = args[2];
                if (!apiKeyRepository.labelExists(label)) {
                    src.sendMessage(Component.text("No key found with label: " + label, NamedTextColor.RED));
                    return;
                }
                apiKeyRepository.revokeKey(label);
                src.sendMessage(Component.text("[BlockwaySocial] Revoked API key: " + label, NamedTextColor.GREEN));
            }
            case "list" -> {
                List<ApiKeyDto> keys = apiKeyRepository.listKeys();
                if (keys.isEmpty()) {
                    src.sendMessage(Component.text("No API keys configured.", NamedTextColor.GRAY));
                    return;
                }
                src.sendMessage(Component.text("Active API keys:", NamedTextColor.GOLD));
                for (ApiKeyDto key : keys) {
                    String lastUsed = key.getLastUsed() == null ? "never" : new java.util.Date(key.getLastUsed()).toString();
                    src.sendMessage(Component.text("  • " + key.getLabel() + " — last used: " + lastUsed, NamedTextColor.WHITE));
                }
            }
            default -> src.sendMessage(Component.text("Unknown sub-command. Use: generate, revoke, list", NamedTextColor.RED));
        }
    }

    private void sendHelp(CommandSource src) {
        src.sendMessage(Component.text("=== Blockway Social Admin ===", NamedTextColor.GOLD));
        src.sendMessage(Component.text("/bwsocial reload", NamedTextColor.YELLOW)
                .append(Component.text(" — Reload configuration", NamedTextColor.GRAY)));
        src.sendMessage(Component.text("/bwsocial apikey generate <label>", NamedTextColor.YELLOW)
                .append(Component.text(" — Generate a new API key", NamedTextColor.GRAY)));
        src.sendMessage(Component.text("/bwsocial apikey revoke <label>", NamedTextColor.YELLOW)
                .append(Component.text(" — Revoke an API key", NamedTextColor.GRAY)));
        src.sendMessage(Component.text("/bwsocial apikey list", NamedTextColor.YELLOW)
                .append(Component.text(" — List all API keys", NamedTextColor.GRAY)));
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source().hasPermission("blockwaysocial.admin");
    }

    @Override
    public CompletableFuture<List<String>> suggestAsync(Invocation invocation) {
        String[] args = invocation.arguments();
        if (args.length <= 1) return CompletableFuture.completedFuture(List.of("reload", "apikey"));
        if (args.length == 2 && args[0].equalsIgnoreCase("apikey"))
            return CompletableFuture.completedFuture(List.of("generate", "revoke", "list"));
        return CompletableFuture.completedFuture(List.of());
    }

    private String randomString(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) sb.append(KEY_CHARS.charAt(RANDOM.nextInt(KEY_CHARS.length())));
        return sb.toString();
    }
}
