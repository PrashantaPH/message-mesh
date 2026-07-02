package com.message.mesh.controller;

import com.message.mesh.dto.AckRequest;
import com.message.mesh.dto.SendMessageRequest;
import com.message.mesh.dto.TypingEvent;
import com.message.mesh.service.MessageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

import java.security.Principal;

/**
 * STOMP message endpoints (client -> server).
 */
@Controller
@RequiredArgsConstructor
public class ChatController {

    private final MessageService messageService;

    @MessageMapping("/chat.send")
    public void send(@Valid @Payload SendMessageRequest req, Principal principal) {
        messageService.handleSend(principal.getName(), req);
    }

    @MessageMapping("/chat.ack")
    public void ack(@Valid @Payload AckRequest req, Principal principal) {
        messageService.handleAck(principal.getName(), req);
    }

    @MessageMapping("/chat.typing")
    public void typing(@Valid @Payload TypingEvent ev, Principal principal) {
        messageService.handleTyping(principal.getName(), ev);
    }
}
