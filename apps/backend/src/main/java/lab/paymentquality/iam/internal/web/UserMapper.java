package lab.paymentquality.iam.internal.web;

import lab.paymentquality.iam.internal.domain.ManagedUser;
import lab.paymentquality.iam.internal.web.dto.UserDetail;
import lab.paymentquality.iam.internal.web.dto.UserSummary;

public final class UserMapper {

    private UserMapper() {
    }

    public static UserSummary toSummary(ManagedUser user) {
        return new UserSummary(
                user.id(),
                user.username(),
                user.email(),
                user.enabled(),
                user.tenantId(),
                user.merchantId(),
                user.roles());
    }

    public static UserDetail toDetail(ManagedUser user) {
        return new UserDetail(
                user.id(),
                user.username(),
                user.email(),
                user.enabled(),
                user.tenantId(),
                user.merchantId(),
                user.roles());
    }
}
