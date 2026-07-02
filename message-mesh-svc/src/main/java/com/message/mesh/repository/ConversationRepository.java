package com.message.mesh.repository;

import com.message.mesh.domain.Conversation;
import com.message.mesh.enums.ConversationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ConversationRepository extends JpaRepository<Conversation, UUID> {

    Page<Conversation> findByTitleContainingIgnoreCase(String title, Pageable pageable);

    /**
     * Admin conversation search with optional filters. Any parameter left
     * {@code null} is ignored.
     */
    @Query("""
            select c from Conversation c
            where (:q is null or lower(c.title) like lower(concat('%', :q, '%')))
              and (:type is null or c.type = :type)
              and (:deleted is null or c.deleted = :deleted)
            """)
    Page<Conversation> search(@Param("q") String q,
                              @Param("type") ConversationType type,
                              @Param("deleted") Boolean deleted,
                              Pageable pageable);

    long countByType(ConversationType type);

    long countByDeleted(boolean deleted);

    @Query("""
            select c from Conversation c
            where c.deleted = false
              and c.id in (
                select m.conversationId from Membership m
                where m.userId = :userId and m.archived = false
            )
            order by c.createdAt desc
            """)
    List<Conversation> findAllForUser(@Param("userId") UUID userId);

    @Query("""
            select c from Conversation c
            where c.type = com.message.mesh.enums.ConversationType.DIRECT
              and exists (select 1 from Membership m1 where m1.conversationId = c.id and m1.userId = :userA)
              and exists (select 1 from Membership m2 where m2.conversationId = c.id and m2.userId = :userB)
            order by c.createdAt asc
            """)
    List<Conversation> findDirectBetween(@Param("userA") UUID userA, @Param("userB") UUID userB);

    default Optional<Conversation> findExistingDirect(UUID userA, UUID userB) {
        return findDirectBetween(userA, userB).stream().findFirst();
    }
}
