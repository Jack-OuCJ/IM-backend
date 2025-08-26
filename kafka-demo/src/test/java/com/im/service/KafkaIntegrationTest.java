package com.im.service;

import com.im.dto.ChatMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = {com.im.ImBackendApplication.class})
@TestPropertySource(properties = {
    "spring.kafka.bootstrap-servers=localhost:9092,localhost:9094,localhost:9096",
    "kafka.topic=test-chat-message-topic",
    "kafka.consumer.group-id=test-consumer-group"
})
public class KafkaIntegrationTest {

    private static final Logger logger = LoggerFactory.getLogger(KafkaIntegrationTest.class);

    @Autowired
    private ChatMessageProducer chatMessageProducer;

    private CountDownLatch latch;
    private AtomicInteger receivedMessageCount;
    private ChatMessage lastReceivedMessage;

    @BeforeEach
    void setUp() {
        receivedMessageCount = new AtomicInteger(0);
        lastReceivedMessage = null;
    }

    @Test
    public void testSingleMessageProduceAndConsume() throws InterruptedException {
        // 准备测试数据
        latch = new CountDownLatch(1);
        ChatMessage testMessage = createTestMessage("单条消息测试");

        // 发送消息
        logger.info("发送测试消息: {}", testMessage.getContent());
        chatMessageProducer.sendMessage(testMessage);

        // 等待消息被消费
        boolean messageReceived = latch.await(10, TimeUnit.SECONDS);

        // 验证结果
        assertTrue(messageReceived, "消息应该在10秒内被消费");
        assertEquals(1, receivedMessageCount.get(), "应该接收到1条消息");
        assertNotNull(lastReceivedMessage, "应该接收到消息");
        assertEquals(testMessage.getContent(), lastReceivedMessage.getContent(), "消息内容应该匹配");
        assertEquals(testMessage.getSender(), lastReceivedMessage.getSender(), "发送者应该匹配");
        assertEquals(testMessage.getReceiver(), lastReceivedMessage.getReceiver(), "接收者应该匹配");

        logger.info("单条消息测试通过");
    }

    @Test
    public void testMultipleMessagesProduceAndConsume() throws InterruptedException {
        int messageCount = 10;
        latch = new CountDownLatch(messageCount);

        logger.info("开始发送{}条消息", messageCount);
        long startTime = System.currentTimeMillis();

        // 发送多条消息
        for (int i = 0; i < messageCount; i++) {
            ChatMessage message = createTestMessage("批量消息测试 " + (i + 1));
            chatMessageProducer.sendMessage(message);
        }

        // 等待所有消息被消费
        boolean allMessagesReceived = latch.await(30, TimeUnit.SECONDS);
        long endTime = System.currentTimeMillis();

        // 验证结果
        assertTrue(allMessagesReceived, "所有消息应该在30秒内被消费");
        assertEquals(messageCount, receivedMessageCount.get(), "应该接收到" + messageCount + "条消息");

        long duration = endTime - startTime;
        double throughput = messageCount * 1000.0 / duration;

        logger.info("批量消息测试通过");
        logger.info("发送并消费{}条消息耗时: {}ms", messageCount, duration);
        logger.info("吞吐量: {:.2f} 消息/秒", throughput);
    }

    @Test
    public void testHighVolumeMessages() throws InterruptedException {
        int messageCount = 100;
        latch = new CountDownLatch(messageCount);

        logger.info("开始高并发消息测试，发送{}条消息", messageCount);
        long startTime = System.currentTimeMillis();

        // 使用线程池并发发送消息
        for (int i = 0; i < messageCount; i++) {
            final int messageIndex = i;
            // 直接发送，不使用额外线程池以避免测试复杂性
            ChatMessage message = createTestMessage("高并发测试消息 " + (messageIndex + 1));
            message.setSender("sender" + (messageIndex % 5)); // 模拟多个发送者
            message.setReceiver("receiver" + (messageIndex % 3)); // 模拟多个接收者
            chatMessageProducer.sendMessage(message);
        }

        // 等待所有消息被消费
        boolean allMessagesReceived = latch.await(60, TimeUnit.SECONDS);
        long endTime = System.currentTimeMillis();

        // 验证结果
        assertTrue(allMessagesReceived, "所有消息应该在60秒内被消费");
        assertEquals(messageCount, receivedMessageCount.get(), "应该接收到" + messageCount + "条消息");

        long duration = endTime - startTime;
        double throughput = messageCount * 1000.0 / duration;

        logger.info("高并发消息测试通过");
        logger.info("发送并消费{}条消息耗时: {}ms", messageCount, duration);
        logger.info("平均吞吐量: {:.2f} 消息/秒", throughput);
        logger.info("平均每条消息处理时间: {:.2f}ms", (double) duration / messageCount);
    }

    @Test
    public void testLargeMessageProduceAndConsume() throws InterruptedException {
        latch = new CountDownLatch(1);

        // 创建大消息（约10KB）
        StringBuilder largeContent = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            largeContent.append("这是一条很长的测试消息，用于测试Kafka处理大消息的能力。消息序号：").append(i).append("。");
        }

        ChatMessage largeMessage = createTestMessage(largeContent.toString());
        
        logger.info("发送大消息，大小约: {} 字节", largeContent.length());
        long startTime = System.currentTimeMillis();

        chatMessageProducer.sendMessage(largeMessage);

        boolean messageReceived = latch.await(15, TimeUnit.SECONDS);
        long endTime = System.currentTimeMillis();

        assertTrue(messageReceived, "大消息应该在15秒内被消费");
        assertEquals(1, receivedMessageCount.get(), "应该接收到1条大消息");
        assertNotNull(lastReceivedMessage, "应该接收到大消息");

        long duration = endTime - startTime;
        double sizeInMB = largeContent.length() / (1024.0 * 1024.0);

        logger.info("大消息测试通过");
        logger.info("消息大小: {:.2f} KB", largeContent.length() / 1024.0);
        logger.info("处理时间: {}ms", duration);
        logger.info("传输速率: {:.2f} MB/秒", sizeInMB * 1000.0 / duration);
    }

    @KafkaListener(topics = "test-chat-message-topic", groupId = "test-consumer-group")
    public void testConsume(@Payload ChatMessage message,
                           @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                           @Header(KafkaHeaders.OFFSET) long offset,
                           Acknowledgment acknowledgment) {
        
        logger.debug("测试消费者接收到消息: {} 来自分区: {} 偏移量: {}", 
                    message.getContent(), partition, offset);
        
        lastReceivedMessage = message;
        receivedMessageCount.incrementAndGet();
        
        if (latch != null) {
            latch.countDown();
        }
        
        acknowledgment.acknowledge();
    }

    private ChatMessage createTestMessage(String content) {
        ChatMessage message = new ChatMessage();
        message.setId(UUID.randomUUID().toString());
        message.setContent(content);
        message.setSender("testSender");
        message.setReceiver("testReceiver");
        message.setTimestamp(LocalDateTime.now());
        return message;
    }
}
