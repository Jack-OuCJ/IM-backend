package com.im.service;

import com.im.dto.ChatMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
public class ChatMessageProducer {

    private static final Logger logger = LoggerFactory.getLogger(ChatMessageProducer.class);

    @Autowired
    private KafkaTemplate<String, ChatMessage> kafkaTemplate;

    @Value("${kafka.topic}")
    private String topic;

    public void sendMessage(ChatMessage message) {
        CompletableFuture<SendResult<String, ChatMessage>> future = kafkaTemplate.send(topic, message);

        future.whenComplete((result, ex) -> {
            if (ex != null) {
                logger.error("Error sending message: {}", ex.getMessage());
            } else {
                logger.info("Message sent successfully: {}", result.getProducerRecord().value());
            }
        });
    }
}