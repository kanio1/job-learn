/**
 * Live black-box scenario layer.
 *
 * <p>Classes in this package run through Maven Failsafe during {@code mvn verify} against
 * the Testcontainers-backed backend/PostgreSQL/Keycloak stack. They exercise public HTTP
 * contracts only through the API facades under {@code lab.paymentquality.apitest.api}; no
 * scenario imports backend production classes or uses Spring test infrastructure.
 *
 * <p>Current flat scenario map:
 * <ul>
 *   <li>{@code StatusSpec} — public status smoke.</li>
 *   <li>{@code SecuritySmokeSpec} — unauthenticated and no-role authorization smoke.</li>
 *   <li>{@code MerchantsContractSpec} — merchant create/read/list/lifecycle/error contract.</li>
 *   <li>{@code PaymentOrdersContractSpec} — payment order create/read/list/lifecycle,
 *       ETag/If-Match, history, and core negative boundaries.</li>
 *   <li>{@code LifecycleIdempotencyContractSpec} — authorize/capture/refund replay and
 *       lifecycle idempotency conflict.</li>
 *   <li>{@code PartialRefundContractSpec} — partial refund and refund amount boundaries.</li>
 *   <li>{@code PatchMetadataContractSpec} — JSON Merge Patch and stale ETag contract.</li>
 *   <li>{@code HttpMethodSemanticsContractSpec} — HEAD, OPTIONS, DELETE, and Accept semantics.</li>
 *   <li>{@code JsonSchemaContractSpec} — schema validation for problem, payment order, and summary bodies.</li>
 *   <li>{@code AuditContractSpec} — async audit-event contract with Awaitility.</li>
 *   <li>{@code PaymentSummaryContractSpec} — payment summary/reporting aggregates.</li>
 *   <li>{@code TenantIsolationContractSpec} — merchant/payment tenant isolation, platform bypass,
 *       and tenant-admin boundary.</li>
 * </ul>
 *
 * <p>Behavioral scenario coverage is current through Phase 8M. Later phases 8N through 8S
 * refreshed documentation, added optional JUnit tags, reviewed consistency, and applied targeted
 * assertion/documentation cleanup without changing scenario structure.
 */
package lab.paymentquality.apitest.scenarios;
