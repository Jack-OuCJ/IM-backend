package com.im.test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.im.dto.ChatMessage;
import org.apache.kafka.clients.producer.*;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.serialization.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 手动Kafka性能测试工具
 * 可以直接运行测试Kafka的生产消费性能
 */
public class ManualKafkaTest {

    private static final String BOOTSTRAP_SERVERS = "localhost:29092,localhost:29093,localhost:29094";
    private static final String TOPIC_NAME = "chat-message-topic";
    
    public static void main(String[] args) {
        ManualKafkaTest test = new ManualKafkaTest();
        
        System.out.println("=".repeat(60));
        System.out.println("Kafka 性能测试工具");
        System.out.println("=".repeat(60));
        
        Scanner scanner = new Scanner(System.in);
        
        while (true) {
            showMenu();
            String choice = scanner.nextLine().trim();
            
            try {
                switch (choice) {
                    case "1":
                        test.testProducerThroughput();
                        break;
                    case "2":
                        test.testProducerLatency();
                        break;
                    case "3":
                        test.testConcurrentProducers();
                        break;
                    case "4":
                        test.testDifferentMessageSizes();
                        break;
                    case "5":
                        test.testEndToEndPerformance();
                        break;
                    case "6":
                        test.runCustomTest(scanner);
                        break;
                    case "0":
                        System.out.println("退出测试");
                        return;
                    default:
                        System.out.println("无效选择，请重新输入");
                }
            } catch (Exception e) {
                System.err.println("测试执行出错: " + e.getMessage());
                e.printStackTrace();
            }
            
            System.out.println("\n" + "=".repeat(60) + "\n");
        }
    }
    
    private static void showMenu() {
        System.out.println("请选择测试类型:");
        System.out.println("1. 生产者吞吐量测试");
        System.out.println("2. 生产者延迟测试");
        System.out.println("3. 并发生产者测试");
        System.out.println("4. 不同消息大小测试");
        System.out.println("5. 端到端性能测试");
        System.out.println("6. 自定义测试");
        System.out.println("0. 退出");
        System.out.print("请输入选择 (0-6): ");
    }
    
    public void testProducerThroughput() {
        int messageCount = 1000;
        System.out.println("开始生产者吞吐量测试，发送 " + messageCount + " 条消息...");
        
        Producer<String, String> producer = createStringProducer();
        
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < messageCount; i++) {
            String message = createJsonMessage(i);
            ProducerRecord<String, String> record = 
                new ProducerRecord<>(TOPIC_NAME, "key-" + i, message);
            
            producer.send(record);
            
            if ((i + 1) % 100 == 0) {
                System.out.println("已发送 " + (i + 1) + " 条消息");
            }
        }
        
