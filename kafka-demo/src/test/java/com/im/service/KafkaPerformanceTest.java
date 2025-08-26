package com.im.service;

import com.im.dto.ChatMessage;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@SpringBootTest
@DirtiesContext
@EmbeddedKafka(
    partitions = 3,
    topics = {"chat-message-topic"},
    brokerProperties = {"listeners=PLAINTEXT://localhost:9092", "port=9092"}
)
@TestPropertySource(properties = {
    "spring.kafka.bootstrap-servers=localhost:9092",
    "spring.kafka.consumer.bootstrap-servers=localhost:9092",
    "spring.kafka.producer.bootstrap-servers=localhost:9092"
})
public class KafkaPerformanceTest {

    @Autowired
    private ChatMessageProducer chatMessageProducer;

    @Autowired
    private KafkaTemplate<String, ChatMessage> kafkaTemplate;

    private final AtomicInteger consumedMessageCount = new AtomicInteger(0);

    @Test
    public void testKafkaProducerPerformance() throws InterruptedException {
        int messageCount = 1000;
        int batchSize = 100;
        
        System.out.println("开始Kafka生产者性能测试...");
        long startTime = System.currentTimeMillis();
        
        ExecutorService executor = Executors.newFixedThreadPool(10);
        List<Future<Void>> futures = new ArrayList<>();
        
        for (int batch = 0; batch < messageCount / batchSize; batch++) {
            final int batchNumber = batch;
            Future<Void> future = executor.submit(() -> {
                for (int i = 0; i < batchSize; i++) {
                    ChatMessage message = createTestMessage(batchNumber * batchSize + i);
                    chatMessageProducer.sendMessage(message);
                }
                return null;
            });
            futures.add(future);
        }
        
        // 等待所有批次完成
        for (Future<Void> future : futures) {
            try {
                future.get(30, TimeUnit.SECONDS);
            } catch (ExecutionException | TimeoutException e) {
                e.printStackTrace();
            }
        }
        
        executor.shutdown();
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        System.out.println("生产者性能测试结果:");
        System.out.println("发送消息数量: " + messageCount);
        System.out.println("总耗时: " + duration + "ms");
        System.out.println("吞吐量: " + (messageCount * 1000.0 / duration) + " 消息/秒");
        System.out.println("平均延迟: " + (duration / (double) messageCount) + "ms/消息");
    }

    @Test
    public void testKafkaProducerConsumerPerformance() throws InterruptedException {
        int messageCount = 500;
        CountDownLatch latch = new CountDownLatch(messageCount);
        
        // 创建消费者监听器
        TestMessageConsumer testConsumer = new TestMessageConsumer(latch);
        
        System.out.println("开始Kafka生产消费性能测试...");
        long startTime = System.currentTimeMillis();
        
        // 异步发送消息
        ExecutorService producerExecutor = Executors.newFixedThreadPool(5);
        for (int i = 0; i < messageCount; i++) {
            final int messageIndex = i;
            producerExecutor.submit(() -> {
                ChatMessage message = createTestMessage(messageIndex);
                chatMessageProducer.sendMessage(message);
            });
        }
        
        // 等待所有消息被消费
        boolean finished = latch.await(60, TimeUnit.SECONDS);
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        producerExecutor.shutdown();
        
        System.out.println("生产消费性能测试结果:");
        System.out.println("是否完成: " + finished);
        System.out.println("发送消息数量: " + messageCount);
        System.out.println("消费消息数量: " + testConsumer.getConsumedCount());
        System.out.println("总耗时: " + duration + "ms");
        System.out.println("端到端吞吐量: " + (testConsumer.getConsumedCount() * 1000.0 / duration) + " 消息/秒");
    }

