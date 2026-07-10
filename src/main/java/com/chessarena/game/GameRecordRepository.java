package com.chessarena.game;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** Spring Data repository for finished {@link GameRecord}s. */
public interface GameRecordRepository extends JpaRepository<GameRecord, Long> {

    /** A player's recent games (as white or black), newest first. */
    List<GameRecord> findTop20ByWhiteUsernameOrBlackUsernameOrderByPlayedAtDesc(
            String whiteUsername, String blackUsername);
}
