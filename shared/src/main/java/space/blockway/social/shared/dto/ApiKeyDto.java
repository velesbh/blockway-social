package space.blockway.social.shared.dto;

import com.google.gson.annotations.SerializedName;

/**
 * Data transfer object for an API key entry.
 * The plaintext {@link #key} is only populated when a key is first created;
 * it is never stored or returned again after that.
 *
 * @author Enzonic LLC — blockway.space
 */
public class ApiKeyDto {

    @SerializedName("label")
    private String label;

    /**
     * Plaintext key — only present in the create response.
     * Format: {@code bws_<32 random alphanumeric chars>}
     */
    @SerializedName("key")
    private String key;

    @SerializedName("createdAt")
    private long createdAt;

    @SerializedName("lastUsed")
    private Long lastUsed;

    public ApiKeyDto() {}

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }

    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public Long getLastUsed() { return lastUsed; }
    public void setLastUsed(Long lastUsed) { this.lastUsed = lastUsed; }
}
