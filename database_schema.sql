-- ===========================================
-- Database Schema for vtvdb Game
-- Generated from DAO analysis
-- All IDs use int to match Java Integer type
-- ===========================================

DROP DATABASE IF EXISTS vtvdb;
CREATE DATABASE vtvdb CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE vtvdb;

-- ===========================================
-- Table: User
-- Stores user account information
-- ===========================================
CREATE TABLE user (
    id int AUTO_INCREMENT PRIMARY KEY,
    fullName VARCHAR(255) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    createAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updateAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    totalScore int DEFAULT 0,
    INDEX idx_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ===========================================
-- Table: Room
-- Stores game room information
-- ===========================================
CREATE TABLE room (
    id int PRIMARY KEY,
    ownerId int,
    maxPlayer int NOT NULL DEFAULT 4,
    status VARCHAR(50) NOT NULL DEFAULT 'pending',
    createdAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    challengePackId int DEFAULT NULL,
    FOREIGN KEY (ownerId) REFERENCES user(id)
        ON DELETE SET NULL
        ON UPDATE CASCADE,
    INDEX idx_status (status),
    INDEX idx_owner (ownerId)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ===========================================
-- Table: Player
-- Junction table between User and Room
-- Tracks player participation and scores
-- ===========================================
CREATE TABLE player (
    userId int NOT NULL,
    roomId int NOT NULL,
    score int DEFAULT 0,
    PRIMARY KEY (userId, roomId),
    FOREIGN KEY (userId) REFERENCES user(id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,
    FOREIGN KEY (roomId) REFERENCES room(id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,
    INDEX idx_room (roomId),
    INDEX idx_user (userId)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ===========================================
-- Table: ChallengePack
-- Stores quiz challenges for game rooms
-- ===========================================
CREATE TABLE challengePack (
    id int AUTO_INCREMENT PRIMARY KEY,
    quizz VARCHAR(255) NOT NULL,
    level int NOT NULL DEFAULT 1,
    INDEX idx_level (level)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ===========================================
-- Table: Dictionary
-- Stores valid words and their frequency
-- ===========================================
CREATE TABLE dictionary (
    word VARCHAR(255) PRIMARY KEY,
    frequency int DEFAULT 0,
    INDEX idx_frequency (frequency)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ===========================================
-- Table: Answer
-- Junction table between ChallengePack and Dictionary
-- Maps valid answers to each challenge
-- ===========================================
CREATE TABLE answer (
    challengePackId int NOT NULL,
    dictionaryWord VARCHAR(255) NOT NULL,
    PRIMARY KEY (challengePackId, dictionaryWord),
    FOREIGN KEY (challengePackId) REFERENCES challengePack(id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,
    FOREIGN KEY (dictionaryWord) REFERENCES dictionary(word)
        ON DELETE CASCADE
        ON UPDATE CASCADE,
    INDEX idx_challenge (challengePackId),
    INDEX idx_word (dictionaryWord)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ===========================================
-- Add foreign key for challengePackId in Room table
-- (After ChallengePack table is created)
-- ===========================================
ALTER TABLE room
    ADD CONSTRAINT fk_room_challengepack
    FOREIGN KEY (challengePackId) REFERENCES challengePack(id)
        ON DELETE SET NULL
        ON UPDATE CASCADE;

-- ===========================================


-- ===========================================
-- Verification Queries
-- ===========================================

-- Check table structure
-- SHOW CREATE TABLE user;
-- SHOW CREATE TABLE room;
-- SHOW CREATE TABLE player;
-- SHOW CREATE TABLE challengePack;
-- SHOW CREATE TABLE dictionary;
-- SHOW CREATE TABLE answer;

-- Check data
-- SELECT * FROM user;
-- SELECT * FROM dictionary LIMIT 10;
-- SELECT * FROM challengePack;
-- SELECT * FROM answer;
