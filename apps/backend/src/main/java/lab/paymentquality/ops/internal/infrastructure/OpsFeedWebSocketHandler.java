package lab.paymentquality.ops.internal.infrastructure;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Component
public class OpsFeedWebSocketHandler extends TextWebSocketHandler {

    private final OpsFeedBroker broker;

    public OpsFeedWebSocketHandler(OpsFeedBroker broker) {
        this.broker = broker;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        broker.register(session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        broker.unregister(session);
    }
}
