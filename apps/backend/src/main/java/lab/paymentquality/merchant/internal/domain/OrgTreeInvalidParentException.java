package lab.paymentquality.merchant.internal.domain;

public class OrgTreeInvalidParentException extends RuntimeException {

    public OrgTreeInvalidParentException(String parent) {
        super("Invalid org-tree parent: " + parent);
    }
}
