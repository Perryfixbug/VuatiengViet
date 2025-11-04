USE vuatiengviet;

CREATE TABLE User (
    id INTEGER NOT NULL PRIMARY KEY auto_increment,
    fullName VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    password VARCHAR(255) NOT NULL,
    createAt TIMESTAMP NOT NULL,
    updateAt TIMESTAMP NOT NULL,
    totalScore INTEGER DEFAULT 0
);


CREATE TABLE Player (
    id INTEGER NOT NULL PRIMARY KEY,
    userId INTEGER NOT NULL,
    roomId INTEGER NOT NULL,
    score INT DEFAULT 0
);
CREATE TABLE Challengepack(
	id INTEGER PRIMARY KEY,
    quizz VARCHAR(255),
    level INTEGER
);
CREATE TABLE Room (
    id INTEGER NOT NULL PRIMARY KEY,
    ownerId INTEGER NOT NULL,
    maxPlayer INTEGER NOT NULL,
    createdAt TIMESTAMP NOT NULL,
    status VARCHAR(50) NOT NULL,
    challengePackId INTEGER DEFAULT NULL,
    FOREIGN KEY (challengePackId) REFERENCES ChallengePack(id)
);



CREATE TABLE dictionary(
	word VARCHAR(255) PRIMARY KEY,
    frequency INTEGER
);


CREATE TABLE answer(
	challengePackId INTEGER,
    dictionaryWord VARCHAR(255),
    FOREIGN KEY (challengePackId) REFERENCES challengepack (id) ON DELETE CASCADE,
	FOREIGN KEY (dictionaryWord) REFERENCES dictionary (word),
    PRIMARY KEY (challengePackId, dictionaryWord)
);