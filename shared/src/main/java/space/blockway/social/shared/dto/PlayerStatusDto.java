package space.blockway.social.shared.dto;

import com.google.gson.annotations.SerializedName;

/**
 * Data transfer object for a player's current network status.
 *
 * @author Enzonic LLC — blockway.space
 */
public class PlayerStatusDto {

    @SerializedName("uuid")
    private String uuid;

    @SerializedName("username")
    private String username;

    @SerializedName("online")
    private boolean online;

    /** Current server name if online, null otherwise. */
    @SerializedName("currentServer")
    private String currentServer;

    /** Epoch ms of last login/logout event. */
    @SerializedName("lastSeen")
    private long lastSeen;

    /** Whether this player has a linked web account. */
    @SerializedName("linked")
    private boolean linked;

    public PlayerStatusDto() {}

    public String getUuid() { return uuid; }
    public void setUuid(String uuid) { this.uuid = uuid; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public boolean isOnline() { return online; }
    public void setOnline(boolean online) { this.online = online; }

    public String getCurrentServer() { return currentServer; }
    public void setCurrentServer(String currentServer) { this.currentServer = currentServer; }

    public long getLastSeen() { return lastSeen; }
    public void setLastSeen(long lastSeen) { this.lastSeen = lastSeen; }

    public boolean isLinked() { return linked; }
    public void setLinked(boolean linked) { this.linked = linked; }
}
