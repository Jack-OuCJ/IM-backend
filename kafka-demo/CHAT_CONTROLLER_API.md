# Chat Controller API Documentation

## Overview
The Chat Controller provides REST APIs for sending chat messages to Kafka with different partitioning strategies. This service demonstrates various Kafka producer patterns and partition distribution strategies.

**Base URL:** `http://localhost:8095`  
**API Prefix:** `/api/chat`

## Message Model

### ChatMessage
```json
{
  "id": "string (optional - auto-generated if not provided)",
  "sender": "string (required)",
  "receiver": "string (required)", 
  "content": "string (required)",
  "timestamp": "string (format: 'yyyy-MM-dd HH:mm:ss', e.g., '2025-08-26 10:30:00')"
}
```

## API Endpoints

### 1. Send Message (Default Partition Strategy)
Send a message using Kafka's default round-robin partition strategy.

**Endpoint:** `POST /api/chat/send`

**Description:** Sends a message using the default Kafka partitioning strategy (round-robin). Messages are distributed evenly across all available partitions.

**Request Body:**
```json
{
  "sender": "user1",
  "receiver": "user2",
  "content": "Hello, this is a test message",
  "timestamp": "2025-08-26 10:30:00"
}
```

**Response:**
```json
"Message sent successfully"
```

**curl Example:**
```bash
curl -X POST http://localhost:8095/api/chat/send \
  -H "Content-Type: application/json" \
  -d '{
    "sender": "user1",
    "receiver": "user2",
    "content": "Hello, this is a test message",
    "timestamp": "2025-08-26 10:30:00"
  }'
```

---

### 2. Send Message with Key
Send a message with a specified key for consistent partition assignment.

**Endpoint:** `POST /api/chat/send-with-key`

**Description:** Sends a message with a specified key. Messages with the same key will always go to the same partition, ensuring ordering for messages with the same key.

**Parameters:**
- `key` (query parameter, required): The partition key

**Request Body:** Same as basic send message

**curl Example:**
```bash
curl -X POST "http://localhost:8095/api/chat/send-with-key?key=conversation-123" \
  -H "Content-Type: application/json" \
  -d '{
    "sender": "alice",
    "receiver": "bob",
    "content": "Message with specific key",
    "timestamp": "2025-08-26 10:35:00"
  }'
```

---

### 3. Send Message to Specific Partition
Send a message directly to a specified partition.

**Endpoint:** `POST /api/chat/send-to-partition`

**Description:** Sends a message directly to a specified partition number. Useful for testing or when you need precise control over partition assignment.

**Parameters:**
- `partition` (query parameter, required): Target partition number (0-based)
- `key` (query parameter, optional): Message key

**curl Example:**
```bash
# Send to partition 0 without key
curl -X POST "http://localhost:8095/api/chat/send-to-partition?partition=0" \
  -H "Content-Type: application/json" \
  -d '{
    "sender": "user1",
    "receiver": "user2",
    "content": "Message to partition 0",
    "timestamp": "2025-08-26 10:40:00"
  }'

# Send to partition 1 with key
curl -X POST "http://localhost:8095/api/chat/send-to-partition?partition=1&key=custom-key" \
  -H "Content-Type: application/json" \
  -d '{
    "sender": "user3",
    "receiver": "user4",
    "content": "Message to partition 1 with key",
    "timestamp": "2025-08-26 10:45:00"
  }'
```

---

### 4. Send Message with Business Partition Strategy
Send a message using business logic for partition assignment.

**Endpoint:** `POST /api/chat/send-business-partition`

**Description:** Sends a message using a business-specific partitioning strategy. This ensures that all messages from the same conversation (between the same sender and receiver) go to the same partition, maintaining message ordering per conversation.

**curl Example:**
```bash
curl -X POST http://localhost:8095/api/chat/send-business-partition \
  -H "Content-Type: application/json" \
  -d '{
    "sender": "alice",
    "receiver": "bob",
    "content": "Business partitioned message",
    "timestamp": "2025-08-26 10:50:00"
  }'
```

---

### 5. Send Batch Messages
Send multiple messages for testing performance and partition distribution.

**Endpoint:** `POST /api/chat/send-batch`

