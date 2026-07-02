package com.message.mesh.repository;

import com.message.mesh.domain.User;
import com.message.mesh.enums.UserRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    Page<User> findByUsernameContainingIgnoreCaseOrDisplayNameContainingIgnoreCase(
            String username, String displayName, Pageable pageable);

    /**
     * Admin user search with optional filters. Any parameter left {@code null}
     * is ignored, so callers can mix a free-text query with role/active filters.
     */
    @Query("""
            select u from User u
            where (:q is null
                   or lower(u.username) like lower(concat('%', :q, '%'))
                   or lower(u.displayName) like lower(concat('%', :q, '%')))
              and (:role is null or u.role = :role)
              and (:active is null or u.active = :active)
            """)
    Page<User> search(@Param("q") String q,
                      @Param("role") UserRole role,
                      @Param("active") Boolean active,
                      Pageable pageable);

    long countByActive(boolean active);

    long countByRole(UserRole role);

    long countByCreatedAtAfter(Instant threshold);
}
