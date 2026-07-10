package com.chessarena.game;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * A finished game, persisted for history and auditing. Stores both players, who won,
 * how the game ended, and the move list (UCI, space-separated) so any game can be
 * replayed later.
 */
@Entity
@Table(name = "game_records")
public class GameRecord {

    public enum Outcome {
        WHITE_WINS,
        BLACK_WINS,
        DRAW
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String whiteUsername;

    @Column(nullable = false)
    private String blackUsername;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Outcome outcome;

    /** How the game ended: CHECKMATE, STALEMATE, DRAW, RESIGNATION, DISCONNECT. */
    @Column(nullable = false)
    private String endReason;

    @Column(length = 4000)
    private String movesUci;

    @Column(nullable = false, updatable = false)
    private Instant playedAt = Instant.now();

    protected GameRecord() {
        // Required by JPA.
    }

    public GameRecord(String whiteUsername, String blackUsername, Outcome outcome,
                      String endReason, String movesUci) {
        this.whiteUsername = whiteUsername;
        this.blackUsername = blackUsername;
        this.outcome = outcome;
        this.endReason = endReason;
        this.movesUci = movesUci;
    }

    public Long getId() {
        return id;
    }

    public String getWhiteUsername() {
        return whiteUsername;
    }

    public String getBlackUsername() {
        return blackUsername;
    }

    public Outcome getOutcome() {
        return outcome;
    }

    public String getEndReason() {
        return endReason;
    }

    public String getMovesUci() {
        return movesUci;
    }

    public Instant getPlayedAt() {
        return playedAt;
    }
}
