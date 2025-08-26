package com.im.service;

import com.im.dto.ChatMessage;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 简单的Kafka性能测试，不依赖Spring Context
 * 直接使用Kafka原生客户端进行测试
 */
public class SimpleKafkaPerformanceTest {

    private static final Logger logger = LoggerFactory.getLogger(SimpleKafkaPerformanceTest.class);
    
    private static final String BOOTSTRAP_SERVERS = "localhost:9092,localhost:9094,localhost:9096";
    private static final String TOPIC_NAME = "kafka-test-topic";
    
    private Producer<String, ChatMessage> producer;
    private KafkaConsumer<String, ChatMessage> consumer;

    @BeforeEach
    void setUp() {
        // 配置生产者
        Properties producerProps = new Properties();
        producerProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
        producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        producerProps.put(ProducerConfig.ACKS_CONFIG, "all");
        producerProps.put(ProducerConfig.RETRIES_CONFIG, 3);
        producerProps.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384);
        producerProps.put(ProducerConfig.LINGER_MS_CONFIG, 1);
        
        producer = new KafkaProducer<>(producerProps);

        // 配置消费者
        Properties consumerProps = new Properties();
        consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
        consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, "test-consumer-group-" + UUID.randomUUID());
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        consumerProps.put(JsonDeserializer.TRUSTED_PACKAGES, "com.im.dto");
        consumerProps.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        
        consumer = new KafkaConsumer<>(consumerProps);
        consumer.subscribe(Collections.singletonList(TOPIC_NAME));
    }

    @AfterEach
    void tearDown() {
        if (producer != null) {
            producer.close();
        }
        if (consumer != null) {
            consumer.close();
        }
    }

    @Test
    void testProducerThroughput() {
        int messageCount = 1000;
        logger.info("开始生产者吞吐量测试，发送 {} 条消息", messageCount);
        
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < messageCount; i++) {
            ChatMessage message = createTestMessage(i);
            ProducerRecord<String, ChatMessage> record = 
                new ProducerRecord<>(TOPIC_NAME, message.getId(), message);
            
            producer.send(record, (metadata, exception) -> {
                if (exception != null) {
                    logger.error("发送消息失败: {}", exception.getMessage());
                }
            });
            
            if ((i + 1) % 100 == 0) {
                logger.info("已发送 {} 条消息", i + 1);
            }
        }
        
        producer.flush(); // 确保所有消息都发送完成
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        logger.info("生产者吞吐量测试完成");
        logger.info("发送消息数: {}", messageCount);
        logger.info("总耗时: {} ms", duration);
        logger.info("平均吞吐量: {:.2f} 消息/秒", messageCount * 1000.0 / duration);
        logger.info("平均延迟: {:.2f} ms/消息", (double) duration / messageCount);
        
        assertTrue(duration > 0, "测试应该消耗一些时间");
    }

    @Test
    void testProducerConsumerEndToEnd() throws InterruptedException {
        int messageCount = 100;
        CountDownLatch latch = new CountDownLatch(messageCount);
        AtomicInteger consumedCount = new AtomicInteger(0);
        
        logger.info("开始端到端测试，发送和接收 {} 条消息", messageCount);
        
        // 启动消费者线程
        ExecutorService consumerExecutor = Executors.newSingleThreadExecutor();
        consumerExecutor.submit(() -> {
            try {
                while (consumedCount.get() < messageCount) {
                    ConsumerRecords<String, ChatMessage> records = consumer.poll(Duration.ofMillis(1000));
                    for (ConsumerRecord<String, ChatMessage> record : records) {
                        logger.debug("消费消息: {} 来自分区: {} 偏移量: {}", 
                                   record.value().getContent(), record.partition(), record.offset());
                        
                        int count = consumedCount.incrementAndGet();
                        latch.countDown();
                        
                        if (count % 20 == 0) {
                            logger.info("已消费 {} 条消息", count);
                        }
                    }
                    consumer.commitSync();
                }
            } catch (Exception e) {
                logger.error("消费消息时出错: {}", e.getMessage());
            }
        });
        
        // 等待一点时间让消费者准备好
        Thread.sleep(2000);
        
        long startTime = System.currentTimeMillis();
        
        // 发送消息
        for (int i = 0; i < messageCount; i++) {
            ChatMessage message = createTestMessage(i);
            ProducerRecord<String, ChatMessage> record = 
                new ProducerRecord<>(TOPIC_NAME, message.getId(), message);
            
            producer.send(record);
            
            if ((i + 1) % 20 == 0) {
                logger.info("已发送 {} 条消息", i + 1);
            }
        }
        
        producer.flush();
        
        // 等待所有消息被消费
        boolean allConsumed = latch.await(30, TimeUnit.SECONDS);
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        consumerExecutor.shutdown();
        
        logger.info("端到端测试完成");
        logger.info("是否全部消费: {}", allConsumed);
        logger.info("发送消息数: {}", messageCount);
        logger.info("消费消息数: {}", consumedCount.get());
        logger.info("总耗时: {} ms", duration);
        logger.info("端到端吞吐量: {:.2f} 消息/秒", consumedCount.get() * 1000.0 / duration);
        
        assertTrue(allConsumed, "所有消息应该在30秒内被消费");
        assertEquals(messageCount, consumedCount.get(), "消费的消息数应该等于发送的消息数");
    }

    @Test
    void testConcurrentProducers() throws InterruptedException {
        int threadCount = 5;
        int messagesPerThread = 100;
        int totalMessages = threadCount * messagesPerThread;
        
        logger.info("开始并发生产者测试，{} 个线程，每个发送 {} 条消息", threadCount, messagesPerThread);
        
        AtomicLong sentCount = new AtomicLong(0);
        AtomicLong errorCount = new AtomicLong(0);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        
        long startTime = System.currentTimeMillis();
        
        for (int t = 0; t < threadCount; t++) {
            final int threadId = t;
            executor.submit(() -> {
                try {
                    for (int i = 0; i < messagesPerThread; i++) {
                        ChatMessage message = createTestMessage(threadId * messagesPerThread + i);
                        message.setSender("Thread" + threadId);
                        
                        ProducerRecord<String, ChatMessage> record = 
                            new ProducerRecord<>(TOPIC_NAME, message.getId(), message);
                        
                        producer.send(record, (metadata, exception) -> {
                            if (exception != null) {
                                errorCount.incrementAndGet();
                                logger.error("发送消息失败: {}", exception.getMessage());
                            } else {
                                sentCount.incrementAndGet();
                            }
                        });
                    }
                } finally {
                    latch.countDown();
                }
            });
        }
        
        boolean finished = latch.await(30, TimeUnit.SECONDS);
        producer.flush();
        
        executor.shutdown();
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        logger.info("并发生产者测试完成");
        logger.info("是否正常完成: {}", finished);
        logger.info("预期消息数: {}", totalMessages);
        logger.info("成功发送消息数: {}", sentCount.get());
        logger.info("错误消息数: {}", errorCount.get());
        logger.info("总耗时: {} ms", duration);
        logger.info("平均吞吐量: {:.2f} 消息/秒", sentCount.get() * 1000.0 / duration);
        logger.info("错误率: {:.2f}%", errorCount.get() * 100.0 / totalMessages);
        
        assertTrue(finished, "测试应该在30秒内完成");
        assertTrue(sentCount.get() > totalMessages * 0.95, "至少95%的消息应该发送成功");
    }

    @Test
    void testDifferentMessageSizes() {
        int[] messageSizes = {100, 1000, 10000}; // 字节
        int messagesPerSize = 50;
        
        logger.info("开始不同消息大小性能测试");
        
        for (int size : messageSizes) {
            logger.info("测试消息大小: {} 字节", size);
            
            long startTime = System.currentTimeMillis();
            
            for (int i = 0; i < messagesPerSize; i++) {
                ChatMessage message = createTestMessageWithSize(i, size);
                ProducerRecord<String, ChatMessage> record = 
                    new ProducerRecord<>(TOPIC_NAME, message.getId(), message);
                
                producer.send(record);
            }
            
            producer.flush();
            
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;
            
            double throughputMsgs = messagesPerSize * 1000.0 / duration;
            double throughputMB = (messagesPerSize * size * 1000.0) / (duration * 1024 * 1024);
            
            logger.info("  发送 {} 条 {} 字节消息耗时: {} ms", messagesPerSize, size, duration);
            logger.info("  消息吞吐量: {:.2f} 消息/秒", throughputMsgs);
            logger.info("  数据吞吐量: {:.2f} MB/秒", throughputMB);
            logger.info("");
            
            assertTrue(duration > 0, "测试应该消耗一些时间");
            assertTrue(throughputMsgs > 0, "吞吐量应该大于0");
        }
    }

    private ChatMessage createTestMessage(int index) {
        ChatMessage message = new ChatMessage();
        message.setId(UUID.randomUUID().toString());
        message.setContent("测试消息 #" + index + " - " + System.currentTimeMillis());
        message.setSender("testSender" + (index % 5));
        message.setReceiver("testReceiver" + (index % 3));
        message.setTimestamp(LocalDateTime.now());
        return message;
    }

    private ChatMessage createTestMessageWithSize(int index, int targetSize) {
        ChatMessage message = new ChatMessage();
        message.setId(UUID.randomUUID().toString());
        
        StringBuilder content = new StringBuilder();
        String pattern = "测试数据填充 ";
        
        while (content.length() < targetSize) {
            content.append(pattern);
        }
        
        if (content.length() > targetSize) {
            content.setLength(targetSize);
        }
        
        message.setContent(content.toString());
        message.setSender("testSender" + (index % 5));
        message.setReceiver("testReceiver" + (index % 3));
        message.setTimestamp(LocalDateTime.now());
        return message;
    }
}