    @Test
    public void testKafkaConcurrentProducers() throws InterruptedException {
        int concurrentProducers = 10;
        int messagesPerProducer = 100;
        
        System.out.println("开始Kafka并发生产者测试...");
        long startTime = System.currentTimeMillis();
        
        ExecutorService executor = Executors.newFixedThreadPool(concurrentProducers);
        CountDownLatch latch = new CountDownLatch(concurrentProducers);
        
        for (int producerId = 0; producerId < concurrentProducers; producerId++) {
            final int pid = producerId;
            executor.submit(() -> {
                try {
                    for (int i = 0; i < messagesPerProducer; i++) {
                        ChatMessage message = createTestMessage(pid * messagesPerProducer + i);
                        message.setSender("Producer" + pid);
                        chatMessageProducer.sendMessage(message);
                    }
                } finally {
                    latch.countDown();
                }
            });
        }
        
        latch.await(30, TimeUnit.SECONDS);
        executor.shutdown();
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        int totalMessages = concurrentProducers * messagesPerProducer;
        
        System.out.println("并发生产者测试结果:");
        System.out.println("并发生产者数量: " + concurrentProducers);
        System.out.println("每个生产者消息数: " + messagesPerProducer);
        System.out.println("总消息数: " + totalMessages);
        System.out.println("总耗时: " + duration + "ms");
        System.out.println("并发吞吐量: " + (totalMessages * 1000.0 / duration) + " 消息/秒");
    }

    @Test
    public void testKafkaMessageSizePerformance() throws InterruptedException {
        System.out.println("开始Kafka不同消息大小性能测试...");
        
        // 测试不同大小的消息
        int[] messageSizes = {100, 1000, 10000, 100000}; // 字节
        int messageCount = 100;
        
        for (int size : messageSizes) {
            long startTime = System.currentTimeMillis();
            
            for (int i = 0; i < messageCount; i++) {
                ChatMessage message = createTestMessageWithSize(i, size);
                chatMessageProducer.sendMessage(message);
            }
            
            // 等待一段时间确保消息发送完成
            Thread.sleep(1000);
            
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;
            
            System.out.println("消息大小: " + size + " 字节");
            System.out.println("发送 " + messageCount + " 条消息耗时: " + duration + "ms");
            System.out.println("吞吐量: " + (messageCount * 1000.0 / duration) + " 消息/秒");
            System.out.println("数据吞吐量: " + (messageCount * size * 1000.0 / duration / 1024 / 1024) + " MB/秒");
            System.out.println("---");
        }
    }

    private ChatMessage createTestMessage(int index) {
        ChatMessage message = new ChatMessage();
        message.setId(UUID.randomUUID().toString());
        message.setContent("测试消息内容 " + index);
        message.setSender("testSender" + (index % 10));
        message.setReceiver("testReceiver" + (index % 5));
        message.setTimestamp(LocalDateTime.now());
        return message;
    }

    private ChatMessage createTestMessageWithSize(int index, int targetSize) {
        ChatMessage message = new ChatMessage();
        message.setId(UUID.randomUUID().toString());
        
        // 创建指定大小的消息内容
        StringBuilder content = new StringBuilder();
        String baseContent = "测试消息内容 " + index + " ";
        while (content.length() < targetSize) {
            content.append(baseContent);
        }
        // 截取到指定大小
        if (content.length() > targetSize) {
            content.setLength(targetSize);
        }
        
        message.setContent(content.toString());
        message.setSender("testSender" + (index % 10));
        message.setReceiver("testReceiver" + (index % 5));
        message.setTimestamp(LocalDateTime.now());
        return message;
    }

    // 内部测试消费者类
    private static class TestMessageConsumer {
        private final CountDownLatch latch;
        private final AtomicInteger consumedCount = new AtomicInteger(0);

        public TestMessageConsumer(CountDownLatch latch) {
            this.latch = latch;
        }

        // 模拟消息消费
        public void consumeMessage(ChatMessage message) {
            consumedCount.incrementAndGet();
            latch.countDown();
        }

        public int getConsumedCount() {
            return consumedCount.get();
        }
    }
}
