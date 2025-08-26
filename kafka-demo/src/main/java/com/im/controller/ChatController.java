package com.im.controller;

import com.im.dto.ChatMessage;
import com.im.service.ChatMessageProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.UUID;

@RestController
@RequestMapping("/api/chat")
@Validated
public class ChatController {

    private static final Logger logger = LoggerFactory.getLogger(ChatController.class);

    private final ChatMessageProducer chatMessageProducer;

    @Autowired
    public ChatController(ChatMessageProducer chatMessageProducer) {
        this.chatMessageProducer = chatMessageProducer;
    }

    @PostMapping("/send")
    public ResponseEntity<String> sendMessage(@Valid @RequestBody ChatMessage chatMessage) {
        chatMessage.setMessageId(UUID.randomUUID().toString());
        chatMessageProducer.sendMessage(chatMessage);
        logger.info("Message sent: {}", chatMessage);
        return ResponseEntity.ok("Message sent successfully");
    }

    // Other existing methods...
}