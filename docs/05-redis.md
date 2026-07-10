# Redis Integration

> Package `com.chessarena.presence`.

## Design: an interface with two implementations

Online presence is accessed only through the `PresenceStore` interface:

```java
public interface PresenceStore {
    void markOnline(String username);
    void markOffline(String username);
    boolean isOnline(String username);
    Set<String> onlineUsers();
}
```

Two implementations exist, selected by Spring profile:

| Bean | Profile | Backing store | When |
|---|---|---|---|
| `InMemoryPresenceStore` | `!redis` (default) | thread-safe in-memory `Set` | local / single instance — **no Docker needed** |
| `RedisPresenceStore` | `redis` | Redis SET `chess:online-players` | multi-instance deployments |

This is **Dependency Inversion in action**: every caller (`LobbyBroadcaster`,
`WebSocketEventListener`) depends on the `PresenceStore` abstraction, so switching to
Redis is a one-flag change with zero caller edits.

## Why it's off by default (current scope)

Running Redis normally means a Docker container. For the current scope Docker is out, so
the app defaults to the in-memory store and starts with no external services. The Redis
code is complete and compiles; it simply isn't the active bean unless you ask for it.

## Enabling Redis later

1. Start Redis (e.g. `docker run -p 6379:6379 redis:7-alpine`, or `brew install redis && redis-server`).
2. Run with the profile:
   ```bash
   ./mvnw spring-boot:run -Dspring-boot.run.profiles=redis
   ```
   (or set `SPRING_PROFILES_ACTIVE=redis`).

`RedisPresenceStore` then becomes the active `PresenceStore`, and presence is shared
across every app instance pointing at the same Redis.

## What would move to Redis at scale

Beyond presence, in a multi-instance deployment these would also move from in-memory to
Redis (the interfaces are already the natural seams):

- **Open challenges** (`MatchmakingService`) → a Redis hash with TTL, so a challenge
  expires automatically and any instance can resolve an accept.
- **Active game sessions** (`GameService`) → shared session state + a Redis pub/sub or
  the STOMP broker relay so move broadcasts reach players on any instance.
