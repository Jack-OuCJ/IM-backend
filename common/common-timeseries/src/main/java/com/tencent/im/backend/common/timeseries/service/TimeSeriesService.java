package com.tencent.im.backend.common.timeseries.service;

import com.tencent.im.backend.common.timeseries.model.MessageData;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 时序数据库服务接口
 */
public interface TimeSeriesService {

    /**
     * 写入单条消息数据
     */
    void writeMessage(MessageData messageData);

    /**
     * 批量写入消息数据
     */
    void writeMessages(List<MessageData> messageDataList);

    /**
     * 查询用户消息历史
     */
    List<MessageData> queryUserMessages(String userId, LocalDateTime startTime, LocalDateTime endTime, int limit);

    /**
     * 查询群组消息历史
     */
    List<MessageData> queryGroupMessages(String groupId, LocalDateTime startTime, LocalDateTime endTime, int limit);

    /**
     * 查询消息统计信息
     */
    MessageStats queryMessageStats(String userId, LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 删除过期消息
     */
    void deleteExpiredMessages(LocalDateTime beforeTime);

    /**
     * 消息统计信息
     */
    class MessageStats {
        private long totalCount;
        private long textCount;
        private long imageCount;
        private long videoCount;
        private long fileCount;
        private long totalSize;

        // Getters and Setters
        public long getTotalCount() {
            return totalCount;
        }

        public void setTotalCount(long totalCount) {
            this.totalCount = totalCount;
        }

        public long getTextCount() {
            return textCount;
        }

        public void setTextCount(long textCount) {
            this.textCount = textCount;
        }

        public long getImageCount() {
            return imageCount;
        }

        public void setImageCount(long imageCount) {
            this.imageCount = imageCount;
        }

        public long getVideoCount() {
            return videoCount;
        }

        public void setVideoCount(long videoCount) {
            this.videoCount = videoCount;
        }

        public long getFileCount() {
            return fileCount;
        }

        public void setFileCount(long fileCount) {
            this.fileCount = fileCount;
        }

        public long getTotalSize() {
            return totalSize;
        }

        public void setTotalSize(long totalSize) {
            this.totalSize = totalSize;
        }
    }
}
