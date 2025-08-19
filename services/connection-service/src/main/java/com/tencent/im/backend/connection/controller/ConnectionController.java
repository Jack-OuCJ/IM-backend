package com.tencent.im.backend.connection.controller;

import com.tencent.im.backend.common.core.model.BaseResponse;
import com.tencent.im.backend.common.websocket.manager.ConnectionManager;
import com.tencent.im.backend.common.websocket.model.ConnectionInfo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 连接管理控制器
 */
@RestController
@RequestMapping("/api/connection")
@Tag(name = "连接管理", description = "WebSocket连接管理相关API")
public class ConnectionController {

    @Autowired
    private ConnectionManager connectionManager;

    @Operation(summary = "获取连接信息")
    @GetMapping("/{connectionId}")
    public BaseResponse<ConnectionInfo> getConnectionInfo(@PathVariable String connectionId) {
        ConnectionInfo connectionInfo = connectionManager.getConnectionInfo(connectionId);
        return BaseResponse.success(connectionInfo);
    }

    @Operation(summary = "检查用户是否在线")
    @GetMapping("/online/{userId}")
    public BaseResponse<Boolean> isUserOnline(@PathVariable String userId) {
        boolean online = connectionManager.isUserOnline(userId);
        return BaseResponse.success(online);
    }

    @Operation(summary = "获取用户所有连接")
    @GetMapping("/user/{userId}")
    public BaseResponse<List<String>> getUserConnections(@PathVariable String userId) {
        List<String> connections = connectionManager.getUserConnections(userId);
        return BaseResponse.success(connections);
    }

    @Operation(summary = "获取连接统计信息")
    @GetMapping("/stats")
    public BaseResponse<Map<String, Object>> getConnectionStats() {
        int localCount = connectionManager.getLocalConnectionCount();
        long globalCount = connectionManager.getGlobalConnectionCount();
        
        Map<String, Object> stats = Map.of(
            "localConnections", localCount,
            "globalConnections", globalCount
        );
        
        return BaseResponse.success(stats);
    }

    @Operation(summary = "断开用户所有连接")
    @PostMapping("/disconnect/{userId}")
    public BaseResponse<Void> disconnectUser(@PathVariable String userId) {
        List<String> connections = connectionManager.getUserConnections(userId);
        for (String connectionId : connections) {
            connectionManager.removeConnection(connectionId);
        }
        return BaseResponse.success();
    }
}
