package com.gt.codinnator.domain.editor.config.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.BinaryWebSocketHandler;

import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
@RequiredArgsConstructor
public class CodeHandler extends BinaryWebSocketHandler {

    // 상태 저장소(roomStates) 삭제! 백엔드는 상태를 모르는 게 낫습니다.
    private final Map<String, Set<WebSocketSession>> roomAttendees = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        URI uri = session.getUri();
        String path = uri.getPath();
        String[] segments = path.split("/");

        if (segments.length < 5) {
            log.error("잘못된 접속 주소입니다: {}", path);
            session.close();
            return;
        }

        String roomId = segments[3];
        String fileId = segments[4];
        String roomKey = roomId + ":" + fileId;

        session.getAttributes().put("roomKey", roomKey);
        roomAttendees.computeIfAbsent(roomKey, k -> ConcurrentHashMap.newKeySet()).add(session);

        log.info("Client Connected: RoomKey={}, SessionID={}, 현재 인원: {}", roomKey, session.getId(), roomAttendees.get(roomKey).size());

        // ⛔ 삭제: 입장 시 백엔드가 저장한 상태를 보내는 로직 제거
        // 이유: Yjs 프로토콜에 따르면 클라이언트끼리 알아서 Sync(동기화)를 맞춥니다.
    }

    @Override
    protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) {
        String roomKey = (String) session.getAttributes().get("roomKey");
        Set<WebSocketSession> attendees = roomAttendees.get(roomKey);

        if (attendees == null || roomKey == null) return;

        // ⛔ 삭제: roomStates.put(...) 로직 제거 (상태 덮어쓰기 방지)

        // 자신을 제외한 나머지 사람들에게 메시지 전송 (Broadcasting)
        // Yjs는 본인이 보낸 메시지를 다시 받으면 안 될 수도 있고, 받아도 무시하지만 트래픽 낭비입니다.
        for (WebSocketSession attendee : attendees) {
//            if (attendee.isOpen() && !attendee.getId().equals(session.getId())) { // 본인 제외
                try {
                    synchronized (attendee) {
                        attendee.sendMessage(message);
                    }
                } catch (IOException e) {
                    log.warn("메시지 전송 실패: {}", attendee.getId());
                }
            }
//        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String roomKey = (String) session.getAttributes().get("roomKey");
        if (roomKey != null) {
            Set<WebSocketSession> attendees = roomAttendees.get(roomKey);
            if (attendees != null) {
                attendees.remove(session);
                if (attendees.isEmpty()) {
                    roomAttendees.remove(roomKey);
                }
            }
        }
        log.info("Client Disconnected: RoomKey={}, SessionID={}", roomKey, session.getId());
    }
}