        producer.flush();
        producer.close();
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        System.out.println("生产者吞吐量测试完成");
        System.out.println("发送消息数: " + messageCount);
        System.out.println("总耗时: " + duration + " ms");
        System.out.println("平均吞吐量: " + String.format("%.2f", messageCount * 1000.0 / duration) + " 消息/秒");
        System.out.println("平均延迟: " + String.format("%.2f", (double) duration / messageCount) + " ms/消息");
    }
    
    public void testProducerLatency() {
        int messageCount = 100;
        System.out.println("开始生产者延迟测试，发送 " + messageCount + " 条消息...");
        
        Producer<String, String> producer = createStringProducer();
        long[] latencies = new long[messageCount];
        
        for (int i = 0; i < messageCount; i++) {
            String message = createJsonMessage(i);
            ProducerRecord<String, String> record = 
                new ProducerRecord<>(TOPIC_NAME, "key-" + i, message);
            
            long startTime = System.nanoTime();
            producer.send(record);
            long endTime = System.nanoTime();
            
            latencies[i] = (endTime - startTime) / 1_000_000; // 转换为毫秒
            
            if ((i + 1) % 20 == 0) {
                System.out.println("已测试 " + (i + 1) + " 条消息的延迟");
            }
            
            try {
                Thread.sleep(10); // 避免发送过快
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        
        producer.flush();
        producer.close();
        
        // 计算统计信息
        Arrays.sort(latencies);
        long minLatency = latencies[0];
        long maxLatency = latencies[messageCount - 1];
        long avgLatency = Arrays.stream(latencies).sum() / messageCount;
        long p95Index = Math.round(messageCount * 0.95) - 1;
        long p95Latency = latencies[(int) p95Index];
        
        System.out.println("延迟测试完成");
        System.out.println("平均延迟: " + avgLatency + " ms");
        System.out.println("最小延迟: " + minLatency + " ms");
        System.out.println("最大延迟: " + maxLatency + " ms");
        System.out.println("95百分位延迟: " + p95Latency + " ms");
    }
    
    public void testConcurrentProducers() throws InterruptedException {
        int threadCount = 5;
        int messagesPerThread = 100;
        int totalMessages = threadCount * messagesPerThread;
        
        System.out.println("开始并发生产者测试，" + threadCount + " 个线程，每个发送 " + messagesPerThread + " 条消息...");
        
        AtomicLong sentCount = new AtomicLong(0);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        
        long startTime = System.currentTimeMillis();
        
        for (int t = 0; t < threadCount; t++) {
            final int threadId = t;
            executor.submit(() -> {
                Producer<String, String> producer = createStringProducer();
                
                try {
                    for (int i = 0; i < messagesPerThread; i++) {
                        String message = createJsonMessage(threadId * messagesPerThread + i);
                        ProducerRecord<String, String> record = 
                            new ProducerRecord<>(TOPIC_NAME, "thread-" + threadId + "-key-" + i, message);
                        
                        producer.send(record);
                        sentCount.incrementAndGet();
                    }
                    producer.flush();
                } finally {
                    producer.close();
                    latch.countDown();
                }
            });
        }
        
        boolean finished = latch.await(30, TimeUnit.SECONDS);
        executor.shutdown();
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        System.out.println("并发生产者测试完成");
        System.out.println("是否正常完成: " + finished);
        System.out.println("预期消息数: " + totalMessages);
        System.out.println("实际发送消息数: " + sentCount.get());
        System.out.println("总耗时: " + duration + " ms");
        System.out.println("平均吞吐量: " + String.format("%.2f", sentCount.get() * 1000.0 / duration) + " 消息/秒");
    }
    
    public void testDifferentMessageSizes() {
        int[] messageSizes = {100, 1000, 10000, 100000}; // 字节
        int messagesPerSize = 50;
        
        System.out.println("开始不同消息大小性能测试");
        
        for (int size : messageSizes) {
            System.out.println("测试消息大小: " + size + " 字节");
            
            Producer<String, String> producer = createStringProducer();
            
            long startTime = System.currentTimeMillis();
            
            for (int i = 0; i < messagesPerSize; i++) {
                String message = createJsonMessageWithSize(i, size);
                ProducerRecord<String, String> record = 
                    new ProducerRecord<>(TOPIC_NAME, "size-" + size + "-key-" + i, message);
                
                producer.send(record);
            }
            
            producer.flush();
            producer.close();
            
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;
            
            double throughputMsgs = messagesPerSize * 1000.0 / duration;
            double throughputMB = (messagesPerSize * size * 1000.0) / (duration * 1024 * 1024);
            
            System.out.println("  发送 " + messagesPerSize + " 条 " + size + " 字节消息耗时: " + duration + " ms");
            System.out.println("  消息吞吐量: " + String.format("%.2f", throughputMsgs) + " 消息/秒");
            System.out.println("  数据吞吐量: " + String.format("%.2f", throughputMB) + " MB/秒");
            System.out.println();
        }
    }
    
    public void testEndToEndPerformance() throws InterruptedException {
        int messageCount = 200;
        CountDownLatch latch = new CountDownLatch(messageCount);
        AtomicLong consumedCount = new AtomicLong(0);
        
        System.out.println("开始端到端性能测试，发送和接收 " + messageCount + " 条消息...");
        
        // 创建消费者
        Consumer<String, String> consumer = createStringConsumer();
        consumer.subscribe(Collections.singletonList(TOPIC_NAME));
        
        // 启动消费者线程
        ExecutorService consumerExecutor = Executors.newSingleThreadExecutor();
        consumerExecutor.submit(() -> {
            try {
                while (consumedCount.get() < messageCount) {
                    ConsumerRecords<String, String> records = consumer.poll(java.time.Duration.ofMillis(1000));
                    for (ConsumerRecord<String, String> record : records) {
                        long count = consumedCount.incrementAndGet();
                        latch.countDown();
                        
                        if (count % 40 == 0) {
                            System.out.println("已消费 " + count + " 条消息");
                        }
                    }
                    consumer.commitSync();
                }
            } catch (Exception e) {
                System.err.println("消费消息时出错: " + e.getMessage());
            } finally {
                consumer.close();
            }
        });
        
        // 等待消费者准备好
        Thread.sleep(3000);
        
        // 创建生产者并发送消息
        Producer<String, String> producer = createStringProducer();
        
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < messageCount; i++) {
            String message = createJsonMessage(i);
            ProducerRecord<String, String> record = 
                new ProducerRecord<>(TOPIC_NAME, "e2e-key-" + i, message);
            
            producer.send(record);
            
            if ((i + 1) % 40 == 0) {
                System.out.println("已发送 " + (i + 1) + " 条消息");
            }
        }
        
        producer.flush();
        producer.close();
        
        // 等待所有消息被消费
        boolean allConsumed = latch.await(60, TimeUnit.SECONDS);
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        consumerExecutor.shutdown();
        
        System.out.println("端到端测试完成");
        System.out.println("是否全部消费: " + allConsumed);
        System.out.println("发送消息数: " + messageCount);
        System.out.println("消费消息数: " + consumedCount.get());
        System.out.println("总耗时: " + duration + " ms");
        System.out.println("端到端吞吐量: " + String.format("%.2f", consumedCount.get() * 1000.0 / duration) + " 消息/秒");
    }
    
    public void runCustomTest(Scanner scanner) throws InterruptedException {
        System.out.print("请输入要发送的消息数量: ");
        int messageCount = Integer.parseInt(scanner.nextLine().trim());
        
        System.out.print("请输入并发线程数 (1-20): ");
        int threadCount = Math.min(20, Math.max(1, Integer.parseInt(scanner.nextLine().trim())));
        
        System.out.println("开始自定义测试: " + messageCount + " 条消息, " + threadCount + " 个线程");
        
        AtomicLong sentCount = new AtomicLong(0);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        
        long startTime = System.currentTimeMillis();
        
        int messagesPerThread = messageCount / threadCount;
        int remainder = messageCount % threadCount;
        
        for (int t = 0; t < threadCount; t++) {
            final int threadId = t;
            final int messagesToSend = messagesPerThread + (t < remainder ? 1 : 0);
            
            executor.submit(() -> {
                Producer<String, String> producer = createStringProducer();
                
                try {
                    for (int i = 0; i < messagesToSend; i++) {
                        String message = createJsonMessage(threadId * messagesPerThread + i);
                        ProducerRecord<String, String> record = 
                            new ProducerRecord<>(TOPIC_NAME, "custom-thread-" + threadId + "-key-" + i, message);
                        
                        producer.send(record);
                        long count = sentCount.incrementAndGet();
                        
                        if (count % Math.max(1, messageCount / 10) == 0) {
                            System.out.println("进度: " + count + "/" + messageCount + 
                                             " (" + String.format("%.1f", count * 100.0 / messageCount) + "%)");
                        }
                    }
                    producer.flush();
                } finally {
                    producer.close();
                    latch.countDown();
                }
            });
        }
        
        boolean finished = latch.await(120, TimeUnit.SECONDS);
        executor.shutdown();
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        System.out.println("自定义测试完成");
        System.out.println("是否正常完成: " + finished);
        System.out.println("实际发送消息数: " + sentCount.get());
        System.out.println("总耗时: " + duration + " ms");
        System.out.println("平均吞吐量: " + String.format("%.2f", sentCount.get() * 1000.0 / duration) + " 消息/秒");
        System.out.println("平均延迟: " + String.format("%.2f", (double) duration / sentCount.get()) + " ms/消息");
    }
    
    private Producer<String, String> createStringProducer() {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.RETRIES_CONFIG, 3);
        props.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384);
        props.put(ProducerConfig.LINGER_MS_CONFIG, 1);
        props.put(ProducerConfig.BUFFER_MEMORY_CONFIG, 33554432);
        
        return new KafkaProducer<>(props);
    }
    
    private Consumer<String, String> createStringConsumer() {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "manual-test-group-" + UUID.randomUUID());
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        
        return new KafkaConsumer<>(props);
    }
    
    private String createJsonMessage(int index) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(new JavaTimeModule());
            
            ChatMessage message = new ChatMessage();
            message.setId(UUID.randomUUID().toString());
            message.setContent("测试消息 #" + index + " - " + System.currentTimeMillis());
            message.setSender("testSender" + (index % 5));
            message.setReceiver("testReceiver" + (index % 3));
            message.setTimestamp(LocalDateTime.now());
            
            return mapper.writeValueAsString(message);
        } catch (Exception e) {
            return "{\"id\":\"" + UUID.randomUUID() + "\",\"content\":\"fallback message " + index + "\"}";
        }
    }
    
    private String createJsonMessageWithSize(int index, int targetSize) {
        StringBuilder content = new StringBuilder();
        String pattern = "测试数据填充 ";
        
        while (content.length() < targetSize - 200) { // 留出JSON其他字段的空间
            content.append(pattern);
        }
        
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(new JavaTimeModule());
            
            ChatMessage message = new ChatMessage();
            message.setId(UUID.randomUUID().toString());
            message.setContent(content.toString());
            message.setSender("testSender" + (index % 5));
            message.setReceiver("testReceiver" + (index % 3));
            message.setTimestamp(LocalDateTime.now());
            
            return mapper.writeValueAsString(message);
        } catch (Exception e) {
            return "{\"id\":\"" + UUID.randomUUID() + "\",\"content\":\"" + content.toString() + "\"}";
        }
    }
}
