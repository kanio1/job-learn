package lab.paymentquality.ops.internal.infrastructure;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import lab.paymentquality.ops.OpsFeedFrame;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

@Component
public class OpsFeedBroker {

    private static final int RECENT_LIMIT = 100;

    private final ConcurrentHashMap<String, Set<WebSocketSession>> sessionsBySubject = new ConcurrentHashMap<>();
    private final ConcurrentLinkedDeque<OpsFeedFrame> recent = new ConcurrentLinkedDeque<>();
    private final ObjectMapper objectMapper = new ObjectMapper()
            .findAndRegisterModules()
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    public OpsFeedBroker() {
    }

    public void register(WebSocketSession session) {
        sessionsBySubject.computeIfAbsent(subjectOf(session), ignored -> ConcurrentHashMap.newKeySet()).add(session);
    }

    public void unregister(WebSocketSession session) {
        String subject = subjectOf(session);
        Set<WebSocketSession> sessions = sessionsBySubject.get(subject);
        if (sessions != null) {
            sessions.remove(session);
            if (sessions.isEmpty()) {
                sessionsBySubject.remove(subject, sessions);
            }
        }
    }

    public void recordAndBroadcast(OpsFeedFrame frame) {
        recent.addFirst(frame);
        while (recent.size() > RECENT_LIMIT) {
            recent.pollLast();
        }
        String json = writeJson(frame);
        broadcast(json, frame.merchantId());
    }

    public void broadcastRaw(String rawPayload) {
        broadcast(rawPayload, null);
    }

    public List<OpsFeedFrame> recent(UUID merchantFilter) {
        List<OpsFeedFrame> frames = new ArrayList<>();
        for (OpsFeedFrame frame : recent) {
            if (merchantFilter != null && !merchantFilter.equals(frame.merchantId())) {
                continue;
            }
            frames.add(frame);
            if (frames.size() >= 50) {
                break;
            }
        }
        return List.copyOf(frames);
    }

    public void disconnect(String subject) {
        Set<WebSocketSession> sessions = sessionsBySubject.remove(subject);
        if (sessions == null) {
            return;
        }
        for (WebSocketSession session : sessions) {
            try {
                if (session.isOpen()) {
                    session.close(CloseStatus.NORMAL);
                }
            } catch (IOException ignored) {
                // caller already asked to drop the socket
            }
        }
    }

    private void broadcast(String payload, UUID merchantFilter) {
        TextMessage message = new TextMessage(payload);
        for (Set<WebSocketSession> sessions : sessionsBySubject.values()) {
            for (WebSocketSession session : sessions) {
                if (!session.isOpen()) {
                    continue;
                }
                UUID scopedMerchant = merchantIdOf(session);
                if (scopedMerchant != null && merchantFilter != null && !scopedMerchant.equals(merchantFilter)) {
                    continue;
                }
                try {
                    synchronized (session) {
                        session.sendMessage(message);
                    }
                } catch (IOException ignored) {
                    // drop a stale socket; reconnect loads GET /recent
                }
            }
        }
    }

    private String writeJson(OpsFeedFrame frame) {
        try {
            return objectMapper.writeValueAsString(frame);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize ops feed frame", e);
        }
    }

    static String subjectOf(WebSocketSession session) {
        Principal principal = session.getPrincipal();
        if (principal instanceof JwtAuthenticationToken jwtAuthentication) {
            String subject = jwtAuthentication.getToken().getSubject();
            if (subject != null && !subject.isBlank()) {
                return subject;
            }
        }
        return principal == null ? "anonymous" : principal.getName();
    }

    static UUID merchantIdOf(WebSocketSession session) {
        if (!(session.getPrincipal() instanceof JwtAuthenticationToken jwtAuthentication)) {
            return null;
        }
        String claim = jwtAuthentication.getToken().getClaimAsString("merchant_id");
        if (claim == null || claim.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(claim.strip());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
