package com.im.test;

import com.im.dto.ChatMessage;
import com.im.service.ChatMessageProducer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/test/partition")
public class PartitionTestController {

    @Autowired
    private ChatMessageProducer chatMessageProducer;

    /**
     * 测试默认分区策略（轮询）
     */
    @PostMapping("/default")
    public String testDefaultPartition(@RequestParam String content) {
        ChatMessage message = createTestMessage(content, "user1", "user2");
        chatMessageProducer.sendMessage(message);
        return "Message sent with default partitioning";
    }

    /**
     * 测试基于key的分区
     */
    @PostMapping("/with-key")
    public String testPartitionWithKey(@RequestParam String key, @RequestParam String content) {
        ChatMessage message = createTestMessage(content, "user1", "user2");
        chatMessageProducer.sendMessageWithKey(key, message);
        return "Message sent with key: " + key;
    }

    /**
     * 测试指定分区
     */
    @PostMapping("/specific-partition")
    public String testSpecificPartition(@RequestParam int partition, @RequestParam String content) {
        ChatMessage message = createTestMessage(content, "user1", "user2");
        chatMessageProducer.sendMessageToPartition(partition, "test-key", message);
        return "Message sent to partition: " + partition;
    }

    /**
     * 测试业务逻辑分区（基于对话）
     */
    @PostMapping("/business-partition")
    public String testBusinessPartition(@RequestParam String sender, 
                                      @RequestParam String receiver, 
                                      @RequestParam String content) {
        ChatMessage message = createTestMessage(content, sender, receiver);
        chatMessageProducer.sendMessageWithBusinessPartition(message);
        return String.format("Message sent with business partitioning from %s to %s", sender, receiver);
    }

    /**
     * 获取分区数量
     */
    @GetMapping("/partition-count")
    public String getPartitionCount() {
        int count = chatMessageProducer.getPartitionCount();
        return "Partition count: " + count;
    }

    /**
     * 批量测试分区分布
     */
    @PostMapping("/batch-test")
    public String batchTest(@RequestParam(defaultValue = "10") int messageCount) {
        for (int i = 0; i < messageCount; i++) {
            ChatMessage message = createTestMessage("Batch message " + i, "user" + (i % 3), "user" + ((i + 1) % 3));
            
            // 测试不同的分区策略
            if (i % 3 == 0) {
                // 默认分区
                chatMessageProducer.sendMessage(message);
            } else if (i % 3 == 1) {
                // 基于key的分区
                chatMessageProducer.sendMessageWithKey("test-key-" + (i % 2), message);
            } else {
                // 业务逻辑分区
                chatMessageProducer.sendMessageWithBusinessPartition(message);
            }
        }
        return "Sent " + messageCount + " messages with different partitioning strategies";
    }

    private ChatMessage createTestMessage(String content, String sender, String receiver) {
        ChatMessage message = new ChatMessage();
        message.setId(UUID.randomUUID().toString());
        message.setContent(content);
        message.setSender(sender);
        message.setReceiver(receiver);
        message.setTimestamp(LocalDateTime.now());
        return message;
    }
}
