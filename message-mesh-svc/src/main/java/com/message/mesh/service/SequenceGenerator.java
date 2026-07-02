package com.message.mesh.service;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.message.mesh.repository.MessageRepository;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Generates monotonic, per-conversation sequence numbers. Counters are seeded
 * lazily from the database high-water mark so they survive restarts.
 *
 * <p>Counters are held in a Caffeine {@link LoadingCache} that bounds memory
 * ({@code maximumSize}) and evicts idle conversations ({@code expireAfterAccess}).
 * Eviction is safe: once a conversation goes quiet its messages are already
 * persisted, so a subsequent reload re-seeds from the same DB high-water mark.
 */
@Component
public class SequenceGenerator {

    /** Bounds heap usage: at most this many live per-conversation counters. */
    private static final long MAX_COUNTERS = 100_000;
    /** Idle conversations are evicted after this period of inactivity. */
    private static final Duration IDLE_TTL = Duration.ofHours(1);

    private final LoadingCache<UUID, AtomicLong> counters;

    public SequenceGenerator(MessageRepository messageRepository) {
        this.counters = Caffeine.newBuilder()
                .maximumSize(MAX_COUNTERS)
                .expireAfterAccess(IDLE_TTL)
                .build(id -> new AtomicLong(messageRepository.findMaxSeq(id)));
    }

    public long next(UUID conversationId) {
        return counters.get(conversationId).incrementAndGet();
    }
}
