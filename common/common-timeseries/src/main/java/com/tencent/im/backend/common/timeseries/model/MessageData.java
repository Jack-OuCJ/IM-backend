package com.tencent.im.backend.common.timeseries.model;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 消息数据模型（用于时序数据库存储）
 */
public class MessageData {

    /** 消息ID */
    private String messageId;

    /** 发送者ID */
    private String fromUserId;

    /** 接收者ID（单聊时使用） */
    private String toUserId;

    /** 群组ID（群聊时使用） */
    private String groupId;

    /** 消息类型 */
    private String messageType;

    /** 消息内容 */
    private String content;

    /** 消息序列号 */
    private Long sequence;

    /** 发送时间 */
    private LocalDateTime timestamp;

    /** 消息大小（字节） */
    private Long messageSize;

    /** 会话类型（C2C/Group） */
    private String chatType;

    /** 扩展字段 */
    private Map<String, Object> extras;

    /** 消息状态（已发送/已送达/已读等） */
    private String status;

    /** 客户端类型 */
    private String clientType;

    /** 平台信息 */
    private String platform;

    public MessageData() {
        this.timestamp = LocalDateTime.now();
    }

    // Getters and Setters
    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getFromUserId() {
        return fromUserId;
    }

    public void setFromUserId(String fromUserId) {
        this.fromUserId = fromUserId;
    }

    public String getToUserId() {
        return toUserId;
    }

    public void setToUserId(String toUserId) {
        this.toUserId = toUserId;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getMessageType() {
        return messageType;
    }

    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Long getSequence() {
        return sequence;
    }

    public void setSequence(Long sequence) {
        this.sequence = sequence;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public Long getMessageSize() {
        return messageSize;
    }

    public void setMessageSize(Long messageSize) {
        this.messageSize = messageSize;
    }

    public String getChatType() {
        return chatType;
    }

    public void setChatType(String chatType) {
        this.chatType = chatType;
    }

    public Map<String, Object> getExtras() {
        return extras;
    }

    public void setExtras(Map<String, Object> extras) {
        this.extras = extras;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getClientType() {
        return clientType;
    }

    public void setClientType(String clientType) {
        this.clientType = clientType;
    }

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }
}
