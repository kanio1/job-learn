# Prompt: Continue Tenant Model and Isolation — Wave 2

Use this prompt in Codex CLI from the repository root on branch `018-rest-security-p1-error-auth-method-hardening`.

```text
You are continuing the Payment Quality Engineering Lab on branch 018-rest-security-p1-error-auth-method-hardening.

Read first:
1. AGENTS.md
2. .codex/current-state.md
3. .codex/tenant-model-and-isolation.md
4. .codex/review-checklist.md
5. .kiro/specs/tenant-model-and-isolation/requirements.md
6. .kiro/specs/tenant-model-and-isolation/design.md
7. .kiro/specs/tenant-model-and-isolation/tasks.md

Important rules:
- Do not modify any .kiro file.
- Treat .kiro as read-only requirements/design/task source.
- Use .codex/current-state.md as the mutable execution-status overlay.
- Do not modify payment module source files for this tenant isolation wave.
- Do not add frontend, Playwright, Kafka, PSP, settlement, reconciliation, KYC, or dashboard work.
- Preserve all existing endpoint paths, authority strings, @PreAuthorize annotations, REST status codes, and headers unless the tenant spec explicitly requires an additive change.

Current checkpoint:
- Wave 0 and Wave 1 are already implemented.
- ./mvnw test was reported green after Wave 1.
- MerchantService still has the Wave 1 placeholder tenant bridge and must be made tenant-aware in Wave 2.

Implement only Wave 2:
- 5.1 Extend JpaMerchantRepository with tenant-filtered query methods.
- 5.2 Add merchant-layer tenant exceptions.
- 5.3 Add optional tenantReference to CreateMerchantRequest.
- 5.4 Extend MerchantService with TenantContext-based create/read/list/activate/suspend behavior.

Known design gap:
- Current TenantResolver has only resolve(Jwt).
- Wave 2 needs platform create to resolve a request-body tenantReference to a tenant UUID or TenantContext.
- Solve this by adding the smallest public tenant module API extension under lab.paymentquality.tenant, implemented inside TenantResolverService.
- Do not import tenant.internal.* from merchant.

After implementation:
- Run cd apps/backend && ./mvnw test.
- Fix compile or test failures within Wave 2 scope.
- Update .codex/current-state.md with what changed and verification result.
- Stop before Wave 3 unless explicitly asked to continue.
```
