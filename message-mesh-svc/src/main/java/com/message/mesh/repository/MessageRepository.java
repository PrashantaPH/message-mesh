package com.message.mesh.repository;

import com.message.mesh.domain.ChatMessage;
import com.message.mesh.enums.MessageStatus;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MessageRepository extends JpaRepository<ChatMessage, UUID> {

    List<ChatMessage> findByConversationIdAndSeqGreaterThanOrderBySeqAsc(
            UUID conversationId, long afterSeq, Pageable pageable);

    Optional<ChatMessage> findFirstByConversationIdOrderBySeqDesc(UUID conversationId);

    org.springframework.data.domain.Page<ChatMessage>
            findByConversationIdOrderBySeqDesc(UUID conversationId, Pageable pageable);

    long countByCreatedAtAfter(java.time.Instant threshold);

    org.springframework.data.domain.Page<ChatMessage>
            findByConversationIdAndDeletedFalseAndBodyContainingIgnoreCaseOrderBySeqDesc(
                    UUID conversationId, String body, Pageable pageable);

    long countByConversationId(UUID conversationId);

    @Query("select coalesce(max(m.seq), 0) from ChatMessage m where m.conversationId = :conversationId")
    long findMaxSeq(@Param("conversationId") UUID conversationId);

    @Modifying
    @Transactional
    @Query("update ChatMessage m set m.status = :status where m.id = :messageId")
    int updateStatus(@Param("messageId") UUID messageId, @Param("status") MessageStatus status);
}
