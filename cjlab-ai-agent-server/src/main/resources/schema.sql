CREATE TABLE IF NOT EXISTS cjlab_user_account (
    id VARCHAR(64) NOT NULL,
    email VARCHAR(255) NOT NULL,
    display_name VARCHAR(100) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    creator VARCHAR(64) NULL,
    updater VARCHAR(64) NULL,
    deleted TINYINT(1) NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_cjlab_user_account_email (email),
    KEY idx_cjlab_user_account_status (status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS cjlab_user_session (
    token VARCHAR(128) NOT NULL,
    user_id VARCHAR(64) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    creator VARCHAR(64) NULL,
    updater VARCHAR(64) NULL,
    deleted TINYINT(1) NOT NULL DEFAULT 0,
    PRIMARY KEY (token),
    KEY idx_cjlab_user_session_user_id (user_id),
    KEY idx_cjlab_user_session_expires_at (expires_at),
    CONSTRAINT fk_cjlab_user_session_user
        FOREIGN KEY (user_id)
        REFERENCES cjlab_user_account (id)
        ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS cjlab_user_role_card (
    id VARCHAR(160) NOT NULL,
    user_id VARCHAR(64) NOT NULL,
    role_id VARCHAR(64) NOT NULL,
    name VARCHAR(80) NOT NULL,
    description VARCHAR(240) NULL,
    instruction TEXT NULL,
    avatar LONGTEXT NULL,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    creator VARCHAR(64) NULL,
    updater VARCHAR(64) NULL,
    deleted TINYINT(1) NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_cjlab_user_role_card_user_role (user_id, role_id),
    KEY idx_cjlab_user_role_card_user_update_time (user_id, update_time),
    CONSTRAINT fk_cjlab_user_role_card_user
        FOREIGN KEY (user_id)
        REFERENCES cjlab_user_account (id)
        ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

ALTER TABLE cjlab_user_role_card
    ADD COLUMN avatar LONGTEXT NULL AFTER instruction;

CREATE TABLE IF NOT EXISTS cjlab_conversation_message (
    id VARCHAR(64) NOT NULL,
    conversation_id VARCHAR(255) NOT NULL,
    role VARCHAR(32) NOT NULL,
    content LONGTEXT NOT NULL,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    creator VARCHAR(64) NULL,
    updater VARCHAR(64) NULL,
    deleted TINYINT(1) NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_cjlab_conversation_message_conversation_created (conversation_id, create_time)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS cjlab_conversation_summary (
    conversation_id VARCHAR(255) NOT NULL,
    content LONGTEXT NOT NULL,
    message_count INT NOT NULL DEFAULT 0,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    creator VARCHAR(64) NULL,
    updater VARCHAR(64) NULL,
    deleted TINYINT(1) NOT NULL DEFAULT 0,
    PRIMARY KEY (conversation_id),
    KEY idx_cjlab_conversation_summary_update_time (update_time)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS cjlab_knowledge_document (
    id VARCHAR(64) NOT NULL,
    title VARCHAR(255) NOT NULL,
    content LONGTEXT NOT NULL,
    metadata_json JSON NULL,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    creator VARCHAR(64) NULL,
    updater VARCHAR(64) NULL,
    deleted TINYINT(1) NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_cjlab_knowledge_document_updated_at (update_time)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS cjlab_tool_call_record (
    id VARCHAR(64) NOT NULL,
    conversation_id VARCHAR(255) NOT NULL,
    tool_name VARCHAR(128) NOT NULL,
    input LONGTEXT NULL,
    arguments_json JSON NULL,
    output LONGTEXT NULL,
    success TINYINT(1) NOT NULL DEFAULT 1,
    error_message TEXT NULL,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    creator VARCHAR(64) NULL,
    updater VARCHAR(64) NULL,
    deleted TINYINT(1) NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_cjlab_tool_call_record_conversation_created (conversation_id, create_time),
    KEY idx_cjlab_tool_call_record_tool_name (tool_name)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;
