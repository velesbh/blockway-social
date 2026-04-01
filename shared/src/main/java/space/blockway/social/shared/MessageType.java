package space.blockway.social.shared;

/**
 * All plugin messaging channel message types used between Paper backends and the Velocity proxy.
 *
 * <p>Upstream = Paper → Velocity
 * <p>Downstream = Velocity → Paper
 *
 * @author Enzonic LLC — blockway.space
 */
public enum MessageType {

    // ── Upstream: Paper → Velocity ─────────────────────────────────────────────

    /** A player has connected to a backend server. */
    PLAYER_JOIN_SERVER,

    /** A player has disconnected from a backend server. */
    PLAYER_LEAVE_SERVER,

    /** Relay a friend request to the target's server (or store if offline). */
    FRIEND_REQUEST_RELAY,

    /** Accept an incoming friend request. */
    FRIEND_ACCEPT_RELAY,

    /** Deny an incoming friend request. */
    FRIEND_DENY_RELAY,

    /** Remove an existing friendship. */
    FRIEND_REMOVE_RELAY,

    /** Request to join a friend's server. */
    FRIEND_JOIN_RELAY,

    /** Relay a party invite to another player. */
    PARTY_INVITE_RELAY,

    /** Accept a party invite. */
    PARTY_ACCEPT_RELAY,

    /** Player is leaving their party. */
    PARTY_LEAVE_RELAY,

    /** Leader is kicking a member from the party. */
    PARTY_KICK_RELAY,

    /** Leader is disbanding the party. */
    PARTY_DISBAND_RELAY,

    /** Leader wants to warp the whole party to a server. */
    PARTY_WARP_RELAY,

    /** Create a new party (sender becomes leader). */
    PARTY_CREATE_RELAY,

    /** Send a message in friend cross-server chat. */
    FRIEND_CHAT_SEND,

    /** Send a message in party cross-server chat. */
    PARTY_CHAT_SEND,

    /** Player requests a web link code. */
    LINK_GENERATE,

    /** Player requests to unlink their web account. */
    LINK_REMOVE,

    // ── Downstream: Velocity → Paper ───────────────────────────────────────────

    /** You received a friend request. */
    FRIEND_REQUEST_RECEIVED,

    /** Your friend request was accepted. */
    FRIEND_REQUEST_ACCEPTED,

    /** Your friend request was denied. */
    FRIEND_REQUEST_DENIED,

    /** A friend came online. */
    FRIEND_ONLINE,

    /** A friend went offline. */
    FRIEND_OFFLINE,

    /** A message arrived in the friend chat channel. */
    FRIEND_CHAT_MESSAGE,

    /** A party invitation arrived. */
    PARTY_INVITE_RECEIVED,

    /** A message arrived in party chat. */
    PARTY_CHAT_MESSAGE,

    /** The party was disbanded by the leader. */
    PARTY_DISBANDED,

    /** You were kicked from the party. */
    PARTY_KICKED,

    /** A new member joined the party. */
    PARTY_MEMBER_JOINED,

    /** A member left or was kicked from the party. */
    PARTY_MEMBER_LEFT,

    /** Leadership was transferred to you. */
    PARTY_LEADER_TRANSFERRED,

    /** Proxy acknowledges party creation. */
    PARTY_CREATED,

    /** Proxy is telling this player to connect to a different server. */
    SEND_TO_SERVER,

    /** The link code was generated successfully. */
    LINK_CODE_GENERATED,

    /** The account was unlinked. */
    LINK_REMOVED,

    /** The account was not linked (cannot unlink). */
    LINK_NOT_FOUND,

    /** Generic success/failure acknowledgement. */
    RESULT
}
