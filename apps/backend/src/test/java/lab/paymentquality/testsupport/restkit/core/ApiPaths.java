package lab.paymentquality.testsupport.restkit.core;

public final class ApiPaths {

    private static final String PAYMENT_ORDERS_COLLECTION = "/api/merchants/{merchantId}/payment-orders";
    private static final String PAYMENT_ORDER = PAYMENT_ORDERS_COLLECTION + "/{paymentOrderId}";
    private static final String PAYMENT_ORDER_SUMMARY = PAYMENT_ORDERS_COLLECTION + "/summary";

    private ApiPaths() {
    }

    public static String paymentOrdersCollection() {
        return PAYMENT_ORDERS_COLLECTION;
    }

    public static String paymentOrder() {
        return PAYMENT_ORDER;
    }

    public static String paymentOrders() {
        return PAYMENT_ORDER;
    }

    public static String paymentOrderSummary() {
        return PAYMENT_ORDER_SUMMARY;
    }
}
