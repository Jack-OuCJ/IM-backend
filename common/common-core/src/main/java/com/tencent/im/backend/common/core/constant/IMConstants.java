package com.tencent.im.backend.common.core.constant;

/**
 * IM相关常量
 */
public class IMConstants {

    /** 消息类型 */
    public static class MessageType {
        public static final String TEXT = "TIMTextElem";
        public static final String IMAGE = "TIMImageElem";
        public static final String SOUND = "TIMSoundElem";
        public static final String VIDEO = "TIMVideoFileElem";
        public static final String FILE = "TIMFileElem";
        public static final String LOCATION = "TIMLocationElem";
        public static final String FACE = "TIMFaceElem";
        public static final String CUSTOM = "TIMCustomElem";
    }

    /** 会话类型 */
    public static class ChatType {
        public static final String C2C = "C2C";
        public static final String GROUP = "Group";
    }

    /** 群组类型 */
    public static class GroupType {
        public static final String PRIVATE = "Private";
        public static final String PUBLIC = "Public";
        public static final String CHAT_ROOM = "ChatRoom";
        public static final String AUDIO_VIDEO_ROOM = "AVChatRoom";
        public static final String BROADCAST_ROOM = "BChatRoom";
    }

    /** 回调命令 */
    public static class CallbackCommand {
        public static final String BEFORE_SEND_MSG = "C2C.CallbackBeforeSendMsg";
        public static final String AFTER_SEND_MSG = "C2C.CallbackAfterSendMsg";
        public static final String GROUP_BEFORE_SEND_MSG = "Group.CallbackBeforeSendMsg";
        public static final String GROUP_AFTER_SEND_MSG = "Group.CallbackAfterSendMsg";
        public static final String GROUP_JOIN = "Group.CallbackAfterNewMemberJoin";
        public static final String GROUP_QUIT = "Group.CallbackAfterMemberExit";
        public static final String STATE_CHANGE = "State.StateChange";
    }

    /** 用户状态 */
    public static class UserStatus {
        public static final String ONLINE = "Online";
        public static final String OFFLINE = "Offline";
        public static final String PUSH_ONLINE = "PushOnline";
    }

    /** 消息序列号相关 */
    public static class MessageSequence {
        /** 单聊消息序列号前缀 */
        public static final String C2C_SEQ_PREFIX = "c2c_seq:";
        /** 群聊消息序列号前缀 */
        public static final String GROUP_SEQ_PREFIX = "group_seq:";
        /** 序列号过期时间（秒） */
        public static final int SEQ_EXPIRE_TIME = 86400 * 7; // 7天
    }

    /** 心跳检测 */
    public static class Heartbeat {
        /** 心跳间隔（毫秒） */
        public static final long HEARTBEAT_INTERVAL = 30000L; // 30秒
        /** 心跳超时（毫秒） */
        public static final long HEARTBEAT_TIMEOUT = 60000L; // 60秒
        /** 最大重试次数 */
        public static final int MAX_RETRY_COUNT = 3;
    }

    /** 连接相关 */
    public static class Connection {
        /** 连接超时时间（毫秒） */
        public static final long CONNECT_TIMEOUT = 10000L; // 10秒
        /** 读超时时间（毫秒） */
        public static final long READ_TIMEOUT = 30000L; // 30秒
        /** 写超时时间（毫秒） */
        public static final long WRITE_TIMEOUT = 10000L; // 10秒
        /** 连接池最大连接数 */
        public static final int MAX_CONNECTIONS = 1000;
    }
}
