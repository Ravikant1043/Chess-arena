package com.chessarena.presence;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Default {@link PresenceStore}: keeps online users in a thread-safe in-memory set.
 *
 * <p>Active whenever the {@code redis} profile is <b>not</b> enabled, so the application
 * runs out of the box with no Docker/Redis required. Suitable for a single instance
 * (which is exactly the local/dev setup).
 */
@Component
@Profile("!redis")
public class InMemoryPresenceStore implements PresenceStore {

    private final Set<String> online = ConcurrentHashMap.newKeySet();

    @Override
    public void markOnline(String username) {
        online.add(username);
    }

    @Override
    public void markOffline(String username) {
        online.remove(username);
    }

    @Override
    public boolean isOnline(String username) {
        return online.contains(username);
    }

    @Override
    public Set<String> onlineUsers() {
        return Collections.unmodifiableSet(Set.copyOf(online));
    }
}
