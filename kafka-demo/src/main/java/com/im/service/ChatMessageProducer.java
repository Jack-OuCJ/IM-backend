package com.im.service;

import com.im.dto.ChatMessage;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Service
public class ChatMessageProducer {

    private static final Logger logger = LoggerFactory.getLogger(ChatMessageProducer.class);

    @Autowired
    private KafkaTemplate<String, ChatMessage> kafkaTemplate;

    @Value("${spring.kafka.topic.name}")
    private String topic;

    @Value("${kafka.producer.retry.max-attempts:3}")
    private int maxRetryAttempts;

    @Value("${kafka.producer.retry.delay-ms:1000}")
    private long retryDelayMs;

    /**
     * Send message using default partition strategy (round-robin)
     */
    public void sendMessage(ChatMessage message) {
        sendMessageWithRetry(() -> kafkaTemplate.send(topic, message), 
                           "default partition", message, 1);
    }

    /**
     * Send message with specified key (partition assigned based on key hash)
     */
    public void sendMessageWithKey(String key, ChatMessage message) {
        sendMessageWithRetry(() -> kafkaTemplate.send(topic, key, message), 
                           "key: " + key, message, 1);
    }

    /**
     * Send message to specified partition
     */
    public void sendMessageToPartition(int partition, String key, ChatMessage message) {
        ProducerRecord<String, ChatMessage> record = new ProducerRecord<>(topic, partition, key, message);
        sendMessageWithRetry(() -> kafkaTemplate.send(record), 
                           "partition: " + partition, message, 1);
    }

    /**
     * Send message using business partition strategy based on sender and receiver
     */
    public void sendMessageWithBusinessPartition(ChatMessage message) {
        String partitionKey = generatePartitionKey(message.getSender(), message.getReceiver());
        sendMessageWithKey(partitionKey, message);
    }

    /**
     * Send message synchronously with retry (blocks until success or max retries reached)
     */
    public boolean sendMessageSync(ChatMessage message) {
        return sendMessageSyncWithRetry(() -> kafkaTemplate.send(topic, message), 
                                      "default partition sync", message);
    }

    /**
     * Send message with key synchronously with retry
     */
    public boolean sendMessageWithKeySync(String key, ChatMessage message) {
        return sendMessageSyncWithRetry(() -> kafkaTemplate.send(topic, key, message), 
                                      "key: " + key + " sync", message);
    }

    /**
     * Send message to specified partition synchronously with retry
     */
    public boolean sendMessageToPartitionSync(int partition, String key, ChatMessage message) {
        ProducerRecord<String, ChatMessage> record = new ProducerRecord<>(topic, partition, key, message);
        return sendMessageSyncWithRetry(() -> kafkaTemplate.send(record), 
                                      "partition: " + partition + " sync", message);
    }

    /**
     * Generate partition key ensuring same conversation uses same partition
     */
    private String generatePartitionKey(String sender, String receiver) {
        if (sender.compareTo(receiver) < 0) {
            return sender + ":" + receiver;
        } else {
            return receiver + ":" + sender;
        }
    }

    /**
     * Send message with retry mechanism
     */
    private void sendMessageWithRetry(MessageSender sender, String context, ChatMessage message, int attempt) {
        CompletableFuture<SendResult<String, ChatMessage>> future = sender.send();

        future.whenComplete((result, ex) -> {
            if (ex != null) {
                if (attempt < maxRetryAttempts) {
                    logger.warn("Failed to send message on attempt {} for {}: {}. Retrying in {}ms...", 
                              attempt, context, ex.getMessage(), retryDelayMs);
                    
                    // Schedule retry with delay
                    CompletableFuture.delayedExecutor(retryDelayMs, TimeUnit.MILLISECONDS)
                        .execute(() -> sendMessageWithRetry(sender, context, message, attempt + 1));
                } else {
                    logger.error("Failed to send message after {} attempts for {}: {}", 
                               maxRetryAttempts, context, ex.getMessage());
                }
            } else {
                RecordMetadata metadata = result.getRecordMetadata();
                if (attempt > 1) {
                    logger.info("Message sent successfully for {} on attempt {}: partition={}, offset={}, topic={}", 
                              context, attempt, metadata.partition(), metadata.offset(), metadata.topic());
                } else {
                    logger.info("Message sent successfully for {}: partition={}, offset={}, topic={}", 
                              context, metadata.partition(), metadata.offset(), metadata.topic());
                }
            }
        });
    }

    /**
     * Send message synchronously with retry mechanism
     */
    private boolean sendMessageSyncWithRetry(MessageSender sender, String context, ChatMessage message) {
        for (int attempt = 1; attempt <= maxRetryAttempts; attempt++) {
            try {
                CompletableFuture<SendResult<String, ChatMessage>> future = sender.send();
                SendResult<String, ChatMessage> result = future.get();
                
                RecordMetadata metadata = result.getRecordMetadata();
                if (attempt > 1) {
                    logger.info("Message sent successfully for {} on attempt {}: partition={}, offset={}, topic={}", 
                              context, attempt, metadata.partition(), metadata.offset(), metadata.topic());
                } else {
                    logger.info("Message sent successfully for {}: partition={}, offset={}, topic={}", 
                              context, metadata.partition(), metadata.offset(), metadata.topic());
                }
                return true;
                
            } catch (Exception ex) {
                if (attempt < maxRetryAttempts) {
                    logger.warn("Failed to send message on attempt {} for {}: {}. Retrying in {}ms...", 
                              attempt, context, ex.getMessage(), retryDelayMs);
                    try {
                        Thread.sleep(retryDelayMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        logger.error("Retry interrupted for {}: {}", context, ie.getMessage());
                        return false;
                    }
                } else {
                    logger.error("Failed to send message after {} attempts for {}: {}", 
                               maxRetryAttempts, context, ex.getMessage());
                }
            }
        }
        return false;
    }

    /**
     * Get partition count for the topic
     */
    public int getPartitionCount() {
        try {
            return kafkaTemplate.getProducerFactory()
                    .createProducer()
                    .partitionsFor(topic)
                    .size();
        } catch (Exception e) {
            logger.error("Failed to get partition count: {}", e.getMessage());
            return -1;
        }
    }

    /**
     * Functional interface for message sending operations
     */
    @FunctionalInterface
    private interface MessageSender {
        CompletableFuture<SendResult<String, ChatMessage>> send();
    }
}