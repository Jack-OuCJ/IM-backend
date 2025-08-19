package com.tencent.im.backend.common.core.model;

import com.alibaba.fastjson2.annotation.JSONField;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 基础响应类
 */
public class BaseResponse<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 状态码 */
    private Integer code;

    /** 消息 */
    private String message;

    /** 数据 */
    private T data;

    /** 时间戳 */
    @JSONField(format = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime timestamp;

    /** 请求ID */
    private String requestId;

    public BaseResponse() {
        this.timestamp = LocalDateTime.now();
    }

    public BaseResponse(Integer code, String message) {
        this();
        this.code = code;
        this.message = message;
    }

    public BaseResponse(Integer code, String message, T data) {
        this(code, message);
        this.data = data;
    }

    public static <T> BaseResponse<T> success() {
        return new BaseResponse<>(200, "success");
    }

    public static <T> BaseResponse<T> success(T data) {
        return new BaseResponse<>(200, "success", data);
    }

    public static <T> BaseResponse<T> success(String message, T data) {
        return new BaseResponse<>(200, message, data);
    }

    public static <T> BaseResponse<T> error(String message) {
        return new BaseResponse<>(500, message);
    }

    public static <T> BaseResponse<T> error(Integer code, String message) {
        return new BaseResponse<>(code, message);
    }

    // Getters and Setters
    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }
}
