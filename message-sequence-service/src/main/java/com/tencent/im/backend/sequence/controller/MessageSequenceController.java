package com.tencent.im.backend.sequence.controller;

import com.tencent.im.backend.common.core.model.BaseResponse;
import com.tencent.im.backend.sequence.service.MessageSequenceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 消息序列号控制器
 */
@RestController
@RequestMapping("/api/sequence")
@Tag(name = "消息序列号管理", description = "消息序列号相关API")
public class MessageSequenceController {

    @Autowired
    private MessageSequenceService messageSequenceService;

    @Operation(summary = "获取单聊消息序列号")
    @GetMapping("/c2c")
    public BaseResponse<Long> getC2CSequence(@RequestParam String fromUserId, @RequestParam String toUserId) {
        Long sequence = messageSequenceService.getC2CMessageSequence(fromUserId, toUserId);
        return BaseResponse.success(sequence);
    }

    @Operation(summary = "获取群聊消息序列号")
    @GetMapping("/group")
    public BaseResponse<Long> getGroupSequence(@RequestParam String groupId) {
        Long sequence = messageSequenceService.getGroupMessageSequence(groupId);
        return BaseResponse.success(sequence);
    }

    @Operation(summary = "获取当前单聊消息序列号")
    @GetMapping("/c2c/current")
    public BaseResponse<Long> getCurrentC2CSequence(@RequestParam String fromUserId, @RequestParam String toUserId) {
        Long sequence = messageSequenceService.getCurrentC2CSequence(fromUserId, toUserId);
        return BaseResponse.success(sequence);
    }

    @Operation(summary = "获取当前群聊消息序列号")
    @GetMapping("/group/current")
    public BaseResponse<Long> getCurrentGroupSequence(@RequestParam String groupId) {
        Long sequence = messageSequenceService.getCurrentGroupSequence(groupId);
        return BaseResponse.success(sequence);
    }

    @Operation(summary = "重置单聊消息序列号")
    @PostMapping("/c2c/reset")
    public BaseResponse<Void> resetC2CSequence(@RequestParam String fromUserId, @RequestParam String toUserId) {
        messageSequenceService.resetC2CMessageSequence(fromUserId, toUserId);
        return BaseResponse.success();
    }

    @Operation(summary = "重置群聊消息序列号")
    @PostMapping("/group/reset")
    public BaseResponse<Void> resetGroupSequence(@RequestParam String groupId) {
        messageSequenceService.resetGroupMessageSequence(groupId);
        return BaseResponse.success();
    }
}
