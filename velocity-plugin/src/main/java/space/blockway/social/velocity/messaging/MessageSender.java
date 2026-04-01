package space.blockway.social.velocity.messaging;

import com.google.gson.Gson;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import org.slf4j.Logger;
import space.blockway.social.shared.ChannelMessage;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;

/**
 * Sends {@link ChannelMessage} objects downstream to Paper backend servers.
 *
 * <p>Plugin messages require an online player as the conduit. If the target player
 * is not connected, the message is silently dropped (ephemeral delivery).
 *
 * @author Enzonic LLC — blockway.space
 */
public class MessageSender {

    public static final MinecraftChannelIdentifier CHANNEL =
            MinecraftChannelIdentifier.create("blockwaysocial", "events");

    private final ProxyServer proxy;
    private final Logger logger;
    private final Gson gson = new Gson();

    public MessageSender(ProxyServer proxy, Logger logger) {
        this.proxy = proxy;
        this.logger = logger;
    }

    /** Send a message to a specific player's current backend server. */
    public void sendToPlayer(UUID playerUuid, ChannelMessage message) {
        Optional<Player> playerOpt = proxy.getPlayer(playerUuid);
        if (playerOpt.isEmpty()) return;
        Player player = playerOpt.get();
        if (player.getCurrentServer().isEmpty()) return;

        byte[] data = gson.toJson(message).getBytes(StandardCharsets.UTF_8);
        player.getCurrentServer().get().sendPluginMessage(CHANNEL, data);
    }

    /** Send a message targeting a specific player UUID (sets targetUuid automatically). */
    public void sendToPlayer(UUID playerUuid, space.blockway.social.shared.MessageType type, String payload) {
        sendToPlayer(playerUuid, new ChannelMessage(type, payload, playerUuid.toString()));
    }
}
