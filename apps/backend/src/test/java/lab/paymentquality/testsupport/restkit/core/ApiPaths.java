package lab.paymentquality.testsupport.restkit.core;

public final class ApiPaths {

    private static final String PAYMENT_ORDERS_COLLECTION = "/api/merchants/{merchantId}/payment-orders";
    private static final String PAYMENT_ORDER = PAYMENT_ORDERS_COLLECTION + "/{paymentOrderId}";
    private static final String PAYMENT_ORDER_SUMMARY = PAYMENT_ORDERS_COLLECTION + "/summary";
    private static final String PAYMENT_ORDER_HISTORY = PAYMENT_ORDER + "/history";
    private static final String MERCHANTS = "/api/merchants";

    private ApiPaths() {
    }

    public static String paymentOrdersCollection() {
        return PAYMENT_ORDERS_COLLECTION;
    }

    public static String paymentOrder() {
        return PAYMENT_ORDER;
    }

    public static String paymentOrderSummary() {
        return PAYMENT_ORDER_SUMMARY;
    }

    public static String paymentOrderHistory() {
        return PAYMENT_ORDER_HISTORY;
    }

    public static String paymentOrderLifecycleAction(String action) {
        return PAYMENT_ORDER + "/" + action;
    }

    public static String merchants() {
        return MERCHANTS;
    }
}
