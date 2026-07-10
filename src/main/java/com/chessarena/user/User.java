package com.chessarena.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;

/**
 * A registered player and their accumulated results.
 *
 * <p>Encapsulation is applied even to a JPA entity: the score/win/loss/draw counters
 * are never set directly by callers. Instead the domain methods {@link #recordWin()},
 * {@link #recordLoss()}, and {@link #recordDraw()} mutate them together with the score,
 * so the invariant "score always matches the tallied results" cannot be broken.
 */
@Entity
@Table(name = "users")
public class User {

    /** Points awarded per result — the scoring rules live in one place. */
    private static final int WIN_POINTS = 3;
    private static final int DRAW_POINTS = 1;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String username;

    @Column(nullable = false)
    private String passwordHash;

    @Column(nullable = false)
    private int score;

    @Column(nullable = false)
    private int wins;

    @Column(nullable = false)
    private int losses;

    @Column(nullable = false)
    private int draws;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    /** Optimistic lock so two concurrent game-result updates cannot clobber each other. */
    @Version
    private Long version;

    protected User() {
        // Required by JPA.
    }

    public User(String username, String passwordHash) {
        this.username = username;
        this.passwordHash = passwordHash;
    }

    // ------------------------------------------------- domain behaviour

    public void recordWin() {
        this.wins++;
        this.score += WIN_POINTS;
    }

    public void recordLoss() {
        this.losses++;
        // A loss adds no points, but is still tallied.
    }

    public void recordDraw() {
        this.draws++;
        this.score += DRAW_POINTS;
    }

    public int gamesPlayed() {
        return wins + losses + draws;
    }

    // ------------------------------------------------- accessors

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public int getScore() {
        return score;
    }

    public int getWins() {
        return wins;
    }

    public int getLosses() {
        return losses;
    }

    public int getDraws() {
        return draws;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
