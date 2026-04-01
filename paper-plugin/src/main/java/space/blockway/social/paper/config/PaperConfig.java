package space.blockway.social.paper.config;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.*;

/**
 * Typed wrapper around the Paper plugin's {@link FileConfiguration}.
 * Message strings are parsed by MiniMessage on first access and cached.
 *
 * @author Enzonic LLC — blockway.space
 */
public class PaperConfig {

    private final FileConfiguration cfg;
    private final MiniMessage mm = MiniMessage.miniMessage();
    private final Map<String, Component> componentCache = new HashMap<>();

    public PaperConfig(FileConfiguration cfg) {
        this.cfg = cfg;
    }

    // ── Messages ──────────────────────────────────────────────────────────────

    public Component getPrefix() {
        return parse("messages.prefix");
    }

    /**
     * Get a message component with optional placeholder replacements.
     *
     * @param key          config path under messages
     * @param replacements alternating key/value pairs: "player", "Notch", "server", "survival-1"
     */
    public Component getMessage(String key, String... replacements) {
        String raw = cfg.getString("messages." + key, "<red>Missing message: " + key);
        if (replacements.length == 0) {
            return componentCache.computeIfAbsent(key, k -> mm.deserialize(raw));
        }
        // Build tag resolvers from key-value pairs
        TagResolver.Builder builder = TagResolver.builder();
        for (int i = 0; i + 1 < replacements.length; i += 2) {
            builder.resolver(Placeholder.component(replacements[i], Component.text(replacements[i + 1])));
        }
        return mm.deserialize(raw, builder.build());
    }

    public Component getPrefixedMessage(String key, String... replacements) {
        return getPrefix().append(getMessage(key, replacements));
    }

    // ── GUI ───────────────────────────────────────────────────────────────────

    public String getFriendsGuiTitle() {
        return cfg.getString("gui.friends-title", "<gradient:#00c3ff:#0080ff>Friends List</gradient>");
    }

    public String getPartyGuiTitle() {
        return cfg.getString("gui.party-title", "<#9B59B6>Party Management");
    }

    // ── General ───────────────────────────────────────────────────────────────

    public String getServerName() {
        return cfg.getString("server-name", "server");
    }

    private Component parse(String path) {
        return componentCache.computeIfAbsent(path, k -> mm.deserialize(cfg.getString(k, "")));
    }
}
