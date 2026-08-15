/**
 * ============================================================
 * 本文件为原创代码，版权归 YOUR_NAME 所有，仅供购买者学习使用。
 * 未经授权禁止复制、转售、二次分发。
 * @author YOUR_NAME
 * ============================================================
 */
package com.ds.university.service;

import com.ds.university.common.BusinessException;
import com.ds.university.config.ChatHandshakeInterceptor;
import com.ds.university.entity.ChatMessage;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 聊天 WebSocket 处理器（原生 WebSocket + JSON 协议）。
 * <ul>
 *   <li>在线会话注册表（本实例本地）：userId → WebSocketSession；</li>
 *   <li>协议：客户端 → {type:send,to,content} / {type:history,with,limit} / {type:read,with} / {type:clear,with}；
 *       服务端 → {type:chat,...} / {type:history,...} / {type:presence,users} / {type:cleared,...} / {type:error,message}；</li>
 *   <li>跨实例路由：多实例模式经 {@link ChatEventPublisher}（Redis Pub/Sub）广播，
 *       接收方所在实例投递；单机模式本地直发；</li>
 *   <li>在线集合：Redis SET（chat:online），跨实例一致；</li>
 *   <li>发送消息先落库（未读），再实时投递；接收方不在线则留作未读；</li>
 *   <li>用户身份取自握手阶段写入的会话属性，不信任客户端自报身份。</li>
 * </ul>
 */
