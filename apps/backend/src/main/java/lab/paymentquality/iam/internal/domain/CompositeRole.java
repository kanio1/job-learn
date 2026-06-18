package lab.paymentquality.iam.internal.domain;

import java.util.Set;

public enum CompositeRole {
    PLATFORM_ADMIN,
    TENANT_ADMIN,
    MERCHANT_MANAGER,
    SUPPORT_AGENT,
    READ_ONLY_USER;

    private static final Set<String> ASSIGNABLE_NAMES = Set.of(
            PLATFORM_ADMIN.name(),
            TENANT_ADMIN.name(),
            MERCHANT_MANAGER.name(),
            SUPPORT_AGENT.name(),
            READ_ONLY_USER.name());

    public static boolean isAssignable(String name) {
        return name != null && ASSIGNABLE_NAMES.contains(name);
    }
}
