-- Blockway Social — MySQL / MariaDB Schema
-- All tables use IF NOT EXISTS so this script is safe to re-run on every startup.

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS bws_players (
    uuid        VARCHAR(36)  NOT NULL PRIMARY KEY,
    username    VARCHAR(16)  NOT NULL,
    last_seen   BIGINT       NOT NULL DEFAULT 0,
    last_server VARCHAR(64),
    INDEX idx_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS bws_friendships (
    player_uuid VARCHAR(36) NOT NULL,
    friend_uuid VARCHAR(36) NOT NULL,
    created_at  BIGINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (player_uuid, friend_uuid),
    FOREIGN KEY (player_uuid) REFERENCES bws_players(uuid) ON DELETE CASCADE,
    FOREIGN KEY (friend_uuid) REFERENCES bws_players(uuid) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS bws_friend_requests (
    id            BIGINT      NOT NULL AUTO_INCREMENT PRIMARY KEY,
    sender_uuid   VARCHAR(36) NOT NULL,
    receiver_uuid VARCHAR(36) NOT NULL,
    sent_at       BIGINT      NOT NULL DEFAULT 0,
    UNIQUE KEY uq_request (sender_uuid, receiver_uuid),
    FOREIGN KEY (sender_uuid)   REFERENCES bws_players(uuid) ON DELETE CASCADE,
    FOREIGN KEY (receiver_uuid) REFERENCES bws_players(uuid) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS bws_parties (
    party_id    VARCHAR(36) NOT NULL PRIMARY KEY,
    leader_uuid VARCHAR(36) NOT NULL,
    created_at  BIGINT      NOT NULL DEFAULT 0,
    FOREIGN KEY (leader_uuid) REFERENCES bws_players(uuid) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS bws_party_members (
    party_id    VARCHAR(36) NOT NULL,
    player_uuid VARCHAR(36) NOT NULL,
    joined_at   BIGINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (party_id, player_uuid),
    FOREIGN KEY (party_id)    REFERENCES bws_parties(party_id) ON DELETE CASCADE,
    FOREIGN KEY (player_uuid) REFERENCES bws_players(uuid)     ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS bws_chat_messages (
    id           BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    channel_type VARCHAR(16)  NOT NULL,
    channel_id   VARCHAR(128) NOT NULL,
    sender_uuid  VARCHAR(36)  NOT NULL,
    sender_name  VARCHAR(16)  NOT NULL,
    message      TEXT         NOT NULL,
    sent_at      BIGINT       NOT NULL DEFAULT 0,
    INDEX idx_chat_channel (channel_type, channel_id, sent_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS bws_link_codes (
    code        VARCHAR(16) NOT NULL PRIMARY KEY,
    player_uuid VARCHAR(36) NOT NULL,
    created_at  BIGINT      NOT NULL DEFAULT 0,
    expires_at  BIGINT      NOT NULL DEFAULT 0,
    FOREIGN KEY (player_uuid) REFERENCES bws_players(uuid) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS bws_web_links (
    player_uuid    VARCHAR(36)  NOT NULL PRIMARY KEY,
    web_account_id VARCHAR(128) NOT NULL,
    linked_at      BIGINT       NOT NULL DEFAULT 0,
    FOREIGN KEY (player_uuid) REFERENCES bws_players(uuid) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS bws_api_keys (
    key_hash   VARCHAR(64) NOT NULL PRIMARY KEY,
    label      VARCHAR(64) NOT NULL UNIQUE,
    created_at BIGINT      NOT NULL DEFAULT 0,
    last_used  BIGINT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS bws_player_settings (
    player_uuid     VARCHAR(36) NOT NULL PRIMARY KEY,
    accept_requests TINYINT(1)  NOT NULL DEFAULT 1,
    notifications   TINYINT(1)  NOT NULL DEFAULT 1,
    private_server  TINYINT(1)  NOT NULL DEFAULT 0,
    FOREIGN KEY (player_uuid) REFERENCES bws_players(uuid) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
