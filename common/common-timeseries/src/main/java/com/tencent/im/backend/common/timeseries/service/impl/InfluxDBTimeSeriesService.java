package com.tencent.im.backend.common.timeseries.service.impl;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import com.influxdb.client.WriteApiBlocking;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;
import com.influxdb.query.FluxRecord;
import com.influxdb.query.FluxTable;
import com.tencent.im.backend.common.timeseries.config.TimeSeriesProperties;
import com.tencent.im.backend.common.timeseries.model.MessageData;
import com.tencent.im.backend.common.timeseries.service.TimeSeriesService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

/**
 * InfluxDB时序数据库服务实现
 */
@Service
@ConditionalOnProperty(name = "timeseries.type", havingValue = "INFLUXDB")
public class InfluxDBTimeSeriesService implements TimeSeriesService {

    private static final Logger logger = LoggerFactory.getLogger(InfluxDBTimeSeriesService.class);

    @Autowired
    private TimeSeriesProperties timeSeriesProperties;

    private InfluxDBClient influxDBClient;

    @PostConstruct
    public void init() {
        try {
            TimeSeriesProperties.InfluxDB influxDBConfig = timeSeriesProperties.getInfluxdb();
            influxDBClient = InfluxDBClientFactory.create(
                    influxDBConfig.getUrl(),
                    influxDBConfig.getToken().toCharArray(),
                    influxDBConfig.getOrg(),
                    influxDBConfig.getBucket()
            );
            logger.info("InfluxDB client initialized successfully");
        } catch (Exception e) {
            logger.error("Failed to initialize InfluxDB client", e);
        }
    }

    @PreDestroy
    public void destroy() {
        if (influxDBClient != null) {
            influxDBClient.close();
        }
    }

    @Override
    public void writeMessage(MessageData messageData) {
        try {
            Point point = createPointFromMessage(messageData);
            WriteApiBlocking writeApi = influxDBClient.getWriteApiBlocking();
            writeApi.writePoint(point);
            logger.debug("Message written to InfluxDB: messageId={}", messageData.getMessageId());
        } catch (Exception e) {
            logger.error("Failed to write message to InfluxDB: messageId={}", messageData.getMessageId(), e);
        }
    }

    @Override
    public void writeMessages(List<MessageData> messageDataList) {
        try {
            List<Point> points = new ArrayList<>();
            for (MessageData messageData : messageDataList) {
                points.add(createPointFromMessage(messageData));
            }
            
            WriteApiBlocking writeApi = influxDBClient.getWriteApiBlocking();
            writeApi.writePoints(points);
            logger.debug("Batch messages written to InfluxDB: count={}", messageDataList.size());
        } catch (Exception e) {
            logger.error("Failed to write batch messages to InfluxDB", e);
        }
    }

    @Override
    public List<MessageData> queryUserMessages(String userId, LocalDateTime startTime, LocalDateTime endTime, int limit) {
        try {
            String flux = String.format(
                    "from(bucket: \"%s\") " +
                    "|> range(start: %s, stop: %s) " +
                    "|> filter(fn: (r) => r._measurement == \"messages\") " +
                    "|> filter(fn: (r) => r.from_user_id == \"%s\" or r.to_user_id == \"%s\") " +
                    "|> sort(columns: [\"_time\"], desc: true) " +
                    "|> limit(n: %d)",
                    timeSeriesProperties.getInfluxdb().getBucket(),
                    startTime.toInstant(ZoneOffset.UTC),
                    endTime.toInstant(ZoneOffset.UTC),
                    userId, userId, limit
            );

            return executeQuery(flux);
        } catch (Exception e) {
            logger.error("Failed to query user messages from InfluxDB: userId={}", userId, e);
            return new ArrayList<>();
        }
    }

    @Override
    public List<MessageData> queryGroupMessages(String groupId, LocalDateTime startTime, LocalDateTime endTime, int limit) {
        try {
            String flux = String.format(
                    "from(bucket: \"%s\") " +
                    "|> range(start: %s, stop: %s) " +
                    "|> filter(fn: (r) => r._measurement == \"messages\") " +
                    "|> filter(fn: (r) => r.group_id == \"%s\") " +
                    "|> sort(columns: [\"_time\"], desc: true) " +
                    "|> limit(n: %d)",
                    timeSeriesProperties.getInfluxdb().getBucket(),
                    startTime.toInstant(ZoneOffset.UTC),
                    endTime.toInstant(ZoneOffset.UTC),
                    groupId, limit
            );

            return executeQuery(flux);
        } catch (Exception e) {
            logger.error("Failed to query group messages from InfluxDB: groupId={}", groupId, e);
            return new ArrayList<>();
        }
    }

