package com.im.service;

import com.im.dto.ChatMessage;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Kafka生产者基准测试
 * 注意：这个测试需要Kafka集群正在运行
 */
@SpringBootTest(classes = {com.im.ImBackendApplication.class})
public class KafkaBenchmarkTest {

    private static final Logger logger = LoggerFactory.getLogger(KafkaBenchmarkTest.class);

    @Autowired
    private ChatMessageProducer chatMessageProducer;

    @Test
    public void benchmarkProducerThroughput() throws InterruptedException {
        int totalMessages = 1000;
        int threadCount = 10;
        
        logger.info("开始Kafka生产者吞吐量基准测试");
        logger.info("总消息数: {}, 线程数: {}", totalMessages, threadCount);
        
        AtomicLong sentCount = new AtomicLong(0);
        long startTime = System.currentTimeMillis();
        
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        
        for (int i = 0; i < totalMessages; i++) {
            final int messageIndex = i;
            executor.submit(() -> {
                try {
                    ChatMessage message = createBenchmarkMessage(messageIndex);
                    chatMessageProducer.sendMessage(message);
                    long count = sentCount.incrementAndGet();
                    
                    if (count % 100 == 0) {
                        logger.info("已发送 {} 条消息", count);
                    }
                } catch (Exception e) {
                    logger.error("发送消息失败: {}", e.getMessage());
                }
            });
        }
        
        executor.shutdown();
        boolean finished = executor.awaitTermination(120, TimeUnit.SECONDS);
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        logger.info("基准测试完成");
        logger.info("是否正常完成: {}", finished);
        logger.info("实际发送消息数: {}", sentCount.get());
        logger.info("总耗时: {} ms", duration);
        logger.info("平均吞吐量: {:.2f} 消息/秒", sentCount.get() * 1000.0 / duration);
        logger.info("平均延迟: {:.2f} ms/消息", (double) duration / sentCount.get());
    }

    @Test
    public void benchmarkProducerLatency() throws InterruptedException {
        int messageCount = 100;
        long[] latencies = new long[messageCount];
        
        logger.info("开始Kafka生产者延迟基准测试");
        logger.info("消息数: {}", messageCount);
        
        for (int i = 0; i < messageCount; i++) {
            ChatMessage message = createBenchmarkMessage(i);
            
            long startTime = System.nanoTime();
            chatMessageProducer.sendMessage(message);
            // 注意：这里测量的是发送到KafkaTemplate的时间，不是端到端时间
            long endTime = System.nanoTime();
            
            latencies[i] = (endTime - startTime) / 1_000_000; // 转换为毫秒
            
            if ((i + 1) % 20 == 0) {
                logger.info("已测试 {} 条消息的延迟", i + 1);
            }
            
            // 添加小延迟避免过快发送
            Thread.sleep(10);
        }
        
        // 计算统计信息
        long totalLatency = 0;
        long minLatency = Long.MAX_VALUE;
        long maxLatency = Long.MIN_VALUE;
        
        for (long latency : latencies) {
            totalLatency += latency;
            minLatency = Math.min(minLatency, latency);
            maxLatency = Math.max(maxLatency, latency);
        }
        
        double avgLatency = (double) totalLatency / messageCount;
        
        // 计算95百分位延迟
        java.util.Arrays.sort(latencies);
        long p95Index = Math.round(messageCount * 0.95) - 1;
        long p95Latency = latencies[(int) p95Index];
        
        logger.info("延迟基准测试完成");
        logger.info("平均延迟: {:.2f} ms", avgLatency);
        logger.info("最小延迟: {} ms", minLatency);
        logger.info("最大延迟: {} ms", maxLatency);
        logger.info("95百分位延迟: {} ms", p95Latency);
    }

