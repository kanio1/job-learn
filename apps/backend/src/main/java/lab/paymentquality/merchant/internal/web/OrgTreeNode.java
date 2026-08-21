package lab.paymentquality.merchant.internal.web;

public record OrgTreeNode(
        String id,
        String type,
        String label,
        String reference,
        boolean lazy
) {
}