**Description:** Sends a batch of messages based on a template. Useful for testing throughput and observing partition distribution patterns.

**Parameters:**
- `count` (query parameter, optional, default=10): Number of messages to send
- `useBusinessPartition` (query parameter, optional, default=false): Whether to use business partition strategy

**curl Examples:**
```bash
# Send 5 messages using default partitioning
curl -X POST "http://localhost:8095/api/chat/send-batch?count=5" \
  -H "Content-Type: application/json" \
  -d '{
    "sender": "user1",
    "receiver": "user2",
    "content": "Batch test message",
    "timestamp": "2025-08-26 11:00:00"
  }'

# Send 10 messages using business partitioning
curl -X POST "http://localhost:8095/api/chat/send-batch?count=10&useBusinessPartition=true" \
  -H "Content-Type: application/json" \
  -d '{
    "sender": "alice",
    "receiver": "bob",
    "content": "Business batch test",
    "timestamp": "2025-08-26 11:05:00"
  }'
```

---

## Error Responses

### Validation Errors
**Status Code:** `400 Bad Request`
```json
{
  "message": "Validation failed",
  "errors": [
    "sender cannot be blank",
    "content cannot be blank"
  ]
}
```

### Server Errors
**Status Code:** `500 Internal Server Error`
```json
{
  "message": "Failed to send message",
  "error": "Kafka connection error"
}
```

---

## Partition Strategies Explained

### 1. Default Partitioning (Round-Robin)
- Messages are distributed evenly across all partitions
- Good for load balancing
- No ordering guarantees

### 2. Key-Based Partitioning
- Messages with the same key go to the same partition
- Maintains ordering for messages with the same key
- Key can be any string value

### 3. Specific Partition
- Direct control over which partition receives the message
- Useful for testing or specific requirements
- Bypass Kafka's partitioning logic

### 4. Business Partitioning
- Uses conversation context (sender + receiver) to determine partition
- Ensures all messages in a conversation maintain order
- Optimized for chat application use cases

---

## Testing Examples

### Basic Functionality Test
```bash
# Test basic message sending
curl -X POST http://localhost:8095/api/chat/send \
  -H "Content-Type: application/json" \
  -d '{
    "sender": "testUser1",
    "receiver": "testUser2",
    "content": "Hello World!",
    "timestamp": "2025-08-26 12:00:00"
  }'
```

### Partition Distribution Test
```bash
# Send messages to different partitions
for i in {0..2}; do
  curl -X POST "http://localhost:8095/api/chat/send-to-partition?partition=$i" \
    -H "Content-Type: application/json" \
    -d "{
      \"sender\": \"user1\",
      \"receiver\": \"user2\",
      \"content\": \"Message to partition $i\",
      \"timestamp\": \"2025-08-26 12:0$i:00\"
    }"
done
```

### Conversation Ordering Test
```bash
# Send multiple messages in a conversation using business partitioning
for i in {1..5}; do
  curl -X POST http://localhost:8095/api/chat/send-business-partition \
    -H "Content-Type: application/json" \
    -d "{
      \"sender\": \"alice\",
      \"receiver\": \"bob\",
      \"content\": \"Conversation message $i\",
      \"timestamp\": \"2025-08-26 12:0$i:00\"
    }"
done
```

### Performance Test
```bash
# Send a large batch for performance testing
curl -X POST "http://localhost:8095/api/chat/send-batch?count=100" \
  -H "Content-Type: application/json" \
  -d '{
    "sender": "loadTestUser",
    "receiver": "targetUser",
    "content": "Performance test message",
    "timestamp": "2025-08-26 12:00:00"
  }'
```

---

## Monitoring and Debugging

### Check Application Health
```bash
# Check if the service is running
curl http://localhost:8095/actuator/health

# Check application info
curl http://localhost:8095/actuator/info
```

### Kafka Topic Information
You can monitor the Kafka topic using Kafka UI at `http://localhost:9090` or use Kafka command-line tools to inspect:
- Topic partitions
- Message distribution
- Consumer lag
- Message content

### Log Monitoring
The application logs detailed information about:
- Message sending operations
- Partition assignments
- Error conditions
- Performance metrics

Check the application logs for detailed information about message processing and any issues that might occur.
