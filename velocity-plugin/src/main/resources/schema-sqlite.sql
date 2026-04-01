-- Blockway Social — SQLite Schema
-- All tables use IF NOT EXISTS so this script is safe to re-run on every startup.

PRAGMA journal_mode=WAL;
PRAGMA foreign_keys=ON;

CREATE TABLE IF NOT EXISTS bws_players (
    uuid        TEXT NOT NULL PRIMARY KEY,
    username    TEXT NOT NULL,
    last_seen   INTEGER NOT NULL DEFAULT 0,
    last_server TEXT
);

CREATE TABLE IF NOT EXISTS bws_friendships (
    player_uuid TEXT    NOT NULL,
    friend_uuid TEXT    NOT NULL,
    created_at  INTEGER NOT NULL DEFAULT 0,
    PRIMARY KEY (player_uuid, friend_uuid),
    FOREIGN KEY (player_uuid) REFERENCES bws_players(uuid) ON DELETE CASCADE,
    FOREIGN KEY (friend_uuid) REFERENCES bws_players(uuid) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS bws_friend_requests (
    id            INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
    sender_uuid   TEXT    NOT NULL,
    receiver_uuid TEXT    NOT NULL,
    sent_at       INTEGER NOT NULL DEFAULT 0,
    UNIQUE (sender_uuid, receiver_uuid),
    FOREIGN KEY (sender_uuid)   REFERENCES bws_players(uuid) ON DELETE CASCADE,
    FOREIGN KEY (receiver_uuid) REFERENCES bws_players(uuid) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS bws_parties (
    party_id    TEXT    NOT NULL PRIMARY KEY,
    leader_uuid TEXT    NOT NULL,
    created_at  INTEGER NOT NULL DEFAULT 0,
    FOREIGN KEY (leader_uuid) REFERENCES bws_players(uuid) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS bws_party_members (
    party_id    TEXT    NOT NULL,
    player_uuid TEXT    NOT NULL,
    joined_at   INTEGER NOT NULL DEFAULT 0,
    PRIMARY KEY (party_id, player_uuid),
    FOREIGN KEY (party_id)    REFERENCES bws_parties(party_id) ON DELETE CASCADE,
    FOREIGN KEY (player_uuid) REFERENCES bws_players(uuid)    ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS bws_chat_messages (
    id           INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
    channel_type TEXT    NOT NULL,
    channel_id   TEXT    NOT NULL,
    sender_uuid  TEXT    NOT NULL,
    sender_name  TEXT    NOT NULL,
    message      TEXT    NOT NULL,
    sent_at      INTEGER NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_chat_channel ON bws_chat_messages (channel_type, channel_id, sent_at);

CREATE TABLE IF NOT EXISTS bws_link_codes (
    code        TEXT    NOT NULL PRIMARY KEY,
    player_uuid TEXT    NOT NULL,
    created_at  INTEGER NOT NULL DEFAULT 0,
    expires_at  INTEGER NOT NULL DEFAULT 0,
    FOREIGN KEY (player_uuid) REFERENCES bws_players(uuid) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS bws_web_links (
    player_uuid   TEXT NOT NULL PRIMARY KEY,
    web_account_id TEXT NOT NULL,
    linked_at     INTEGER NOT NULL DEFAULT 0,
    FOREIGN KEY (player_uuid) REFERENCES bws_players(uuid) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS bws_api_keys (
    key_hash   TEXT    NOT NULL PRIMARY KEY,
    label      TEXT    NOT NULL UNIQUE,
    created_at INTEGER NOT NULL DEFAULT 0,
    last_used  INTEGER
);

CREATE TABLE IF NOT EXISTS bws_player_settings (
    player_uuid          TEXT    NOT NULL PRIMARY KEY,
    accept_requests      INTEGER NOT NULL DEFAULT 1,
    notifications        INTEGER NOT NULL DEFAULT 1,
    private_server       INTEGER NOT NULL DEFAULT 0,
    FOREIGN KEY (player_uuid) REFERENCES bws_players(uuid) ON DELETE CASCADE
);
