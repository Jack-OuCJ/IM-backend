package com.im.redis;

import com.im.redis.service.*;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.ZSetOperations;

import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive Redis Demo Tests
 * Click ▶️ next to each test method to run individual demos
 */
@Slf4j
@SpringBootTest
@DisplayName("Redis Best Practices Demo Tests")
class RedisDemoTests {

    @Autowired
    private RedisBasicService redisBasicService;

    @Autowired
    private RedisHashService redisHashService;

    @Autowired
    private RedisListService redisListService;

    @Autowired
    private RedisSetService redisSetService;

    @Autowired
    private RedisZSetService redisZSetService;

    @Autowired
    private RedisPipelineService redisPipelineService;

    @Autowired
    private RedisTransactionService redisTransactionService;

    @Autowired
    private RedisDistributedLockService redisDistributedLockService;

    @Autowired
    private RedisPubSubService redisPubSubService;

    @Test
    @DisplayName("1. Basic String Operations - SET/GET/EXPIRE/DEL")
    void testBasicStringOperations() {
        log.info("=== Testing Basic String Operations ===");

        // Test SET and GET
        redisBasicService.setValue("user", "profile", "user123", "John Doe");
        String value = redisBasicService.getValue("user", "profile", "user123");
        assertEquals("John Doe", value);

        // Test TTL
        long ttl = redisBasicService.getTtl("user", "profile", "user123");
        assertTrue(ttl > 0 && ttl <= 720); // Should be within jitter range

        // Test key exists
        assertTrue(redisBasicService.hasKey("user", "profile", "user123"));

        // Test SET if not exists
        boolean setResult = redisBasicService.setIfAbsent("user", "profile", "user123", "Jane Doe");
        assertFalse(setResult); // Should fail because key exists

        // Test increment
        long counter = redisBasicService.increment("system", "counter", "login_count");
        assertTrue(counter >= 1);

        // Test custom TTL
        redisBasicService.setValue("temp", "data", "temp123", "temporary", 120);
        
        // Test delete
        boolean deleted = redisBasicService.deleteKey("temp", "data", "temp123");
        assertTrue(deleted);

        log.info("✅ Basic String Operations test completed successfully");
    }

    @Test
    @DisplayName("2. Hash Operations - Field Management")
    void testHashOperations() {
        log.info("=== Testing Hash Operations ===");

        String userId = "user456";

        // Set individual hash fields
        redisHashService.setHashField("user", "profile", userId, "name", "Alice Smith");
        redisHashService.setHashField("user", "profile", userId, "email", "alice@example.com");
        redisHashService.setHashField("user", "profile", userId, "age", "28");

        // Get individual field
        String name = redisHashService.getHashField("user", "profile", userId, "name");
        assertEquals("Alice Smith", name);

        // Set multiple fields
        Map<String, String> fields = new HashMap<>();
        fields.put("city", "New York");
        fields.put("country", "USA");
        fields.put("phone", "+1-555-0123");
        redisHashService.setHashFields("user", "profile", userId, fields);

        // Get all fields
        Map<Object, Object> allFields = redisHashService.getAllHashFields("user", "profile", userId);
        assertEquals(6, allFields.size());

        // Check field existence
        assertTrue(redisHashService.hasHashField("user", "profile", userId, "email"));
        assertFalse(redisHashService.hasHashField("user", "profile", userId, "salary"));

        // Increment numeric field
        long newAge = redisHashService.incrementHashField("user", "profile", userId, "age", 1);
        assertEquals(29, newAge);

        // Get hash size
        long size = redisHashService.getHashSize("user", "profile", userId);
        assertEquals(6, size);

        // Delete field
        boolean deleted = redisHashService.deleteHashField("user", "profile", userId, "phone");
        assertTrue(deleted);

        log.info("✅ Hash Operations test completed successfully");
    }

