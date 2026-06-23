/**
 * Root package for the Payment Quality Lab black-box API test framework.
 *
 * <p>This module tests the {@code payment-quality-lab} backend through HTTP only.
 * No backend application classes are imported; all DTOs are local test-side records.
 *
 * <p>Four layers (inner → outer):
 * <ul>
 *   <li>{@code core/} — HTTP plumbing, auth, context, problem contract, data helpers</li>
 *   <li>{@code api/} — per-resource thin API clients (DSL over REST Assured)</li>
 *   <li>{@code support/} — JUnit extensions, lifecycle hooks, async polling</li>
 *   <li>{@code scenarios/} — what we test: scenario classes (*Spec, *Test)</li>
 * </ul>
 */
package lab.paymentquality.apitest;
