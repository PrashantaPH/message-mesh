package com.message.mesh.service.relay;

import com.message.mesh.constant.AppConstants;
import com.message.mesh.dto.MessageDto;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class InMemoryMessageRelay implements MessageRelay {

    private final SimpMessagingTemplate template;

    @Override
    public void relay(UUID conversationId, MessageDto payload) {
        template.convertAndSend(AppConstants.topicConversation(conversationId), payload);
    }

    @Override
    public void relayToUser(String username, String destination, Object payload) {
        template.convertAndSendToUser(username, destination, payload);
    }

    @Override
    public void broadcast(String destination, Object payload) {
        template.convertAndSend(destination, payload);
    }
}
