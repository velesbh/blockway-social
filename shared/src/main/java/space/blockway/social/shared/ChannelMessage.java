package space.blockway.social.shared;

import com.google.gson.annotations.SerializedName;

/**
 * Envelope for all plugin messaging channel communications on {@code blockwaysocial:events}.
 *
 * <p>All messages are UTF-8 JSON-serialized instances of this class. The {@code payload} field
 * holds a JSON string of the inner payload specific to each {@link MessageType}. Keeping it as a
 * string avoids tight coupling — each handler only deserializes the payloads it cares about.
 *
 * <p>Example wire format:
 * <pre>{@code
 * {
 *   "type": "FRIEND_REQUEST_RELAY",
 *   "payload": "{\"senderUuid\":\"...\",\"senderUsername\":\"Notch\",\"receiverUuid\":\"...\"}",
 *   "targetUuid": "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee",
 *   "timestamp": 1748000000000
 * }
 * }</pre>
 *
 * @author Enzonic LLC — blockway.space
 */
public class ChannelMessage {

    @SerializedName("type")
    private MessageType type;

    /**
     * Inner JSON payload string. Parse this according to the {@link #type}.
     */
    @SerializedName("payload")
    private String payload;

    /**
     * UUID string of the player this message targets (may be null for broadcast messages).
     */
    @SerializedName("targetUuid")
    private String targetUuid;

    /** Epoch milliseconds when this message was created. */
    @SerializedName("timestamp")
    private long timestamp;

    public ChannelMessage() {}

    public ChannelMessage(MessageType type, String payload, String targetUuid) {
        this.type = type;
        this.payload = payload;
        this.targetUuid = targetUuid;
        this.timestamp = System.currentTimeMillis();
    }

    public ChannelMessage(MessageType type, String payload) {
        this(type, payload, null);
    }

    public MessageType getType() { return type; }
    public void setType(MessageType type) { this.type = type; }

    public String getPayload() { return payload; }
    public void setPayload(String payload) { this.payload = payload; }

    public String getTargetUuid() { return targetUuid; }
    public void setTargetUuid(String targetUuid) { this.targetUuid = targetUuid; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    @Override
    public String toString() {
        return "ChannelMessage{type=" + type + ", targetUuid=" + targetUuid + "}";
    }
}
