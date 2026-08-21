package lab.paymentquality.ops.internal.domain;

public class OpsNotificationNotFoundException extends RuntimeException {

    public OpsNotificationNotFoundException(String notificationId) {
        super("Notification not found: " + notificationId);
    }
}
