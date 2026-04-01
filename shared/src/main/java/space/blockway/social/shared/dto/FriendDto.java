package space.blockway.social.shared.dto;

import com.google.gson.annotations.SerializedName;

/**
 * Data transfer object representing a friend entry.
 *
 * @author Enzonic LLC — blockway.space
 */
public class FriendDto {

    @SerializedName("uuid")
    private String uuid;

    @SerializedName("username")
    private String username;

    /** The server name this friend is on, or null if offline. */
    @SerializedName("onlineServer")
    private String onlineServer;

    /** Whether this friend is currently online. */
    @SerializedName("online")
    private boolean online;

    /** Epoch milliseconds when the friendship was established. */
    @SerializedName("friendedAt")
    private long friendedAt;

    /** Epoch milliseconds of this friend's last known activity. */
    @SerializedName("lastSeen")
    private long lastSeen;

    public FriendDto() {}

    public FriendDto(String uuid, String username, String onlineServer, boolean online,
                     long friendedAt, long lastSeen) {
        this.uuid = uuid;
        this.username = username;
        this.onlineServer = onlineServer;
        this.online = online;
        this.friendedAt = friendedAt;
        this.lastSeen = lastSeen;
    }

    public String getUuid() { return uuid; }
    public void setUuid(String uuid) { this.uuid = uuid; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getOnlineServer() { return onlineServer; }
    public void setOnlineServer(String onlineServer) { this.onlineServer = onlineServer; }

    public boolean isOnline() { return online; }
    public void setOnline(boolean online) { this.online = online; }

    public long getFriendedAt() { return friendedAt; }
    public void setFriendedAt(long friendedAt) { this.friendedAt = friendedAt; }

    public long getLastSeen() { return lastSeen; }
    public void setLastSeen(long lastSeen) { this.lastSeen = lastSeen; }
}
