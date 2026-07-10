package com.chessarena.presence;

import java.util.Set;

/**
 * The shared, volatile state the lobby needs: who is online right now.
 *
 * <p>This interface is the seam that lets the platform run with <b>no external
 * services</b> during development (an in-memory implementation) while a Redis-backed
 * implementation exists for multi-instance deployments. The rest of the code depends
 * only on this abstraction — textbook Dependency Inversion, and it means switching to
 * Redis is a one-line profile change with zero impact on callers.
 *
 * @see InMemoryPresenceStore the default (active with no configuration)
 * @see RedisPresenceStore    activated by the {@code redis} Spring profile
 */
public interface PresenceStore {

    /** Marks a user online. */
    void markOnline(String username);

    /** Marks a user offline (on disconnect or logout). */
    void markOffline(String username);

    /** @return true if the user is currently online. */
    boolean isOnline(String username);

    /** @return an immutable snapshot of everyone currently online. */
    Set<String> onlineUsers();
}
