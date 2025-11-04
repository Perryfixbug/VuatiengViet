-- ===========================================
-- Table: User
-- ===========================================
-- CREATE TABLE user (
--     id INTEGER AUTO_INCREMENT PRIMARY KEY,
--     fullName VARCHAR(255) NOT NULL,
--     email VARCHAR(255) UNIQUE NOT NULL,
--     password VARCHAR(255) NOT NULL,
--     createAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
--     updateAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
--     totalScore INTEGER DEFAULT 0,
--     INDEX idx_email (email)
-- ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ===========================================
-- Table: ChallengePack
-- ===========================================
-- CREATE TABLE challengePack (
--     id INTEGER AUTO_INCREMENT PRIMARY KEY,
--     quizz VARCHAR(255) NOT NULL,
--     level INTEGER NOT NULL DEFAULT 1,
--     INDEX idx_level (level)
-- ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -- ===========================================
-- -- Table: Dictionary
-- -- ===========================================
-- CREATE TABLE dictionary (
--     word VARCHAR(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin PRIMARY KEY,
--     frequency INT DEFAULT 0,
--     INDEX idx_frequency (frequency)
-- ) ENGINE=InnoDB
--   DEFAULT CHARSET=utf8mb4
--   COLLATE=utf8mb4_bin;

-- ===========================================
-- Table: Answer
-- ===========================================
CREATE TABLE answer (
    challengePackId INTEGER NOT NULL,
    dictionaryWord VARCHAR(255)
        CHARACTER SET utf8mb4
        COLLATE utf8mb4_bin
        NOT NULL,
    PRIMARY KEY (challengePackId, dictionaryWord),
    FOREIGN KEY (challengePackId) REFERENCES challengePack(id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,
    FOREIGN KEY (dictionaryWord) REFERENCES dictionary(word)
        ON DELETE CASCADE
        ON UPDATE CASCADE,
    INDEX idx_challenge (challengePackId),
    INDEX idx_word (dictionaryWord)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_bin;

-- ===========================================
-- Table: Room
-- ===========================================
CREATE TABLE room (
    id INTEGER PRIMARY KEY,
    ownerId INTEGER,
    maxPlayer INTEGER NOT NULL DEFAULT 4,
    status VARCHAR(50) NOT NULL DEFAULT 'pending',
    createdAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    challengePackId INTEGER DEFAULT NULL,
    FOREIGN KEY (challengePackId) REFERENCES challengePack(id)
        ON DELETE SET NULL
        ON UPDATE CASCADE,
    FOREIGN KEY (ownerId) REFERENCES user(id)
        ON DELETE SET NULL
        ON UPDATE CASCADE,
    INDEX idx_status (status),
    INDEX idx_owner (ownerId)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ===========================================
-- Table: Player
-- ===========================================
-- CREATE TABLE player (
--     userId INTEGER NOT NULL,
--     roomId INTEGER NOT NULL,
--     score INTEGER DEFAULT 0,
--     PRIMARY KEY (userId, roomId),
--     FOREIGN KEY (userId) REFERENCES user(id)
--         ON DELETE CASCADE
--         ON UPDATE CASCADE,
--     FOREIGN KEY (roomId) REFERENCES room(id)
--         ON DELETE CASCADE
--         ON UPDATE CASCADE,
--     INDEX idx_room (roomId),
--     INDEX idx_user (userId)
-- ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;