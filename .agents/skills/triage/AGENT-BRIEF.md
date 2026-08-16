# Writing agent briefs

An agent brief is the contract a later `implement` session works from. Original discussion is context; the brief is the spec.

## Principles

- Behavioural, not procedural. Interfaces and contracts, not file paths or line numbers.
- Durable: still useful after refactors.
- Complete, testable acceptance criteria.
- Explicit out of scope (include standing lab non-goals unless expanded).
- Name **seams**: REST Assured / Playwright REST / Playwright E2E / unit.

## Template

```markdown
## Agent Brief

**Category:** bug / enhancement
**Summary:** one line

**Current behavior:**

**Desired behavior:**

**Key interfaces:**
- HTTP contract / module interface / UI state — what must change

**Seams:**
- REST Assured / Playwright REST / Playwright E2E / unit

**Acceptance criteria:**
- [ ] criterion

**Out of scope:**
- PSP, Kafka, settlement, KYC, top-level POST /payments unless explicitly in scope
```
