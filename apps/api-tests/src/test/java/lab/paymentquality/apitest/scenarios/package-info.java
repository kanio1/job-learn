/**
 * Scenario layer — Phase 7+ (deferred).
 *
 * <p>Contains *Spec classes run by maven-failsafe-plugin against the live stack:
 * <ul>
 *   <li>{@code payment/PaymentLifecycleSpec} — full CREATED→AUTHORIZED→CAPTURED lifecycle</li>
 *   <li>{@code payment/IdempotencyReplaySpec} — 201 on create, 200 on replay, body equality</li>
 *   <li>{@code payment/OptimisticLockingSpec} — concurrent mutate → 412 on stale ETag</li>
 *   <li>{@code tenant/TenantIsolationSpec} — cross-tenant read=404, write=403</li>
 *   <li>{@code audit/AuditTrailSpec} — audit events via Eventually polling</li>
 * </ul>
 *
 * <p>No *Spec class runs via {@code mvn test}; all require {@code mvn verify} with
 * {@code BACKEND_IMAGE} env set and Podman/Docker available.
 */
package lab.paymentquality.apitest.scenarios;
