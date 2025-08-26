package com.im;

import com.im.dto.ChatMessage;
import com.im.service.ChatMessageProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Profile;

import java.time.LocalDateTime;
import java.util.Scanner;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

@SpringBootApplication
@Profile("test")
public class KafkaTestRunner implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(KafkaTestRunner.class);

    @Autowired
    private ChatMessageProducer chatMessageProducer;

    public static void main(String[] args) {
        System.setProperty("spring.profiles.active", "test");
        SpringApplication.run(KafkaTestRunner.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        logger.info("Kafka测试工具启动");
        logger.info("=".repeat(50));
        
        Scanner scanner = new Scanner(System.in);
        
        while (true) {
            showMenu();
            String choice = scanner.nextLine().trim();
            
            try {
                switch (choice) {
                    case "1":
                        testSingleMessage();
                        break;
                    case "2":
                        testBatchMessages();
                        break;
                    case "3":
                        testHighThroughput();
                        break;
                    case "4":
                        testLargeMessages();
                        break;
                    case "5":
                        testConcurrentProducers();
                        break;
                    case "6":
                        runCustomTest(scanner);
                        break;
                    case "0":
                        logger.info("退出测试程序");
                        return;
                    default:
                        logger.warn("无效选择，请重新输入");
                }
            } catch (Exception e) {
                logger.error("执行测试时出错: {}", e.getMessage(), e);
            }
            
            logger.info("\n" + "=".repeat(50) + "\n");
        }
    }

    private void showMenu() {
        System.out.println("请选择要执行的测试:");
        System.out.println("1. 单条消息测试");
        System.out.println("2. 批量消息测试 (100条)");
        System.out.println("3. 高吞吐量测试 (1000条)");
        System.out.println("4. 大消息测试");
        System.out.println("5. 并发生产者测试");
        System.out.println("6. 自定义测试");
        System.out.println("0. 退出");
        System.out.print("请输入选择 (0-6): ");
    }

    private void testSingleMessage() {
        logger.info("开始单条消息测试...");
        
        ChatMessage message = createTestMessage("单条消息测试 - " + System.currentTimeMillis());
        
        long startTime = System.currentTimeMillis();
        chatMessageProducer.sendMessage(message);
        long endTime = System.currentTimeMillis();
        
        logger.info("单条消息发送完成");
        logger.info("消息ID: {}", message.getId());
        logger.info("消息内容: {}", message.getContent());
        logger.info("发送耗时: {} ms", endTime - startTime);
    }

    private void testBatchMessages() throws InterruptedException {
        int messageCount = 100;
        logger.info("开始批量消息测试，发送 {} 条消息...", messageCount);
        
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < messageCount; i++) {
            ChatMessage message = createTestMessage("批量消息 #" + (i + 1));
            chatMessageProducer.sendMessage(message);
            
            if ((i + 1) % 20 == 0) {
                logger.info("已发送 {} 条消息", i + 1);
            }
        }
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        logger.info("批量消息发送完成");
        logger.info("发送消息数: {}", messageCount);
        logger.info("总耗时: {} ms", duration);
        logger.info("平均吞吐量: {:.2f} 消息/秒", messageCount * 1000.0 / duration);
        logger.info("平均每条消息耗时: {:.2f} ms", (double) duration / messageCount);
    }

    private void testHighThroughput() throws InterruptedException {
        int messageCount = 1000;
        int threadCount = 10;
        
        logger.info("开始高吞吐量测试，使用 {} 个线程发送 {} 条消息...", threadCount, messageCount);
        
        AtomicLong sentCount = new AtomicLong(0);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < messageCount; i++) {
            final int messageIndex = i;
            executor.submit(() -> {
                ChatMessage message = createTestMessage("高吞吐量测试消息 #" + messageIndex);
                chatMessageProducer.sendMessage(message);
                long count = sentCount.incrementAndGet();
                
                if (count % 100 == 0) {
                    logger.info("已发送 {} 条消息", count);
                }
            });
        }
        
        executor.shutdown();
        boolean finished = executor.awaitTermination(60, TimeUnit.SECONDS);
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        logger.info("高吞吐量测试完成");
        logger.info("是否正常完成: {}", finished);
        logger.info("实际发送消息数: {}", sentCount.get());
        logger.info("总耗时: {} ms", duration);
        logger.info("平均吞吐量: {:.2f} 消息/秒", sentCount.get() * 1000.0 / duration);
    }

    private void testLargeMessages() throws InterruptedException {
        logger.info("开始大消息测试...");
        
        // 创建不同大小的消息进行测试
        int[] sizes = {1024, 10240, 102400}; // 1KB, 10KB, 100KB
        
        for (int size : sizes) {
            logger.info("测试 {} KB 大小的消息", size / 1024);
            
            StringBuilder content = new StringBuilder();
            String pattern = "大消息测试数据 ";
            while (content.length() < size) {
                content.append(pattern);
            }
            content.setLength(size); // 精确控制大小
            
            ChatMessage message = createTestMessage(content.toString());
            
            long startTime = System.currentTimeMillis();
            chatMessageProducer.sendMessage(message);
            long endTime = System.currentTimeMillis();
            
            double sizeInMB = size / (1024.0 * 1024.0);
            long duration = endTime - startTime;
            
            logger.info("  消息大小: {} KB", size / 1024);
            logger.info("  发送耗时: {} ms", duration);
            logger.info("  传输速率: {:.2f} MB/秒", sizeInMB * 1000.0 / duration);
            
            Thread.sleep(500); // 间隔一下
        }
    }

    private void testConcurrentProducers() throws InterruptedException {
        int producerCount = 5;
        int messagesPerProducer = 50;
        
        logger.info("开始并发生产者测试，{} 个生产者，每个发送 {} 条消息...", 
                   producerCount, messagesPerProducer);
        
        ExecutorService executor = Executors.newFixedThreadPool(producerCount);
        AtomicLong totalSent = new AtomicLong(0);
        
        long startTime = System.currentTimeMillis();
        
        for (int producerId = 0; producerId < producerCount; producerId++) {
            final int pid = producerId;
            executor.submit(() -> {
                for (int i = 0; i < messagesPerProducer; i++) {
                    ChatMessage message = createTestMessage(
                        String.format("生产者%d 消息#%d", pid, i + 1));
                    message.setSender("Producer" + pid);
                    
                    chatMessageProducer.sendMessage(message);
                    long count = totalSent.incrementAndGet();
                    
                    if (count % 25 == 0) {
                        logger.info("总共已发送 {} 条消息", count);
                    }
                }
                logger.info("生产者 {} 完成发送", pid);
            });
        }
        
        executor.shutdown();
        boolean finished = executor.awaitTermination(30, TimeUnit.SECONDS);
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        long totalMessages = producerCount * messagesPerProducer;
        
        logger.info("并发生产者测试完成");
        logger.info("是否正常完成: {}", finished);
        logger.info("预期消息数: {}", totalMessages);
        logger.info("实际发送消息数: {}", totalSent.get());
        logger.info("总耗时: {} ms", duration);
        logger.info("平均吞吐量: {:.2f} 消息/秒", totalSent.get() * 1000.0 / duration);
    }

    private void runCustomTest(Scanner scanner) throws InterruptedException {
        System.out.print("请输入要发送的消息数量: ");
        int messageCount = Integer.parseInt(scanner.nextLine().trim());
        
        System.out.print("请输入并发线程数 (1-20): ");
        int threadCount = Math.min(20, Math.max(1, Integer.parseInt(scanner.nextLine().trim())));
        
        logger.info("开始自定义测试: {} 条消息, {} 个线程", messageCount, threadCount);
        
        AtomicLong sentCount = new AtomicLong(0);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < messageCount; i++) {
            final int messageIndex = i;
            executor.submit(() -> {
                ChatMessage message = createTestMessage("自定义测试消息 #" + messageIndex);
                chatMessageProducer.sendMessage(message);
                long count = sentCount.incrementAndGet();
                
                if (count % Math.max(1, messageCount / 10) == 0) {
                    logger.info("进度: {}/{} ({:.1f}%)", count, messageCount, 
                               count * 100.0 / messageCount);
                }
            });
        }
        
        executor.shutdown();
        boolean finished = executor.awaitTermination(120, TimeUnit.SECONDS);
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        logger.info("自定义测试完成");
        logger.info("是否正常完成: {}", finished);
        logger.info("实际发送消息数: {}", sentCount.get());
        logger.info("总耗时: {} ms", duration);
        logger.info("平均吞吐量: {:.2f} 消息/秒", sentCount.get() * 1000.0 / duration);
        logger.info("平均延迟: {:.2f} ms/消息", (double) duration / sentCount.get());
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
