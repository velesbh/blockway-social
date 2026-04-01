package space.blockway.social.shared.dto;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/**
 * Data transfer object representing a party.
 *
 * @author Enzonic LLC — blockway.space
 */
public class PartyDto {

    @SerializedName("partyId")
    private String partyId;

    @SerializedName("leaderUuid")
    private String leaderUuid;

    @SerializedName("leaderUsername")
    private String leaderUsername;

    /** UUIDs of all party members including the leader. */
    @SerializedName("memberUuids")
    private List<String> memberUuids;

    /** Enriched member objects (may be null in lightweight contexts). */
    @SerializedName("members")
    private List<FriendDto> members;

    @SerializedName("memberCount")
    private int memberCount;

    @SerializedName("createdAt")
    private long createdAt;

    public PartyDto() {}

    public String getPartyId() { return partyId; }
    public void setPartyId(String partyId) { this.partyId = partyId; }

    public String getLeaderUuid() { return leaderUuid; }
    public void setLeaderUuid(String leaderUuid) { this.leaderUuid = leaderUuid; }

    public String getLeaderUsername() { return leaderUsername; }
    public void setLeaderUsername(String leaderUsername) { this.leaderUsername = leaderUsername; }

    public List<String> getMemberUuids() { return memberUuids; }
    public void setMemberUuids(List<String> memberUuids) { this.memberUuids = memberUuids; }

    public List<FriendDto> getMembers() { return members; }
    public void setMembers(List<FriendDto> members) { this.members = members; }

    public int getMemberCount() { return memberCount; }
    public void setMemberCount(int memberCount) { this.memberCount = memberCount; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}
