package lab.paymentquality.eventlab.internal;

/**
 * Wave-1 lab topic names. Auto-create is OFF; KafkaAdmin {@code NewTopic} beans
 * and {@code dev-stack.sh --kafka} create these explicitly (RF=1 is lab≠prod).
 */
public final class EventLabTopics {

    public static final String AUDITABLE_ACTIONS = "lab.auditable-actions.v1";
    /** Single retry topic for {@code @RetryableTopic} fixed 500ms backoff. */
    public static final String RETRY = "lab.auditable-actions.v1-retry";
    public static final String DLT = "lab.event-lab.dlq.v1";

    public static final String INSPECTOR_GROUP = "eventlab-inspector";

    private EventLabTopics() {}
}