    @Test
    @DisplayName("3. List Operations - Message Queue & Chat History")
    void testListOperations() {
        log.info("=== Testing List Operations ===");

        String chatId = "chat789";

        // Push messages to chat
        redisListService.rightPush("chat", "messages", chatId, "Hello!");
        redisListService.rightPush("chat", "messages", chatId, "How are you?");
        redisListService.rightPush("chat", "messages", chatId, "I'm fine, thanks!");

        // Get list size
        long size = redisListService.getSize("chat", "messages", chatId);
        assertEquals(3, size);

        // Get all messages
        List<String> allMessages = redisListService.getAllElements("chat", "messages", chatId);
        assertEquals(3, allMessages.size());
        assertEquals("Hello!", allMessages.get(0));

        // Get message by index
        String firstMessage = redisListService.getByIndex("chat", "messages", chatId, 0);
        assertEquals("Hello!", firstMessage);

        // Get range
        List<String> lastTwoMessages = redisListService.getRange("chat", "messages", chatId, -2, -1);
        assertEquals(2, lastTwoMessages.size());

        // Push multiple messages
        redisListService.rightPushAll("chat", "messages", chatId, "Message 4", "Message 5", "Message 6");

        // Keep only latest 3 messages (simulate chat history limit)
        redisListService.keepLatest("chat", "messages", chatId, 3);
        long newSize = redisListService.getSize("chat", "messages", chatId);
        assertEquals(3, newSize);

        // Pop message (message consumption)
        String poppedMessage = redisListService.leftPop("chat", "messages", chatId);
        assertNotNull(poppedMessage);

        log.info("✅ List Operations test completed successfully");
    }

    @Test
    @DisplayName("4. Set Operations - Unique Collections & Relationships")
    void testSetOperations() {
        log.info("=== Testing Set Operations ===");

        // Online users set
        redisSetService.addMember("system", "online", "users", "user1");
        redisSetService.addMember("system", "online", "users", "user2");
        redisSetService.addMember("system", "online", "users", "user3");

        // Friends set
        redisSetService.addMembers("user", "friends", "user1", "user2", "user4", "user5");
        redisSetService.addMembers("user", "friends", "user2", "user1", "user3", "user6");

        // Check membership
        assertTrue(redisSetService.isMember("system", "online", "users", "user1"));
        assertFalse(redisSetService.isMember("system", "online", "users", "user4"));

        // Get all members
        Set<String> onlineUsers = redisSetService.getAllMembers("system", "online", "users");
        assertEquals(3, onlineUsers.size());

        // Get random member
        String randomUser = redisSetService.getRandomMember("system", "online", "users");
        assertNotNull(randomUser);

        // Set operations - intersection (mutual friends)
        Set<String> mutualFriends = redisSetService.getIntersection(
            "user", "friends", "user1",
            "user", "friends", "user2"
        );
        assertTrue(mutualFriends.contains("user1") || mutualFriends.contains("user2"));

        // Set operations - union (all friends)
        Set<String> allFriends = redisSetService.getUnion(
            "user", "friends", "user1",
            "user", "friends", "user2"
        );
        assertTrue(allFriends.size() >= 3);

        // Remove member
        long removed = redisSetService.removeMember("system", "online", "users", "user3");
        assertEquals(1, removed);

        // Get set size
        long size = redisSetService.getSize("system", "online", "users");
        assertEquals(2, size);

        log.info("✅ Set Operations test completed successfully");
    }

    @Test
    @DisplayName("5. Sorted Set Operations - Rankings & Leaderboards")
    void testZSetOperations() {
        log.info("=== Testing Sorted Set Operations ===");

        String leaderboard = "global";

        // Add players with scores
        redisZSetService.addMember("ranking", "score", leaderboard, "player1", 1000);
        redisZSetService.addMember("ranking", "score", leaderboard, "player2", 1500);
        redisZSetService.addMember("ranking", "score", leaderboard, "player3", 800);
        redisZSetService.addMember("ranking", "score", leaderboard, "player4", 2000);

        // Add multiple members
        Set<ZSetOperations.TypedTuple<String>> tuples = new HashSet<>();
        tuples.add(ZSetOperations.TypedTuple.of("player5", 1200.0));
        tuples.add(ZSetOperations.TypedTuple.of("player6", 900.0));
        redisZSetService.addMembers("ranking", "score", leaderboard, tuples);

        // Get player score
        Double player2Score = redisZSetService.getScore("ranking", "score", leaderboard, "player2");
        assertEquals(1500.0, player2Score);

        // Get player rank (0-based, lowest score = rank 0)
        Long player4Rank = redisZSetService.getReverseRank("ranking", "score", leaderboard, "player4");
        assertEquals(0L, player4Rank); // Highest score should have rank 0

        // Increment score
        double newScore = redisZSetService.incrementScore("ranking", "score", leaderboard, "player1", 100);
        assertEquals(1100.0, newScore);

        // Get top 3 players
        Set<ZSetOperations.TypedTuple<String>> top3 = redisZSetService.getTopN("ranking", "score", leaderboard, 3);
        assertEquals(3, top3.size());

        // Get players in score range
        Set<String> midRangePlayers = redisZSetService.getRangeByScore("ranking", "score", leaderboard, 1000, 1600);
        assertTrue(midRangePlayers.size() >= 2);

        // Get total players
        long totalPlayers = redisZSetService.getSize("ranking", "score", leaderboard);
        assertEquals(6, totalPlayers);

        // Remove player
        long removed = redisZSetService.removeMember("ranking", "score", leaderboard, "player3");
        assertEquals(1, removed);

        log.info("✅ Sorted Set Operations test completed successfully");
    }

