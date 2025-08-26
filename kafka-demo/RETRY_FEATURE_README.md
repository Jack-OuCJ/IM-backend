# Kafka Producer Retry Feature

This document describes the retry functionality added to the ChatMessageProducer service.

## Overview

The ChatMessageProducer now includes comprehensive retry mechanisms to ensure reliable message delivery to Kafka. The service provides both asynchronous and synchronous sending methods with configurable retry parameters.

## Features

### 1. Configurable Retry Parameters

The retry behavior can be configured through application properties:

```yaml
kafka:
  producer:
    retry:
      max-attempts: 3      # Maximum number of retry attempts (default: 3)
      delay-ms: 1000       # Delay between retries in milliseconds (default: 1000)
```

### 2. Asynchronous Methods with Retry

These methods send messages asynchronously and retry on failure:

- `sendMessage(ChatMessage message)` - Default partition strategy
- `sendMessageWithKey(String key, ChatMessage message)` - Hash-based partitioning
- `sendMessageToPartition(int partition, String key, ChatMessage message)` - Specific partition
- `sendMessageWithBusinessPartition(ChatMessage message)` - Business logic partitioning

### 3. Synchronous Methods with Retry

These methods block until success or max retries reached:

- `sendMessageSync(ChatMessage message)` - Returns boolean indicating success
- `sendMessageWithKeySync(String key, ChatMessage message)` - Returns boolean
- `sendMessageToPartitionSync(int partition, String key, ChatMessage message)` - Returns boolean

## Usage Examples

### Basic Asynchronous Sending

```java
@Autowired
private ChatMessageProducer producer;

ChatMessage message = new ChatMessage("user1", "user2", "Hello!", System.currentTimeMillis());

// Will retry up to 3 times with 1 second delay between attempts
producer.sendMessage(message);
```

### Synchronous Sending with Result Check

```java
ChatMessage message = new ChatMessage("user1", "user2", "Important message", System.currentTimeMillis());

boolean success = producer.sendMessageSync(message);
if (success) {
    logger.info("Message delivered successfully");
} else {
    logger.error("Message failed after all retries");
    // Handle failure (e.g., store in database, alert admin)
}
```

### Business Partition Strategy

```java
// Messages between same users always go to same partition
// Ensures ordering for conversation threads
producer.sendMessageWithBusinessPartition(message);
```

## REST API Endpoints

The service provides REST endpoints for testing retry functionality:

### 1. Asynchronous Endpoints

```http
POST /api/v1/chat/send
Content-Type: application/json

{
  "sender": "user1",
  "receiver": "user2", 
  "content": "Hello World!",
  "messageType": "TEXT"
}
```

```http
POST /api/v1/chat/send-with-key?key=conversation-123
POST /api/v1/chat/send-to-partition?partition=1&key=optional-key
POST /api/v1/chat/send-business-partition
```

### 2. Synchronous Endpoint

```http
POST /api/v1/chat/send-sync
Content-Type: application/json

{
  "sender": "user1",
  "receiver": "user2",
  "content": "Critical message",
  "messageType": "TEXT"
}
```

## Logging

The service provides detailed logging for retry attempts:

```
INFO  - Message sent successfully for default partition: partition=1, offset=123, topic=chat-message-topic
WARN  - Failed to send message on attempt 1 for key: user1:user2: Connection timeout. Retrying in 1000ms...
INFO  - Message sent successfully for key: user1:user2 on attempt 2: partition=1, offset=124, topic=chat-message-topic
ERROR - Failed to send message after 3 attempts for partition: 2: Broker not available
```

## Error Handling

### Asynchronous Methods
- Log warnings for retry attempts
- Log errors when max retries exceeded
- Application continues normal operation

### Synchronous Methods
- Return `false` when max retries exceeded
- Allow caller to handle failure appropriately
- Can be used for critical messages requiring guaranteed delivery

## Configuration Best Practices

### Development Environment
```yaml
kafka:
  producer:
    retry:
      max-attempts: 2
      delay-ms: 500
```

### Production Environment
```yaml
kafka:
  producer:
    retry:
      max-attempts: 5
      delay-ms: 2000
```

### High-Throughput Environment
```yaml
kafka:
  producer:
    retry:
      max-attempts: 3
      delay-ms: 100
```

## Testing

Run the test suite to verify retry functionality:

```bash
mvn test -Dtest=ChatMessageProducerRetryTest
```

## Performance Considerations

1. **Asynchronous vs Synchronous**: Use async methods for better throughput, sync for critical messages
2. **Retry Delay**: Balance between quick recovery and avoiding system overload
3. **Max Attempts**: Consider downstream impact of repeated failures
4. **Monitoring**: Monitor retry metrics to identify infrastructure issues

## Troubleshooting

### High Retry Rates
- Check Kafka broker health
- Verify network connectivity
- Review producer configuration (batch size, timeouts)

### Messages Still Failing
- Increase max retry attempts
- Increase retry delay
- Check for data serialization issues
- Verify topic exists and is accessible

## Future Enhancements

Potential improvements to consider:

1. **Exponential Backoff**: Increase delay progressively
2. **Dead Letter Queue**: Store permanently failed messages
3. **Circuit Breaker**: Temporarily stop retries when broker is down
4. **Metrics**: Add Micrometer metrics for retry statistics
5. **Custom Retry Strategies**: Per-message type retry configuration
