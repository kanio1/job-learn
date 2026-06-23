/**
 * HTTP plumbing layer — Phase 2 and Phase 3.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>{@code Headers} / {@code ContentTypes} — string constants, no logic</li>
 *   <li>{@code ApiConfig} — base URI resolution from environment</li>
 *   <li>{@code RestAssuredSetup} — immutable BASE spec, parser registration, encoder config</li>
 *   <li>{@code RequestSpecs} — factory methods returning isolated spec copies</li>
 *   <li>{@code ResponseSpecs} — reusable response contracts (Vary, Cache-Control, ETag)</li>
 *   <li>{@code AuthFilter} / {@code CorrelationFilter} — cross-cutting HTTP concerns (Phase 3)</li>
 * </ul>
 *
 * <p>SDET concept: separating "how to speak HTTP" from "what to assert" keeps scenarios readable.
 */
package lab.paymentquality.apitest.core.http;