    @Test
    @DisplayName("6. Pipeline Operations - Batch Processing")
    void testPipelineOperations() {
        log.info("=== Testing Pipeline Operations ===");

        // Prepare test data
        Map<String, String> testData = new HashMap<>();
        for (int i = 1; i <= 100; i++) {
            testData.put("im:demo:test:batch:key" + i, "value" + i);
        }

        // Test batch set
        redisPipelineService.batchSetStrings(testData);

        // Test batch get
        List<String> keys = new ArrayList<>(testData.keySet());
        List<Object> values = redisPipelineService.batchGetStrings(keys);
        assertEquals(100, values.size());

        // Test batch check existence
        List<Object> existsResults = redisPipelineService.batchCheckExists(keys);
        assertEquals(100, existsResults.size());

        // Test batch increment
        List<String> counterKeys = Arrays.asList(
            "im:demo:test:counter:1",
            "im:demo:test:counter:2",
            "im:demo:test:counter:3"
        );
        List<Object> incrementResults = redisPipelineService.batchIncrementCounters(counterKeys);
        assertEquals(3, incrementResults.size());

        // Test batch list operations
        Map<String, List<String>> listData = new HashMap<>();
        listData.put("im:demo:test:list:1", Arrays.asList("item1", "item2", "item3"));
        listData.put("im:demo:test:list:2", Arrays.asList("itemA", "itemB"));
        redisPipelineService.batchAddToLists(listData);

        // Test batch set operations
        Map<String, List<String>> setData = new HashMap<>();
        setData.put("im:demo:test:set:1", Arrays.asList("member1", "member2", "member3"));
        setData.put("im:demo:test:set:2", Arrays.asList("memberA", "memberB"));
        redisPipelineService.batchAddToSets(setData);

        // Performance comparison
        Map<String, String> perfTestData = new HashMap<>();
        for (int i = 1; i <= 50; i++) {
            perfTestData.put("im:demo:perf:test:key" + i, "value" + i);
        }
        redisPipelineService.performanceComparison(perfTestData);

        // Cleanup
        List<String> allTestKeys = new ArrayList<>(testData.keySet());
        allTestKeys.addAll(counterKeys);
        allTestKeys.addAll(listData.keySet());
        allTestKeys.addAll(setData.keySet());
        redisPipelineService.batchDeleteKeys(allTestKeys);

        log.info("✅ Pipeline Operations test completed successfully");
    }

    @Test
    @DisplayName("7. Transaction & Optimistic Lock")
    void testTransactionAndOptimisticLock() {
        log.info("=== Testing Transaction & Optimistic Lock ===");

        // Test simple transaction
        List<Object> results = redisTransactionService.executeTransaction(
            "test", "tx", "key1", "value1", "key2", "value2"
        );
        assertNotNull(results);
        assertEquals(4, results.size()); // 2 SET + 2 EXPIRE operations

        // Test optimistic lock for counter increment
        boolean incrementSuccess = redisTransactionService.incrementCounterWithOptimisticLock(
            "test", "counter", "global", 5
        );
        assertTrue(incrementSuccess);

        // Test transfer operation with optimistic lock
        // Setup initial balances
        redisBasicService.setValue("account", "balance", "acc1", "1000");
        redisBasicService.setValue("account", "balance", "acc2", "500");

        boolean transferSuccess = redisTransactionService.transferValue(
            "account", "balance", "acc1",
            "account", "balance", "acc2",
            100, 3
        );
        assertTrue(transferSuccess);

        // Verify balances after transfer
        String acc1Balance = redisBasicService.getValue("account", "balance", "acc1");
        String acc2Balance = redisBasicService.getValue("account", "balance", "acc2");
        assertEquals("900", acc1Balance);
        assertEquals("600", acc2Balance);

        // Test user session update with version control
        String userId = "user999";
        boolean sessionUpdateSuccess = redisTransactionService.updateUserSession(
            userId, "new_session_data", null, 3
        );
        assertTrue(sessionUpdateSuccess);

        // Get session with version
        RedisTransactionService.SessionWithVersion sessionWithVersion = 
            redisTransactionService.getUserSessionWithVersion(userId);
        assertNotNull(sessionWithVersion.sessionData);
        assertNotNull(sessionWithVersion.version);

        // Test concurrent update (should fail)
        boolean concurrentUpdateSuccess = redisTransactionService.updateUserSession(
            userId, "concurrent_session_data", "wrong_version", 3
        );
        assertFalse(concurrentUpdateSuccess);

        log.info("✅ Transaction & Optimistic Lock test completed successfully");
    }

