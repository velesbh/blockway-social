package space.blockway.social.shared.dto;

import com.google.gson.annotations.SerializedName;

/**
 * Data transfer object for a persisted chat message (friend or party channel).
 *
 * @author Enzonic LLC — blockway.space
 */
public class ChatMessageDto {

    @SerializedName("id")
    private long id;

    /** Either {@code "FRIEND"} or {@code "PARTY"}. */
    @SerializedName("channelType")
    private String channelType;

    /**
     * The channel identifier.
     * For FRIEND channels: {@code min(uuidA, uuidB) + "-" + max(uuidA, uuidB)}.
     * For PARTY channels: the party UUID.
     */
    @SerializedName("channelId")
    private String channelId;

    @SerializedName("senderUuid")
    private String senderUuid;

    @SerializedName("senderUsername")
    private String senderUsername;

    @SerializedName("message")
    private String message;

    /** Epoch milliseconds when the message was sent. */
    @SerializedName("sentAt")
    private long sentAt;

    public ChatMessageDto() {}

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getChannelType() { return channelType; }
    public void setChannelType(String channelType) { this.channelType = channelType; }

    public String getChannelId() { return channelId; }
    public void setChannelId(String channelId) { this.channelId = channelId; }

    public String getSenderUuid() { return senderUuid; }
    public void setSenderUuid(String senderUuid) { this.senderUuid = senderUuid; }

    public String getSenderUsername() { return senderUsername; }
    public void setSenderUsername(String senderUsername) { this.senderUsername = senderUsername; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public long getSentAt() { return sentAt; }
    public void setSentAt(long sentAt) { this.sentAt = sentAt; }
}
