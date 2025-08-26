package com.im.controller;

import com.im.dto.ChatMessage;
import com.im.service.ChatMessageProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/chat")
public class ChatMessageRetryController {

    private static final Logger logger = LoggerFactory.getLogger(ChatMessageRetryController.class);

    @Autowired
    private ChatMessageProducer chatMessageProducer;

    /**
     * Send message asynchronously with retry
     */
    @PostMapping("/send")
    public ResponseEntity<String> sendMessage(@RequestBody ChatMessage message) {
        try {
            message.setTimestamp(LocalDateTime.now());
            chatMessageProducer.sendMessage(message);
            logger.info("Message queued for sending: {}", message);
            return ResponseEntity.ok("Message sent successfully (async)");
        } catch (Exception e) {
            logger.error("Failed to queue message: {}", e.getMessage());
            return ResponseEntity.internalServerError().body("Failed to send message");
        }
    }

    /**
     * Send message synchronously with retry - guarantees delivery or failure
     */
    @PostMapping("/send-sync")
    public ResponseEntity<String> sendMessageSync(@RequestBody ChatMessage message) {
        try {
            message.setTimestamp(LocalDateTime.now());
            boolean success = chatMessageProducer.sendMessageSync(message);
            
            if (success) {
                logger.info("Message sent successfully (sync): {}", message);
                return ResponseEntity.ok("Message sent successfully (sync)");
            } else {
                logger.error("Failed to send message after retries: {}", message);
                return ResponseEntity.internalServerError().body("Failed to send message after retries");
            }
        } catch (Exception e) {
            logger.error("Failed to send message: {}", e.getMessage());
            return ResponseEntity.internalServerError().body("Failed to send message");
        }
    }

    /**
     * Send message with custom key and retry
     */
    @PostMapping("/send-with-key")
    public ResponseEntity<String> sendMessageWithKey(
            @RequestParam String key,
            @RequestBody ChatMessage message) {
        try {
            message.setTimestamp(LocalDateTime.now());
            chatMessageProducer.sendMessageWithKey(key, message);
            logger.info("Message with key '{}' queued for sending: {}", key, message);
            return ResponseEntity.ok("Message with key sent successfully (async)");
        } catch (Exception e) {
            logger.error("Failed to queue message with key '{}': {}", key, e.getMessage());
            return ResponseEntity.internalServerError().body("Failed to send message with key");
        }
    }

    /**
     * Send message to specific partition with retry
     */
    @PostMapping("/send-to-partition")
    public ResponseEntity<String> sendMessageToPartition(
            @RequestParam int partition,
            @RequestParam(required = false) String key,
            @RequestBody ChatMessage message) {
        try {
            message.setTimestamp(LocalDateTime.now());
            chatMessageProducer.sendMessageToPartition(partition, key, message);
            logger.info("Message queued for partition {}: {}", partition, message);
            return ResponseEntity.ok("Message sent to partition " + partition + " successfully (async)");
        } catch (Exception e) {
            logger.error("Failed to queue message to partition {}: {}", partition, e.getMessage());
            return ResponseEntity.internalServerError().body("Failed to send message to partition");
        }
    }

    /**
     * Send message using business partition strategy with retry
     */
    @PostMapping("/send-business-partition")
    public ResponseEntity<String> sendMessageWithBusinessPartition(@RequestBody ChatMessage message) {
        try {
            message.setTimestamp(LocalDateTime.now());
            chatMessageProducer.sendMessageWithBusinessPartition(message);
            logger.info("Message with business partition queued for sending: {}", message);
            return ResponseEntity.ok("Message sent with business partition successfully (async)");
        } catch (Exception e) {
            logger.error("Failed to queue message with business partition: {}", e.getMessage());
            return ResponseEntity.internalServerError().body("Failed to send message with business partition");
        }
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Chat message service with retry is healthy");
    }
}