    @Override
    public MessageStats queryMessageStats(String userId, LocalDateTime startTime, LocalDateTime endTime) {
        MessageStats stats = new MessageStats();
        try {
            String flux = String.format(
                    "from(bucket: \"%s\") " +
                    "|> range(start: %s, stop: %s) " +
                    "|> filter(fn: (r) => r._measurement == \"messages\") " +
                    "|> filter(fn: (r) => r.from_user_id == \"%s\") " +
                    "|> group(columns: [\"message_type\"]) " +
                    "|> count()",
                    timeSeriesProperties.getInfluxdb().getBucket(),
                    startTime.toInstant(ZoneOffset.UTC),
                    endTime.toInstant(ZoneOffset.UTC),
                    userId
            );

            List<FluxTable> tables = influxDBClient.getQueryApi().query(flux);
            for (FluxTable table : tables) {
                for (FluxRecord record : table.getRecords()) {
                    String messageType = String.valueOf(record.getValueByKey("message_type"));
                    Long count = (Long) record.getValue();
                    
                    switch (messageType) {
                        case "TIMTextElem":
                            stats.setTextCount(count);
                            break;
                        case "TIMImageElem":
                            stats.setImageCount(count);
                            break;
                        case "TIMVideoFileElem":
                            stats.setVideoCount(count);
                            break;
                        case "TIMFileElem":
                            stats.setFileCount(count);
                            break;
                    }
                    stats.setTotalCount(stats.getTotalCount() + count);
                }
            }
        } catch (Exception e) {
            logger.error("Failed to query message stats from InfluxDB: userId={}", userId, e);
        }
        return stats;
    }

    @Override
    public void deleteExpiredMessages(LocalDateTime beforeTime) {
        try {
            String flux = String.format(
                    "from(bucket: \"%s\") " +
                    "|> range(start: 1970-01-01T00:00:00Z, stop: %s) " +
                    "|> filter(fn: (r) => r._measurement == \"messages\") " +
                    "|> drop()",
                    timeSeriesProperties.getInfluxdb().getBucket(),
                    beforeTime.toInstant(ZoneOffset.UTC)
            );

            influxDBClient.getQueryApi().query(flux);
            logger.info("Expired messages deleted from InfluxDB: beforeTime={}", beforeTime);
        } catch (Exception e) {
            logger.error("Failed to delete expired messages from InfluxDB", e);
        }
    }

    private Point createPointFromMessage(MessageData messageData) {
        Point point = Point.measurement("messages")
                .time(messageData.getTimestamp().toInstant(ZoneOffset.UTC), WritePrecision.S)
                .addTag("message_id", messageData.getMessageId())
                .addTag("from_user_id", messageData.getFromUserId())
                .addTag("message_type", messageData.getMessageType())
                .addTag("chat_type", messageData.getChatType())
                .addField("content", messageData.getContent())
                .addField("sequence", messageData.getSequence())
                .addField("message_size", messageData.getMessageSize());

        if (messageData.getToUserId() != null) {
            point.addTag("to_user_id", messageData.getToUserId());
        }
        if (messageData.getGroupId() != null) {
            point.addTag("group_id", messageData.getGroupId());
        }
        if (messageData.getStatus() != null) {
            point.addTag("status", messageData.getStatus());
        }
        if (messageData.getClientType() != null) {
            point.addTag("client_type", messageData.getClientType());
        }
        if (messageData.getPlatform() != null) {
            point.addTag("platform", messageData.getPlatform());
        }

        return point;
    }

    private List<MessageData> executeQuery(String flux) {
        List<MessageData> messages = new ArrayList<>();
        List<FluxTable> tables = influxDBClient.getQueryApi().query(flux);
        
        for (FluxTable table : tables) {
            for (FluxRecord record : table.getRecords()) {
                MessageData messageData = createMessageFromRecord(record);
                messages.add(messageData);
            }
        }
        
        return messages;
    }

    private MessageData createMessageFromRecord(FluxRecord record) {
        MessageData messageData = new MessageData();
        messageData.setMessageId(String.valueOf(record.getValueByKey("message_id")));
        messageData.setFromUserId(String.valueOf(record.getValueByKey("from_user_id")));
        messageData.setToUserId(String.valueOf(record.getValueByKey("to_user_id")));
        messageData.setGroupId(String.valueOf(record.getValueByKey("group_id")));
        messageData.setMessageType(String.valueOf(record.getValueByKey("message_type")));
        messageData.setChatType(String.valueOf(record.getValueByKey("chat_type")));
        messageData.setContent(String.valueOf(record.getValue()));
        
        Instant instant = (Instant) record.getTime();
        if (instant != null) {
            messageData.setTimestamp(LocalDateTime.ofInstant(instant, ZoneOffset.UTC));
        }
        
        return messageData;
    }
}
