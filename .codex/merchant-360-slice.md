# Merchant 360 — slice overlay

Mutable. Agents update this after each `PW-M360-Txx`. Do not put this content in chat prompts.

| Field | Value |
|---|---|
| Milestone | `playwright-merchant-360` |
| Status | DONE |
| Last closed | `PW-M360-T20` |
| Next | — |
| Blocked | — |
| Last verify | Review-and-fix (all M360 review findings): `./mvnw -Dtest=MerchantIfMatchRestAssuredTest,MerchantModuleTest,EntitySearchRestAssuredTest,ModulithArchitectureTest,PaymentModuleTest test` (BUILD SUCCESS; If-Match 8 tests incl. malformed 400 + concurrent 412). Frontend `corepack pnpm typecheck` + `lint` green. |

Roadmap: `status/roadmaps/playwright-merchant-360/`  
Catalog: `docs/testing/merchant-360-erp-lab/`  
Prompt: `.codex/prompts/merchant-360-implement.md`  
Review: `.codex/prompts/merchant-360-review.md` · skill `merchant-360-review`
