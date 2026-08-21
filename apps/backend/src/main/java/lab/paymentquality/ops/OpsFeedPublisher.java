package lab.paymentquality.ops;

/**
 * Public seam for broadcasting a live operations frame.
 * Other modules must not import {@code lab.paymentquality.ops.internal}.
 */
public interface OpsFeedPublisher {

    void publish(OpsFeedFrame frame);

    void publishRaw(String rawPayload);
}
