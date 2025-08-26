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

    /**
     * Send message with specified key (partition assigned based on key hash)
     */
    @PostMapping("/send-with-key")
    public ResponseEntity<String> sendMessageWithKey(
            @RequestParam String key,
            @Valid @RequestBody ChatMessage chatMessage) {
        chatMessage.setMessageId(UUID.randomUUID().toString());
        chatMessageProducer.sendMessageWithKey(key, chatMessage);
        logger.info("Message sent with key '{}': {}", key, chatMessage);
        return ResponseEntity.ok("Message sent successfully with key: " + key);
    }

    /**
     * Send message to specified partition
     */
    @PostMapping("/send-to-partition")
    public ResponseEntity<String> sendMessageToPartition(
            @RequestParam int partition,
            @RequestParam(required = false) String key,
            @Valid @RequestBody ChatMessage chatMessage) {
        chatMessage.setMessageId(UUID.randomUUID().toString());
        String messageKey = key != null ? key : chatMessage.getSender() + "-" + chatMessage.getReceiver();
        chatMessageProducer.sendMessageToPartition(partition, messageKey, chatMessage);
        logger.info("Message sent to partition {}: {}", partition, chatMessage);
        return ResponseEntity.ok("Message sent successfully to partition: " + partition);
    }

    /**
     * Send message using business partition strategy (ensures messages from same conversation are in same partition)
     */
    @PostMapping("/send-business-partition")
    public ResponseEntity<String> sendMessageWithBusinessPartition(@Valid @RequestBody ChatMessage chatMessage) {
        chatMessage.setMessageId(UUID.randomUUID().toString());
        chatMessageProducer.sendMessageWithBusinessPartition(chatMessage);
        logger.info("Message sent with business partition strategy: {}", chatMessage);
        return ResponseEntity.ok("Message sent successfully with business partition strategy");
    }

    /**
     * Send batch messages (test performance and partition distribution)
     */
    @PostMapping("/send-batch")
    public ResponseEntity<String> sendBatchMessages(
            @RequestParam(defaultValue = "10") int count,
            @RequestParam(defaultValue = "false") boolean useBusinessPartition,
            @Valid @RequestBody ChatMessage template) {
        
        for (int i = 0; i < count; i++) {
            ChatMessage message = new ChatMessage();
            message.setMessageId(UUID.randomUUID().toString());
            message.setSender(template.getSender());
            message.setReceiver(template.getReceiver());
            message.setContent(template.getContent() + " - Message " + (i + 1));
            message.setTimestamp(java.time.LocalDateTime.now());
            
            if (useBusinessPartition) {
                chatMessageProducer.sendMessageWithBusinessPartition(message);
            } else {
                chatMessageProducer.sendMessage(message);
            }
        }
        
        logger.info("Sent {} messages using {} partition strategy", 
                   count, useBusinessPartition ? "business" : "default");
        return ResponseEntity.ok("Successfully sent " + count + " messages");
    }
}