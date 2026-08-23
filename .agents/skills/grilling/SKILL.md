---
name: grilling
description: Interview the user relentlessly about a plan, decision, or idea until every branch of the design tree is resolved. Use when the user wants to stress-test thinking, or another skill needs an interview primitive.
---

# Grilling

Interview until you reach a shared understanding. Map this as a **design tree**: every decision branches into the decisions that hang off it.

Work the tree in **rounds**. The **frontier** is every decision whose prerequisites are already settled. Ask the whole frontier in one round: number each question and give your recommended answer. Then wait.

```
❓ **Q1** - **<question title>**: <body, including choices>

➡️ <your recommended answer>
```

Each round reshapes the tree. A question that depends on another still-open question belongs to a later round.

Finding **facts** is your job (read the repo, specs, tests). The **decisions** are the user's.

## Lab questions that usually belong on the frontier

- Which **seam** is under test: REST Assured HTTP, Playwright REST/BFF, Playwright E2E, or domain unit?
- Which **module** (`merchant`, `payment`, `tenant`, `iam`) owns the change?
- Does this need a Kiro spec, a `.codex` continuation note, or a small lesson-sized slice?
- Which authorities and tenant/merchant ownership rules apply?
- What is explicitly out of scope (see `AGENTS.md`: PSP, settlement, KYC; Kafka only in `eventlab` overlay)?

Do not act until the user confirms shared understanding. The frontier is empty only when nothing is silently assumed.
