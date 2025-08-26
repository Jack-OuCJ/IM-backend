package com.im.service;

import com.im.dto.ChatMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
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

    @Value("${kafka.topic}")
    private String topic;

    private final KafkaTemplate<String, ChatMessage> kafkaTemplate;

    @Autowired
    public ChatMessageConsumer(KafkaTemplate<String, ChatMessage> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @KafkaListener(topics = "${kafka.topic}", groupId = "${kafka.consumer.group-id}")
    public void consume(@Payload ChatMessage message,
                        @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                        @Header(KafkaHeaders.OFFSET) long offset,
                        Acknowledgment acknowledgment) {
        logger.info("Consumed message: {} from partition: {} with offset: {}", message, partition, offset);

        // Process the message
        // ...

        // Manually acknowledge the message if required
        acknowledgment.acknowledge();
    }

    // Other methods...
}