package com.message.mesh.service;

import com.message.mesh.domain.ChatMessage;
import com.message.mesh.domain.Conversation;
import com.message.mesh.domain.Membership;
import com.message.mesh.domain.User;
import com.message.mesh.dto.AddMembersRequest;
import com.message.mesh.dto.ConversationDto;
import com.message.mesh.dto.ConversationEvent;
import com.message.mesh.dto.ConversationMemberDto;
import com.message.mesh.dto.CreateConversationRequest;
import com.message.mesh.dto.MembershipPrefsRequest;
import com.message.mesh.dto.MessageDto;
import com.message.mesh.dto.PagedResponse;
import com.message.mesh.enums.ConversationType;
import com.message.mesh.enums.MembershipRole;
import com.message.mesh.exception.BadRequestException;
import com.message.mesh.exception.ResourceNotFoundException;
import com.message.mesh.constant.AppConstants;
import com.message.mesh.repository.ConversationRepository;
import com.message.mesh.repository.MembershipRepository;
import com.message.mesh.repository.MessageRepository;
import com.message.mesh.repository.UserRepository;
import com.message.mesh.service.relay.MessageRelay;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConversationService {

    private final ConversationRepository conversationRepository;
    private final MembershipRepository membershipRepository;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final MessageRelay relay;
    private final AuditService auditService;
    private final MessageDtoAssembler messageDtoAssembler;

    @Transactional
    public ConversationDto create(String creatorUsername, CreateConversationRequest req) {
        User creator = requireUser(creatorUsername);

        Set<String> usernames = new LinkedHashSet<>(req.memberUsernames());
        usernames.add(creatorUsername);

        if (req.type() == ConversationType.DIRECT && usernames.size() != 2) {
            throw new BadRequestException("A DIRECT conversation must have exactly 2 distinct members");
        }

        if (req.type() == ConversationType.DIRECT) {
            String otherUsername = usernames.stream()
                    .filter(u -> !u.equals(creatorUsername))
                    .findFirst()
                    .orElseThrow(() -> new BadRequestException(
                            "A DIRECT conversation must include another member"));
            User other = requireUser(otherUsername);
            var existing = conversationRepository.findExistingDirect(creator.getId(), other.getId());
            if (existing.isPresent()) {
                log.info("Reusing existing DIRECT conversation {} between {} and {}",
                        existing.get().getId(), creatorUsername, otherUsername);
                return toDto(existing.get(), creator.getId());
            }
        }

        Conversation conversation = Conversation.builder()
                .type(req.type())
                .title(req.title())
                .createdBy(creatorUsername)
                .build();
        conversationRepository.save(conversation);

        List<User> members = new ArrayList<>(usernames.size());
        for (String username : usernames) {
            User user = requireUser(username);
            members.add(user);
            MembershipRole role = (req.type() == ConversationType.GROUP && username.equals(creatorUsername))
                    ? MembershipRole.ADMIN
                    : MembershipRole.MEMBER;
            membershipRepository.save(Membership.builder()
                    .userId(user.getId())
                    .conversationId(conversation.getId())
                    .role(role)
                    .build());
        }

        log.info("Created {} conversation {} with {} members",
                req.type(), conversation.getId(), usernames.size());

        // Notify every member (including newly added ones) so their clients pick up
        // the conversation and subscribe to its topic without a page refresh.
        for (User member : members) {
            relay.relayToUser(member.getUsername(), AppConstants.QUEUE_CONVERSATIONS,
                    toDto(conversation, member.getId()));
        }

        return toDto(conversation, creator.getId());
    }

    @Transactional(readOnly = true)
    public List<ConversationDto> listForUser(String username) {
        User user = requireUser(username);
        List<Conversation> conversations = conversationRepository.findAllForUser(user.getId());
        List<ConversationDto> result = new ArrayList<>(conversations.size());
        for (Conversation conversation : conversations) {
            result.add(toDto(conversation, user.getId()));
        }
        return result;
    }

    @Transactional(readOnly = true)
    public List<MessageDto> getMessages(String username, UUID conversationId, long afterSeq, int limit) {
        User user = requireUser(username);
        assertMembership(user.getId(), conversationId);
        int pageSize = Math.min(Math.max(limit, 1), 200);
        List<ChatMessage> messages = messageRepository
                .findByConversationIdAndSeqGreaterThanOrderBySeqAsc(
                        conversationId, afterSeq, PageRequest.of(0, pageSize));
        return messageDtoAssembler.toDtos(messages);
    }

    /**
     * Case-insensitive full-text search of a conversation's (non-deleted) messages,
     * newest first. Membership is enforced before any rows are read.
     */
    @Transactional(readOnly = true)
    public PagedResponse<MessageDto> searchMessages(String username, UUID conversationId,
                                                    String query, int page, int size) {
        User user = requireUser(username);
        assertMembership(user.getId(), conversationId);
        if (query == null || query.isBlank()) {
            return new PagedResponse<>(List.of(), 0, size, 0, 0);
        }
        int safeSize = Math.min(Math.max(size, 1), 100);
        Page<ChatMessage> result = messageRepository
                .findByConversationIdAndDeletedFalseAndBodyContainingIgnoreCaseOrderBySeqDesc(
                        conversationId, query.trim(), PageRequest.of(Math.max(page, 0), safeSize));
        List<MessageDto> content = messageDtoAssembler.toDtos(result.getContent());
        return new PagedResponse<>(content, result.getNumber(), result.getSize(),
                result.getTotalElements(), result.getTotalPages());
    }

    /**
     * Returns every participant of a conversation together with their membership
     * role, so clients can render a group roster (member count + admins). Admins
     * are listed first, then members, each group ordered by display name.
     */
    @Transactional(readOnly = true)
    public List<ConversationMemberDto> getMembers(String username, UUID conversationId) {
        User user = requireUser(username);
        assertMembership(user.getId(), conversationId);
        List<ConversationMemberDto> members = new ArrayList<>();
        for (Membership membership : membershipRepository.findByConversationId(conversationId)) {
            userRepository.findById(membership.getUserId())
                    .ifPresent(member -> members.add(ConversationMemberDto.of(member, membership.getRole())));
        }
        members.sort(Comparator
                .comparingInt((ConversationMemberDto m) -> m.role() == MembershipRole.ADMIN ? 0 : 1)
                .thenComparing(ConversationMemberDto::displayName, String.CASE_INSENSITIVE_ORDER));
        return members;
    }

    @Transactional
    public void markRead(String username, UUID conversationId, long seq) {
        User user = requireUser(username);
        Membership membership = membershipRepository
                .findByUserIdAndConversationId(user.getId(), conversationId)
                .orElseThrow(() -> new BadRequestException(
                        "User is not a member of conversation " + conversationId));
        if (seq > membership.getLastReadSeq()) {
            membership.setLastReadSeq(seq);
            membershipRepository.save(membership);
        }
    }

    // ------------------------------------------------------------------
    // Group management
    // ------------------------------------------------------------------

    /** Rename a group conversation (group admins only). */
    @Transactional
    public ConversationDto renameGroup(String username, UUID conversationId, String title) {
        User user = requireUser(username);
        Conversation conversation = requireGroup(conversationId);
        requireGroupAdmin(user.getId(), conversationId);
        conversation.setTitle(title);
        conversationRepository.save(conversation);
        auditService.record(username, "CONVERSATION_RENAMED", "conversation",
                conversationId.toString(), "title=" + title);

        ConversationEvent event = new ConversationEvent(
                ConversationEvent.Type.RENAMED, conversationId, username, null, title);
        relay.broadcast(AppConstants.topicConversationMeta(conversationId), event);
        return toDto(conversation, user.getId());
    }

    /** Add users to a group conversation (group admins only). */
    @Transactional
    public List<ConversationMemberDto> addMembers(String username, UUID conversationId, List<String> usernames) {
        User actor = requireUser(username);
        Conversation conversation = requireGroup(conversationId);
        requireGroupAdmin(actor.getId(), conversationId);

        for (String candidate : new LinkedHashSet<>(usernames)) {
            User member = requireUser(candidate);
            if (membershipRepository.existsByUserIdAndConversationId(member.getId(), conversationId)) {
                continue;
            }
            membershipRepository.save(Membership.builder()
                    .userId(member.getId())
                    .conversationId(conversationId)
                    .role(MembershipRole.MEMBER)
                    .build());
            // Let the new member's client pick up the conversation live.
            relay.relayToUser(member.getUsername(), AppConstants.QUEUE_CONVERSATIONS,
                    toDto(conversation, member.getId()));
            relay.broadcast(AppConstants.topicConversationMeta(conversationId),
                    new ConversationEvent(ConversationEvent.Type.MEMBER_ADDED,
                            conversationId, username, member.getUsername(), null));
            auditService.record(username, "MEMBER_ADDED", "conversation",
                    conversationId.toString(), "user=" + candidate);
        }
        return getMembers(username, conversationId);
    }

    /** Remove a member from a group. Admins can remove anyone; members can remove only themselves (leave). */
    @Transactional
    public void removeMember(String username, UUID conversationId, UUID targetUserId) {
        User actor = requireUser(username);
        Conversation conversation = requireGroup(conversationId);
        Membership actorMembership = requireMembership(actor.getId(), conversationId);

        boolean removingSelf = actor.getId().equals(targetUserId);
        if (!removingSelf && actorMembership.getRole() != MembershipRole.ADMIN) {
            throw new BadRequestException("Only group admins can remove other members");
        }

        Membership target = requireMembership(targetUserId, conversationId);
        // Prevent removing the last admin (which would orphan the group).
        if (target.getRole() == MembershipRole.ADMIN
                && membershipRepository.countByConversationIdAndRole(conversationId, MembershipRole.ADMIN) <= 1) {
            throw new BadRequestException("Cannot remove the last admin of the group");
        }

        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + targetUserId));
        membershipRepository.delete(target);
        relay.broadcast(AppConstants.topicConversationMeta(conversationId),
                new ConversationEvent(ConversationEvent.Type.MEMBER_REMOVED,
                        conversationId, username, targetUser.getUsername(), null));
        auditService.record(username, removingSelf ? "MEMBER_LEFT" : "MEMBER_REMOVED",
                "conversation", conversationId.toString(), "user=" + targetUser.getUsername());
    }

    /** Promote/demote a group member (group admins only). */
    @Transactional
    public List<ConversationMemberDto> updateMemberRole(String username, UUID conversationId,
                                                        UUID targetUserId, MembershipRole role) {
        User actor = requireUser(username);
        requireGroup(conversationId);
        requireGroupAdmin(actor.getId(), conversationId);

        Membership target = requireMembership(targetUserId, conversationId);
        // Prevent demoting the last admin.
        if (target.getRole() == MembershipRole.ADMIN && role != MembershipRole.ADMIN
                && membershipRepository.countByConversationIdAndRole(conversationId, MembershipRole.ADMIN) <= 1) {
            throw new BadRequestException("Cannot demote the last admin of the group");
        }
        target.setRole(role);
        membershipRepository.save(target);

        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + targetUserId));
        relay.broadcast(AppConstants.topicConversationMeta(conversationId),
                new ConversationEvent(ConversationEvent.Type.MEMBER_ROLE_CHANGED,
                        conversationId, username, targetUser.getUsername(), role.name()));
        auditService.record(username, "MEMBER_ROLE_CHANGED", "conversation",
                conversationId.toString(), "user=" + targetUser.getUsername() + " role=" + role);
        return getMembers(username, conversationId);
    }

    /** Update the caller's per-conversation mute/archive preferences. */
    @Transactional
    public ConversationDto setMembershipPrefs(String username, UUID conversationId, MembershipPrefsRequest req) {
        User user = requireUser(username);
        Membership membership = requireMembership(user.getId(), conversationId);
        if (req.muted() != null) {
            membership.setMuted(req.muted());
        }
        if (req.archived() != null) {
            membership.setArchived(req.archived());
        }
        membershipRepository.save(membership);
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found: " + conversationId));
        return toDto(conversation, user.getId());
    }

    /**
     * Leave (for a direct conversation) or soft-delete (group admin) a conversation.
     * A group admin soft-deletes the whole conversation; other cases fall back to
     * removing only the caller's own membership.
     */
    @Transactional
    public void deleteConversation(String username, UUID conversationId) {
        User user = requireUser(username);
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found: " + conversationId));
        Membership membership = requireMembership(user.getId(), conversationId);

        boolean isGroupAdmin = conversation.getType() == ConversationType.GROUP
                && membership.getRole() == MembershipRole.ADMIN;
        if (isGroupAdmin) {
            conversation.setDeleted(true);
            conversationRepository.save(conversation);
            relay.broadcast(AppConstants.topicConversationMeta(conversationId),
                    new ConversationEvent(ConversationEvent.Type.CONVERSATION_DELETED,
                            conversationId, username, null, null));
            auditService.record(username, "CONVERSATION_DELETED", "conversation",
                    conversationId.toString(), null);
        } else {
            membershipRepository.delete(membership);
            relay.broadcast(AppConstants.topicConversationMeta(conversationId),
                    new ConversationEvent(ConversationEvent.Type.MEMBER_REMOVED,
                            conversationId, username, username, null));
            auditService.record(username, "MEMBER_LEFT", "conversation",
                    conversationId.toString(), null);
        }
    }

    private Conversation requireGroup(UUID conversationId) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found: " + conversationId));
        if (conversation.getType() != ConversationType.GROUP) {
            throw new BadRequestException("Operation is only valid for group conversations");
        }
        return conversation;
    }

    private Membership requireMembership(UUID userId, UUID conversationId) {
        return membershipRepository.findByUserIdAndConversationId(userId, conversationId)
                .orElseThrow(() -> new BadRequestException(
                        "User is not a member of conversation " + conversationId));
    }

    private void requireGroupAdmin(UUID userId, UUID conversationId) {
        Membership membership = requireMembership(userId, conversationId);
        if (membership.getRole() != MembershipRole.ADMIN) {
            throw new BadRequestException("Only group admins can perform this action");
        }
    }

    private ConversationDto toDto(Conversation conversation, UUID userId) {
        Membership membership = membershipRepository
                .findByUserIdAndConversationId(userId, conversation.getId())
                .orElse(null);
        long lastReadSeq = membership != null ? membership.getLastReadSeq() : 0L;
        boolean muted = membership != null && membership.isMuted();
        boolean archived = membership != null && membership.isArchived();
        long maxSeq = messageRepository.findMaxSeq(conversation.getId());
        long unread = Math.max(0, maxSeq - lastReadSeq);
        MessageDto last = messageRepository
                .findFirstByConversationIdOrderBySeqDesc(conversation.getId())
                .map(MessageDto::from)
                .orElse(null);
        int memberCount = (int) membershipRepository.countByConversationId(conversation.getId());
        return new ConversationDto(
                conversation.getId(),
                conversation.getType(),
                resolveTitle(conversation, userId),
                last,
                unread,
                memberCount,
                muted,
                archived);
    }

    /**
     * For DIRECT conversations the title is viewer-relative: it must show the OTHER
     * participant's display name. For GROUP conversations the stored title is used.
     */
    private String resolveTitle(Conversation conversation, UUID viewerId) {
        if (conversation.getType() != ConversationType.DIRECT) {
            return conversation.getTitle();
        }
        return membershipRepository.findByConversationId(conversation.getId()).stream()
                .map(Membership::getUserId)
                .filter(id -> !id.equals(viewerId))
                .findFirst()
                .flatMap(userRepository::findById)
                .map(User::getDisplayName)
                .orElse(conversation.getTitle());
    }

    private void assertMembership(UUID userId, UUID conversationId) {
        if (!membershipRepository.existsByUserIdAndConversationId(userId, conversationId)) {
            throw new BadRequestException("User is not a member of conversation " + conversationId);
        }
    }

    private User requireUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
    }
}