    @Test
    @DisplayName("8. Distributed Lock")
    void testDistributedLock() {
        log.info("=== Testing Distributed Lock ===");

        String resource = "test_resource_001";

        // Test simple lock execution
        String result = redisDistributedLockService.executeWithLock(resource, () -> {
            log.info("Executing critical section with lock");
            try {
                Thread.sleep(100); // Simulate work
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return "work_completed";
        });
        assertEquals("work_completed", result);

        // Test lock status check
        assertFalse(redisDistributedLockService.isLocked(resource));

        // Test try lock (non-blocking)
        boolean acquired = redisDistributedLockService.tryLock(resource, 10, TimeUnit.SECONDS);
        assertTrue(acquired);
        assertTrue(redisDistributedLockService.isLocked(resource));

        // Force unlock for cleanup
        redisDistributedLockService.forceUnlock(resource);

        // Test distributed counter
        int counterValue = redisDistributedLockService.incrementDistributedCounter("test_counter", 3);
        assertTrue(counterValue > 0);

        // Test resource allocation
        boolean allocated = redisDistributedLockService.allocateResource("server", "srv001", "user123");
        // Note: This might be true or false depending on the random simulation

        // Test concurrent access simulation
        CountDownLatch latch = new CountDownLatch(3);
        List<String> executionOrder = Collections.synchronizedList(new ArrayList<>());

        for (int i = 1; i <= 3; i++) {
            final int threadId = i;
            new Thread(() -> {
                try {
                    redisDistributedLockService.executeWithLock("concurrent_test", 2, 5, TimeUnit.SECONDS, () -> {
                        executionOrder.add("Thread-" + threadId);
                        try {
                            Thread.sleep(50);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    });
                } catch (Exception e) {
                    log.warn("Thread {} failed to acquire lock: {}", threadId, e.getMessage());
                } finally {
                    latch.countDown();
                }
            }).start();
        }

        try {
            latch.await(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        log.info("Concurrent execution order: {}", executionOrder);
        // At least one thread should have executed
        assertFalse(executionOrder.isEmpty());

        log.info("✅ Distributed Lock test completed successfully");
    }

    // @Test
    // @DisplayName("9. Pub/Sub Messaging")
    // void testPubSubMessaging() throws InterruptedException {
    //     log.info("=== Testing Pub/Sub Messaging ===");

    //     CountDownLatch messageLatch = new CountDownLatch(3);
    //     List<String> receivedMessages = Collections.synchronizedList(new ArrayList<>());

    //     // Custom message handler
    //     RedisPubSubService.MessageHandler chatHandler = (channel, message) -> {
    //         log.info("📥 Received chat message: {}", message);
    //         receivedMessages.add(message);
    //         messageLatch.countDown();
    //     };

    //     // Subscribe to chat room
    //     String roomId = "room001";
    //     redisPubSubService.subscribeToChatRoom(roomId, chatHandler);

    //     // Wait a bit for subscription to be active
    //     Thread.sleep(100);

    //     // Publish chat messages
    //     redisPubSubService.publishChatMessage(roomId, "user1", "Hello everyone!");
    //     redisPubSubService.publishChatMessage(roomId, "user2", "Hi there!");
    //     redisPubSubService.publishChatMessage(roomId, "user1", "How's everyone doing?");

    //     // Wait for messages to be received
    //     boolean allMessagesReceived = messageLatch.await(5, TimeUnit.SECONDS);
    //     assertTrue(allMessagesReceived, "Not all messages were received in time");
    //     assertEquals(3, receivedMessages.size());

    //     // Test notification system
    //     CountDownLatch notificationLatch = new CountDownLatch(2);
    //     List<String> notifications = Collections.synchronizedList(new ArrayList<>());

    //     RedisPubSubService.MessageHandler notificationHandler = (channel, message) -> {
    //         log.info("🔔 Received notification: {}", message);
    //         notifications.add(message);
    //         notificationLatch.countDown();
    //     };

    //     String userId = "user123";
    //     redisPubSubService.subscribeToUserNotifications(userId, notificationHandler);

    //     // Wait a bit for subscription
    //     Thread.sleep(100);

    //     // Publish notifications
    //     redisPubSubService.publishSystemNotification(userId, "Welcome to the system!");
    //     redisPubSubService.publishSystemNotification(userId, "You have a new message");

    //     // Wait for notifications
    //     boolean allNotificationsReceived = notificationLatch.await(5, TimeUnit.SECONDS);
    //     assertTrue(allNotificationsReceived, "Not all notifications were received in time");
    //     assertEquals(2, notifications.size());

    //     // Test status updates
    //     CountDownLatch statusLatch = new CountDownLatch(1);
    //     redisPubSubService.subscribeToUserStatusUpdates((channel, message) -> {
    //         log.info("📊 Status update: {}", message);
    //         statusLatch.countDown();
    //     });

    //     Thread.sleep(100);
    //     redisPubSubService.publishUserStatusUpdate("user456", "online");

    //     boolean statusReceived = statusLatch.await(5, TimeUnit.SECONDS);
    //     assertTrue(statusReceived, "Status update was not received in time");

    //     // Test broadcast
    //     redisPubSubService.broadcastToChannelType("announcement", "System maintenance in 10 minutes");

    //     // Check active subscriptions
    //     int activeSubscriptions = redisPubSubService.getActiveSubscriptionsCount();
    //     assertTrue(activeSubscriptions > 0);

    //     // Cleanup subscriptions
    //     redisPubSubService.unsubscribeFromChannel("chat", roomId);
    //     redisPubSubService.unsubscribeFromChannel("notification", userId);
    //     redisPubSubService.unsubscribeFromChannel("status", "global");

    //     Thread.sleep(100);
        
    //     log.info("✅ Pub/Sub Messaging test completed successfully");
    // }

    @Test
    @DisplayName("10. Connection Pool & Performance Test")
    void testConnectionPoolAndPerformance() {
        log.info("=== Testing Connection Pool & Performance ===");

        // Test concurrent operations to stress connection pool
        int numberOfThreads = 10;
        int operationsPerThread = 50;
        CountDownLatch latch = new CountDownLatch(numberOfThreads);
        
        long startTime = System.currentTimeMillis();

        for (int i = 0; i < numberOfThreads; i++) {
            final int threadId = i;
            new Thread(() -> {
                try {
                    for (int j = 0; j < operationsPerThread; j++) {
                        String key = String.format("thread_%d_op_%d", threadId, j);
                        
                        // Mix of different operations
                        redisBasicService.setValue("perf", "test", key, "value_" + j);
                        redisBasicService.getValue("perf", "test", key);
                        redisBasicService.increment("perf", "counter", "thread_" + threadId);
                        
                        if (j % 10 == 0) {
                            redisHashService.setHashField("perf", "hash", "thread_" + threadId, 
                                "field_" + j, "hash_value_" + j);
                        }
                        
                        if (j % 5 == 0) {
                            redisSetService.addMember("perf", "set", "global", "member_" + threadId + "_" + j);
                        }
                    }
                } catch (Exception e) {
                    log.error("Error in performance thread {}: {}", threadId, e.getMessage());
                } finally {
                    latch.countDown();
                }
            }).start();
        }

        try {
            latch.await(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;
        int totalOperations = numberOfThreads * operationsPerThread * 4; // 4 operations per iteration

        log.info("Performance Test Results:");
        log.info("- Threads: {}", numberOfThreads);
        log.info("- Operations per thread: {}", operationsPerThread);
        log.info("- Total operations: {}", totalOperations);
        log.info("- Total time: {} ms", totalTime);
        log.info("- Operations per second: {}", (totalOperations * 1000) / totalTime);

        // Test memory usage with large data
        Map<String, String> largeDataSet = new HashMap<>();
        for (int i = 0; i < 1000; i++) {
            largeDataSet.put("im:demo:large:key" + i, "large_value_" + i + "_" + "x".repeat(100));
        }

        long batchStartTime = System.currentTimeMillis();
        redisPipelineService.batchSetStrings(largeDataSet);
        long batchEndTime = System.currentTimeMillis();

        log.info("Batch operation (1000 keys): {} ms", batchEndTime - batchStartTime);

        // Cleanup
        redisPipelineService.batchDeleteKeys(new ArrayList<>(largeDataSet.keySet()));

        log.info("✅ Connection Pool & Performance test completed successfully");
    }
}