    @Test
    public void benchmarkDifferentMessageSizes() throws InterruptedException {
        int[] messageSizes = {100, 500, 1000, 5000, 10000}; // 字节
        int messagesPerSize = 50;
        
        logger.info("开始不同消息大小的基准测试");
        
        for (int size : messageSizes) {
            logger.info("测试消息大小: {} 字节", size);
            
            long startTime = System.currentTimeMillis();
            
            for (int i = 0; i < messagesPerSize; i++) {
                ChatMessage message = createBenchmarkMessageWithSize(i, size);
                chatMessageProducer.sendMessage(message);
            }
            
            // 等待一段时间确保消息发送完成
            Thread.sleep(1000);
            
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;
            
            double throughputMsgs = messagesPerSize * 1000.0 / duration;
            double throughputMB = (messagesPerSize * size * 1000.0) / (duration * 1024 * 1024);
            
            logger.info("  发送 {} 条 {} 字节消息耗时: {} ms", messagesPerSize, size, duration);
            logger.info("  消息吞吐量: {:.2f} 消息/秒", throughputMsgs);
            logger.info("  数据吞吐量: {:.2f} MB/秒", throughputMB);
            logger.info("  平均每字节处理时间: {:.6f} ms", (double) duration / (messagesPerSize * size));
            logger.info("");
        }
    }

    @Test
    public void stressTestProducer() throws InterruptedException {
        int duration = 30; // 秒
        int threadCount = 20;
        
        logger.info("开始Kafka生产者压力测试");
        logger.info("测试时长: {} 秒, 线程数: {}", duration, threadCount);
        
        AtomicLong messagesSent = new AtomicLong(0);
        AtomicLong errors = new AtomicLong(0);
        boolean[] running = {true};
        
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        
        long startTime = System.currentTimeMillis();
        
        // 启动发送线程
        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            executor.submit(() -> {
                int messageIndex = 0;
                while (running[0]) {
                    try {
                        ChatMessage message = createBenchmarkMessage(threadId * 10000 + messageIndex);
                        chatMessageProducer.sendMessage(message);
                        messagesSent.incrementAndGet();
                        messageIndex++;
                        
                        // 稍微控制发送速度
                        Thread.sleep(1);
                    } catch (Exception e) {
                        errors.incrementAndGet();
                        logger.warn("发送消息出错: {}", e.getMessage());
                    }
                }
            });
        }
        
        // 定期报告进度
        for (int i = 0; i < duration; i++) {
            Thread.sleep(1000);
            long currentSent = messagesSent.get();
            long currentErrors = errors.get();
            logger.info("第 {} 秒: 已发送 {} 条消息, 错误 {} 次, 当前速率: {:.2f} 消息/秒", 
                       i + 1, currentSent, currentErrors, currentSent * 1000.0 / ((i + 1) * 1000));
        }
        
        running[0] = false;
        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);
        
        long endTime = System.currentTimeMillis();
        long totalDuration = endTime - startTime;
        
        logger.info("压力测试完成");
        logger.info("总发送消息数: {}", messagesSent.get());
        logger.info("总错误数: {}", errors.get());
        logger.info("实际测试时长: {} ms", totalDuration);
        logger.info("平均吞吐量: {:.2f} 消息/秒", messagesSent.get() * 1000.0 / totalDuration);
        logger.info("错误率: {:.2f}%", errors.get() * 100.0 / messagesSent.get());
    }

    private ChatMessage createBenchmarkMessage(int index) {
        ChatMessage message = new ChatMessage();
        message.setId(UUID.randomUUID().toString());
        message.setContent("基准测试消息 #" + index + " - " + System.currentTimeMillis());
        message.setSender("benchmarkSender" + (index % 5));
        message.setReceiver("benchmarkReceiver" + (index % 3));
        message.setTimestamp(LocalDateTime.now());
        return message;
    }

    private ChatMessage createBenchmarkMessageWithSize(int index, int targetSize) {
        ChatMessage message = new ChatMessage();
        message.setId(UUID.randomUUID().toString());
        
        StringBuilder content = new StringBuilder();
        String pattern = "基准测试消息 #" + index + " 数据填充 ";
        
        while (content.length() < targetSize) {
            content.append(pattern);
        }
        
        if (content.length() > targetSize) {
            content.setLength(targetSize);
        }
        
        message.setContent(content.toString());
        message.setSender("benchmarkSender" + (index % 5));
        message.setReceiver("benchmarkReceiver" + (index % 3));
        message.setTimestamp(LocalDateTime.now());
        return message;
    }
}
