package com.im.service;

import com.im.dto.ChatMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.PartitionOffset;
import org.springframework.kafka.annotation.TopicPartition;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.util.Random;

@Service
public class ChatMessageConsumer {

    private static final Logger logger = LoggerFactory.getLogger(ChatMessageConsumer.class);

    @Value("${spring.kafka.topic.name}")
    private String topic;

    private final KafkaTemplate<String, ChatMessage> kafkaTemplate;

    @Autowired
    public ChatMessageConsumer(KafkaTemplate<String, ChatMessage> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    // Listen to specific partitions (0 and 1) of the topic
    @KafkaListener(
        topicPartitions = @TopicPartition(
            topic = "${spring.kafka.topic.name}", 
            partitions = {"0", "1"}
        ),
        groupId = "${spring.kafka.consumer.group-id}"
    )
    public void consume(@Payload ChatMessage message,
                        @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                        @Header(KafkaHeaders.OFFSET) long offset,
                        Acknowledgment acknowledgment) {
        logger.info("Consumed message from partition {}: {} with offset: {}", partition, message, offset);

        // Process the message for specific partitions
        processMessageFromPartition(message, partition);

        // Manually acknowledge the message if required
        acknowledgment.acknowledge();
    }

    /**
     * Process messages from specific partitions (0 and 1)
     */
    private void processMessageFromPartition(ChatMessage message, int partition) {
        switch (partition) {
            case 0:
                logger.info("Processing message from partition 0: {}", message.getContent());
                // Business logic for partition 0
                break;
            case 1:
                logger.info("Processing message from partition 1: {}", message.getContent());
                // Business logic for partition 1
                break;
            default:
                logger.warn("Unexpected partition: {}", partition);
        }
    }
}