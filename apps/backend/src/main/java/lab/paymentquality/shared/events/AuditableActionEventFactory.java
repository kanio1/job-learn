package lab.paymentquality.shared.events;

import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public final class AuditableActionEventFactory {

    private static final String PLATFORM_TENANT_REFERENCE = "PLATFORM_TENANT";
    private static final String SYSTEM_ACTOR = "system";

    private AuditableActionEventFactory() {
    }

    public static AuditableActionOccurred success(
            String action,
            String targetType,
            String targetId,
            String tenantReference) {
        return success(action, targetType, targetId, tenantReference, null, null);
    }

    public static AuditableActionOccurred success(
            String action,
            String targetType,
            String targetId,
            String tenantReference,
            String actorSubject,
            String correlationId) {
        return success(action, targetType, targetId, tenantReference, actorSubject, correlationId, null, null);
    }

    /**
     * Variant carrying field-level before/after state for the audit diff
     * drawer (F-D7). Pass null for either map when there is nothing
     * meaningful to diff (e.g. creation has no beforeState).
     */
    public static AuditableActionOccurred success(
            String action,
            String targetType,
            String targetId,
            String tenantReference,
            String actorSubject,
            String correlationId,
            Map<String, Object> beforeState,
            Map<String, Object> afterState) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String resolvedSubject = valueOrFallback(actorSubject, subject(authentication));
        String actorDisplay = display(authentication, resolvedSubject);
        String resolvedTenant = valueOrFallback(tenantReference, tenantReference(authentication));
        String resolvedCorrelationId = valueOrFallback(correlationId, MDC.get("correlationId"));

        return new AuditableActionOccurred(
                Instant.now(),
                resolvedSubject,
                actorDisplay,
                action,
                targetType,
                targetId,
                valueOrFallback(resolvedTenant, PLATFORM_TENANT_REFERENCE),
                valueOrFallback(resolvedCorrelationId, UUID.randomUUID().toString()),
                Outcome.SUCCESS,
                beforeState,
                afterState);
    }

    private static String subject(Authentication authentication) {
        if (authentication instanceof JwtAuthenticationToken jwtAuthentication) {
            return valueOrFallback(
                    jwtAuthentication.getToken().getSubject(),
                    valueOrFallback(authentication.getName(), SYSTEM_ACTOR));
        }
        return authentication == null ? SYSTEM_ACTOR : valueOrFallback(authentication.getName(), SYSTEM_ACTOR);
    }

    private static String display(Authentication authentication, String actorSubject) {
        if (authentication instanceof JwtAuthenticationToken jwtAuthentication) {
            return valueOrFallback(
                    jwtAuthentication.getToken().getClaimAsString("preferred_username"),
                    valueOrFallback(authentication.getName(), actorSubject));
        }
        return authentication == null ? actorSubject : valueOrFallback(authentication.getName(), actorSubject);
    }

    private static String tenantReference(Authentication authentication) {
        if (authentication instanceof JwtAuthenticationToken jwtAuthentication) {
            return jwtAuthentication.getToken().getClaimAsString("tenant_id");
        }
        return PLATFORM_TENANT_REFERENCE;
    }

    private static String valueOrFallback(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.strip();
    }
}