@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(ChatWebSocketHandler.class);
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    /** 聊天消息频道（跨实例路由） */
    public static final String CHANNEL_MESSAGE = "chat:message";
    /** 在线状态变更频道 */
    public static final String CHANNEL_PRESENCE = "chat:presence";

    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final ChatService chatService;
    private final ObjectMapper objectMapper;
    private final ChatEventPublisher publisher;

    public ChatWebSocketHandler(ChatService chatService, ObjectMapper objectMapper,
                                ChatEventPublisher publisher) {
        this.chatService = chatService;
        this.objectMapper = objectMapper;
        this.publisher = publisher;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String userId = userId(session);
        if (userId == null) {
            closeQuietly(session, CloseStatus.NOT_ACCEPTABLE);
            return;
        }
        WebSocketSession previous = sessions.put(userId, session);
        if (previous != null && previous != session && previous.isOpen()) {
            closeQuietly(previous, CloseStatus.NORMAL);
        }
        publisher.registerOnline(userId);
        presenceUpdate();
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        String userId = userId(session);
        if (userId == null) {
            return;
        }
        Map<String, Object> req;
        try {
            req = objectMapper.readValue(message.getPayload(), new TypeReference<Map<String, Object>>() { });
        } catch (Exception e) {
            sendJson(session, error("消息格式不正确"));
            return;
        }
        String type = req.get("type") == null ? "" : String.valueOf(req.get("type"));
        try {
            switch (type) {
                case "send":
                    handleSend(userId, req);
                    break;
                case "history":
                    handleHistory(session, userId, req);
                    break;
                case "read":
                    handleRead(userId, req);
                    break;
                case "clear":
                    handleClear(userId, req);
                    break;
                default:
                    sendJson(session, error("不支持的消息类型：" + type));
            }
        } catch (BusinessException e) {
            sendJson(session, error(e.getMessage()));
        } catch (Exception e) {
            log.warn("聊天消息处理失败 userId={}: {}", userId, e.toString());
            sendJson(session, error("消息处理失败，请重试"));
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String userId = userId(session);
        if (userId != null) {
            sessions.remove(userId, session);
            publisher.unregisterOnline(userId);
            presenceUpdate();
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.debug("聊天连接异常 userId={}: {}", userId(session), exception.toString());
        try {
            session.close(CloseStatus.SERVER_ERROR);
        } catch (Exception ignored) {
            // 连接已不可用，忽略
        }
    }

    // ==================== 消息处理 ====================

    /** 发送：校验落库 → 回执发送方 → 投递接收方（跨实例广播或本地直发） */
    private void handleSend(String userId, Map<String, Object> req) {
        String to = req.get("to") == null ? null : String.valueOf(req.get("to"));
        String content = req.get("content") == null ? null : String.valueOf(req.get("content"));
        ChatMessage saved = chatService.send(userId, chatService.displayName(userId), to, content);
        Map<String, Object> payload = toPayload(saved);
        sendTo(userId, payload);   // 发送方回执（确认已落库）
        if (publisher.isRemoteEnabled()) {
            publisher.publishMessage(writeJson(payload));   // 跨实例：广播，由接收方实例投递
        } else {
            sendTo(to, payload);   // 单机：本地直发
        }
    }

    /** 历史：查询后回传（查询即标记对方发来的消息已读） */
    private void handleHistory(WebSocketSession session, String userId, Map<String, Object> req) {
        String with = req.get("with") == null ? null : String.valueOf(req.get("with"));
        if (with == null || with.isEmpty()) {
            sendJson(session, error("缺少聊天对象"));
            return;
        }
        int limit = req.get("limit") == null ? ChatService.HISTORY_LIMIT
                : Integer.parseInt(String.valueOf(req.get("limit")));
        List<Map<String, Object>> messages = new ArrayList<>();
        for (ChatMessage m : chatService.history(userId, with, limit)) {
            messages.add(toPayload(m));
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", "history");
        payload.put("with", with);
        payload.put("messages", messages);
        sendJson(session, payload);
    }

    /** 已读回执：把 with → 我 的未读标记为已读 */
    private void handleRead(String userId, Map<String, Object> req) {
        String with = req.get("with") == null ? null : String.valueOf(req.get("with"));
        if (with != null && !with.isEmpty()) {
            chatService.markRead(userId, with);
        }
    }

    /** 清空与某人的聊天记录（双向删除），并通知双方界面同步清空 */
    private void handleClear(String userId, Map<String, Object> req) {
        String with = req.get("with") == null ? null : String.valueOf(req.get("with"));
        chatService.clearConversation(userId, with);
        // 发起方收到确认（with = 对方）
        Map<String, Object> ack = new LinkedHashMap<>();
        ack.put("type", "cleared");
        ack.put("with", with);
        sendTo(userId, ack);
        // 对方在线则同步清空（其视角的 with = 我）
        Map<String, Object> notify = new LinkedHashMap<>();
        notify.put("type", "cleared");
        notify.put("with", userId);
        if (publisher.isRemoteEnabled()) {
            publisher.publishMessage(writeJson(notify));
        } else {
            sendTo(with, notify);
        }
    }

    /** 跨实例消息到达：投递给本机接收方（由 ChatEventListener 调用） */
    public void handleRemoteMessage(String json) {
        try {
            Map<String, Object> payload = objectMapper.readValue(json,
                    new TypeReference<Map<String, Object>>() { });
            String to = payload.get("to") == null ? null : String.valueOf(payload.get("to"));
            sendTo(to, payload);
        } catch (Exception e) {
            log.debug("跨实例聊天消息解析失败: {}", e.toString());
        }
    }

    /** 跨实例在线状态变更：向本机客户端推送最新在线集合（由 ChatEventListener 调用） */
    public void handleRemotePresence() {
        pushPresenceLocal();
    }

    // ==================== 投递工具 ====================

    /** 在线状态更新：多实例走 Pub/Sub 广播（各实例收到后推送本地），单机直接推送 */
    private void presenceUpdate() {
        if (publisher.isRemoteEnabled()) {
            publisher.publishPresenceChange();
        } else {
            pushPresenceLocal();
        }
    }

    /** 向本机客户端广播在线用户集合（来自全局在线集合） */
    private void pushPresenceLocal() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", "presence");
        payload.put("users", new ArrayList<>(publisher.onlineUserIds()));
        for (WebSocketSession session : sessions.values()) {
            if (session.isOpen()) {
                sendJson(session, payload);
            }
        }
    }

    private void sendTo(String userId, Map<String, Object> payload) {
        if (userId == null) {
            return;
        }
        WebSocketSession session = sessions.get(userId);
        if (session != null && session.isOpen()) {
            sendJson(session, payload);
        }
    }

    private void sendJson(WebSocketSession session, Object payload) {
        try {
            String json = writeJson(payload);
            // 同一连接并发写可能触发 TEXT_PARTIAL_WRITING，按会话串行化发送
            synchronized (session) {
                if (session.isOpen()) {
                    session.sendMessage(new TextMessage(json));
                }
            }
        } catch (Exception e) {
            log.debug("聊天消息投递失败: {}", e.toString());
        }
    }

    private String writeJson(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            log.warn("聊天消息序列化失败: {}", e.toString());
            return "{}";
        }
    }

    private Map<String, Object> toPayload(ChatMessage m) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("type", "chat");
        map.put("id", m.getId());
        map.put("from", m.getFromUser());
        map.put("fromName", m.getFromName());
        map.put("to", m.getToUser());
        map.put("toName", m.getToName());
        map.put("content", m.getContent());
        map.put("time", m.getCreateTime() == null ? "" : TIME_FMT.format(m.getCreateTime()));
        return map;
    }

    private Map<String, Object> error(String message) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("type", "error");
        map.put("message", message);
        return map;
    }

    private String userId(WebSocketSession session) {
        Object value = session.getAttributes().get(ChatHandshakeInterceptor.ATTR_USER_ID);
        return value == null ? null : String.valueOf(value);
    }

    private void closeQuietly(WebSocketSession session, CloseStatus status) {
        try {
            session.close(status);
        } catch (Exception ignored) {
            // 关闭失败忽略
        }
    }
}
