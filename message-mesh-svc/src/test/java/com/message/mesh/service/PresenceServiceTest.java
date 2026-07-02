package com.message.mesh.service;

import com.message.mesh.constant.AppConstants;
import com.message.mesh.dto.PresenceDto;
import com.message.mesh.service.relay.MessageRelay;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
@DisplayName("PresenceService")
class PresenceServiceTest {

    @Mock
    private MessageRelay relay;

    private PresenceService presenceService;

    @BeforeEach
    void setUp() {
        presenceService = new PresenceService(relay);
    }

    @Test
    @DisplayName("markOnline registers the user and broadcasts an online presence event")
    void markOnlineBroadcastsPresence() {
        presenceService.markOnline("alice", "session-1");

        assertThat(presenceService.isOnline("alice")).isTrue();
        assertThat(presenceService.onlineUsernames()).containsExactly("alice");
        verify(relay).broadcast(AppConstants.TOPIC_PRESENCE, new PresenceDto("alice", true));
    }

    @Test
    @DisplayName("markOffline only broadcasts offline once the last session closes")
    void markOfflineWhenLastSessionCloses() {
        presenceService.markOnline("alice", "session-1");
        presenceService.markOnline("alice", "session-2");

        presenceService.markOffline("alice", "session-1");
        assertThat(presenceService.isOnline("alice")).isTrue();

        presenceService.markOffline("alice", "session-2");
        assertThat(presenceService.isOnline("alice")).isFalse();

        verify(relay).broadcast(AppConstants.TOPIC_PRESENCE, new PresenceDto("alice", false));
    }

    @Test
    @DisplayName("markOffline for an unknown user is a no-op")
    void markOfflineUnknownUserDoesNothing() {
        presenceService.markOffline("ghost", "session-x");

        assertThat(presenceService.isOnline("ghost")).isFalse();
        verifyNoInteractions(relay);
    }

    @Test
    @DisplayName("onlineUsernames returns an immutable snapshot")
    void onlineUsernamesIsImmutableSnapshot() {
        presenceService.markOnline("alice", "s1");
        var snapshot = presenceService.onlineUsernames();

        assertThat(snapshot).containsExactly("alice");
        // snapshot must not reflect subsequent changes
        presenceService.markOnline("bob", "s2");
        assertThat(snapshot).containsExactly("alice");
    }
}
