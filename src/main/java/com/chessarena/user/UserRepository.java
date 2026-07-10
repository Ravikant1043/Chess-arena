package com.chessarena.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data repository for {@link User}. Declaring an interface and letting Spring
 * synthesise the implementation is Dependency Inversion in practice: services depend on
 * this abstraction, not on any concrete JDBC/JPA code.
 */
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    /** Leaderboard: highest score first, then most wins. */
    List<User> findTop20ByOrderByScoreDescWinsDesc();
}
