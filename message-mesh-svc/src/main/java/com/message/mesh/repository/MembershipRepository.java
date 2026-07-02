package com.message.mesh.repository;

import com.message.mesh.domain.Membership;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MembershipRepository extends JpaRepository<Membership, UUID> {

    List<Membership> findByConversationId(UUID conversationId);

    List<Membership> findByUserId(UUID userId);

    Optional<Membership> findByUserIdAndConversationId(UUID userId, UUID conversationId);

    boolean existsByUserIdAndConversationId(UUID userId, UUID conversationId);

    long countByConversationId(UUID conversationId);

    long countByConversationIdAndRole(UUID conversationId, com.message.mesh.enums.MembershipRole role);

    long countByUserId(UUID userId);

    void deleteByUserId(UUID userId);

    void deleteByConversationId(UUID conversationId);
}
