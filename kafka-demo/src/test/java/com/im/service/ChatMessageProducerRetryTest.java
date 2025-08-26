package com.im.service;

import com.im.dto.ChatMessage;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatMessageProducerRetryTest {

    @Mock
    private KafkaTemplate<String, ChatMessage> kafkaTemplate;

    @Mock
    private SendResult<String, ChatMessage> sendResult;

    @InjectMocks
    private ChatMessageProducer chatMessageProducer;

    private ChatMessage testMessage;

    @BeforeEach
    void setUp() {
        // Set configuration values
        ReflectionTestUtils.setField(chatMessageProducer, "topic", "test-topic");
        ReflectionTestUtils.setField(chatMessageProducer, "maxRetryAttempts", 3);
        ReflectionTestUtils.setField(chatMessageProducer, "retryDelayMs", 100L);

        // Create test message
        testMessage = new ChatMessage();
        testMessage.setSender("user1");
        testMessage.setReceiver("user2");
        testMessage.setContent("Test message");
        testMessage.setTimestamp(LocalDateTime.now());
    }

    @Test
    void testSendMessageSuccess() {
        // Arrange
        CompletableFuture<SendResult<String, ChatMessage>> future = CompletableFuture.completedFuture(sendResult);
        when(kafkaTemplate.send(anyString(), any(ChatMessage.class))).thenReturn(future);

        // Act
        assertDoesNotThrow(() -> chatMessageProducer.sendMessage(testMessage));

        // Assert
        verify(kafkaTemplate, times(1)).send("test-topic", testMessage);
    }

    @Test
    void testSendMessageWithKeySuccess() {
        // Arrange
        String key = "test-key";
        CompletableFuture<SendResult<String, ChatMessage>> future = CompletableFuture.completedFuture(sendResult);
        when(kafkaTemplate.send(anyString(), anyString(), any(ChatMessage.class))).thenReturn(future);

        // Act
        assertDoesNotThrow(() -> chatMessageProducer.sendMessageWithKey(key, testMessage));

        // Assert
        verify(kafkaTemplate, times(1)).send("test-topic", key, testMessage);
    }

    @Test
    void testSendMessageToPartitionSuccess() {
        // Arrange
        int partition = 1;
        String key = "test-key";
        CompletableFuture<SendResult<String, ChatMessage>> future = CompletableFuture.completedFuture(sendResult);
        when(kafkaTemplate.send(any(ProducerRecord.class))).thenReturn(future);

        // Act
        assertDoesNotThrow(() -> chatMessageProducer.sendMessageToPartition(partition, key, testMessage));

        // Assert
        verify(kafkaTemplate, times(1)).send(any(ProducerRecord.class));
    }

    @Test
    void testSendMessageSyncSuccess() {
        // Arrange
        CompletableFuture<SendResult<String, ChatMessage>> future = CompletableFuture.completedFuture(sendResult);
        when(kafkaTemplate.send(anyString(), any(ChatMessage.class))).thenReturn(future);

        // Act
        boolean result = chatMessageProducer.sendMessageSync(testMessage);

        // Assert
        assertTrue(result);
        verify(kafkaTemplate, times(1)).send("test-topic", testMessage);
    }

    @Test
    void testSendMessageWithBusinessPartition() {
        // Arrange
        CompletableFuture<SendResult<String, ChatMessage>> future = CompletableFuture.completedFuture(sendResult);
        when(kafkaTemplate.send(anyString(), anyString(), any(ChatMessage.class))).thenReturn(future);

        // Act
        assertDoesNotThrow(() -> chatMessageProducer.sendMessageWithBusinessPartition(testMessage));

        // Assert
        verify(kafkaTemplate, times(1)).send(eq("test-topic"), eq("user1:user2"), eq(testMessage));
    }

    @Test
    void testGeneratePartitionKeyOrdering() {
        // Test that partition key is consistent regardless of sender/receiver order
        testMessage.setSender("userB");
        testMessage.setReceiver("userA");
        
        CompletableFuture<SendResult<String, ChatMessage>> future = CompletableFuture.completedFuture(sendResult);
        when(kafkaTemplate.send(anyString(), anyString(), any(ChatMessage.class))).thenReturn(future);

        chatMessageProducer.sendMessageWithBusinessPartition(testMessage);

        // Should generate "userA:userB" regardless of original order
        verify(kafkaTemplate, times(1)).send(eq("test-topic"), eq("userA:userB"), eq(testMessage));
    }
}
