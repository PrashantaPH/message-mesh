package com.message.mesh.repository;

import com.message.mesh.domain.MessageReaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MessageReactionRepository extends JpaRepository<MessageReaction, UUID> {

    List<MessageReaction> findByMessageId(UUID messageId);

    List<MessageReaction> findByMessageIdIn(Collection<UUID> messageIds);

    Optional<MessageReaction> findByMessageIdAndUsernameAndEmoji(UUID messageId, String username, String emoji);

    void deleteByMessageId(UUID messageId);

    void deleteByMessageIdAndUsernameAndEmoji(UUID messageId, String username, String emoji);
}
