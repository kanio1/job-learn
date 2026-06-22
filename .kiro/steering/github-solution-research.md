---
inclusion: manual
---

# GitHub Solution Research

Use GitHub as a problem-solving evidence source when a concrete engineering problem,
integration failure, dependency issue, unclear API usage, implementation blocker, or
tool/capability decision may already have a proven open-source solution or precedent.

This is an evidence discipline, not an auto-fix. Kiro uses its web_search and
web_fetch tools, plus `gh` CLI via execute_bash when available, to find GitHub
evidence. Every answer must include citations, rejected candidates, and a
verification step. No links-only answers. No "common pattern" claims without evidence.

---

## When to Use This

Invoke with `#github-solution-research` in chat when:

- A build, test, deploy, dependency, framework, or integration error may have
  open-source precedent (issues, PRs, release notes, examples).
- A feature implementation is blocked by an unclear edge case, missing API usage
  pattern, or version-specific behavior.
- You need to compare multiple GitHub projects (Stars, license, activity,
  adaptation cost) before choosing a library or approach.
- You want to know whether your Spring Boot 4 / Spring Modulith / Keycloak 26 /
  PostgreSQL 18 / Nuxt 4 problem has been solved somewhere before designing
  a local solution.

Do NOT use it for:
- Bugs already visible in local code and logs — check local files first.
- Writing Playwright tests or REST Assured test bodies.
- Official API questions where Spring, Keycloak, PostgreSQL, or Nuxt docs are
  the right primary source.
- Production incident triage before local logs and Actuator output are checked.
- Anything involving private repositories, JWT secrets, Keycloak credentials,
  database passwords, or any sensitive project data.

---

## Research Workflow

### Step 1 — Frame the problem locally first

Before searching GitHub, capture:
- Goal and actual symptom or error
- Exact error text, exception class, or failing command output
- Versions: Spring Boot, Spring Framework, JDK, PostgreSQL, Nuxt, Keycloak, etc.
- Recent changes (migration, dependency bump, config change)
- What you've already tried
- Local constraints (module boundaries, auth model, DB schema, security rules)

If any of these facts can be found in local project files, read those files first.

### Step 2 — Choose the evidence mode

| Situation | Priority surface |
|---|---|
| Error or regression | Issues → PRs → release notes → code |
| API usage / config pattern unclear | Code → examples → issues |
| Choosing a library or approach | Repository candidates → issues → PRs |
| Implementation blocker (feature) | Repository candidates + code + issues |

### Step 3 — Search with `gh` CLI first, web tools second

Use `gh` via `execute_bash` as the default. Fall back to `remote_web_search` +
`web_fetch` when `gh` is unavailable or rate-limited.

```bash
# Repository candidates
gh search repos "<query>" --archived=false --sort stars --order desc --limit 10 \
  --json fullName,url,description,stargazersCount,forksCount,language,license,pushedAt,isArchived

# Issues (errors, regressions)
gh search issues "<query>" --sort updated --order desc --limit 10 \
  --json title,url,state,updatedAt,commentsCount,repository

# Merged PRs (fixes)
gh search prs "<query>" --merged --sort updated --order desc --limit 10 \
  --json title,url,state,updatedAt,commentsCount,repository

# Code examples
gh search code "<query>" --limit 10 \
  --json path,url,repository

# Deep-read a repo
gh repo view owner/repo \
  --json nameWithOwner,url,description,stargazerCount,forkCount,licenseInfo,primaryLanguage,pushedAt
```

Only run `gh auth status` if a command returns 403 or an explicit auth error.
Never print or log tokens.

### Step 4 — Rank by problem fit

Scoring priority order (highest → lowest):

1. Exact match: same error text, same framework version, same config, same runtime
2. Maintainer-confirmed issue, merged PR, released fix, official example
3. Reproducible code with matching stack
4. Repeated independent reports on matching version range
5. Stars/forks (maturity signal and tie-breaker only — NOT a proxy for correctness)

**Version recency rule for this project:** Demote any evidence targeting Spring Boot
≤3.x, JDK ≤21, Nuxt ≤3.x, Keycloak ≤24.x, or PostgreSQL ≤16 unless the issue
is explicitly confirmed to still apply to the current version. Always note the
evidence version and whether it has been confirmed on the current version.

### Step 5 — Deep-read the strongest candidates

For each shortlisted repository or evidence item, capture:

- Repo name, URL, Stars, forks, language, license, last activity, archived status
- What the project actually does (not just its tagline)
- Why it matches the specific local problem
- The reusable part: config shape, API pattern, test fixture, workflow, migration script
- Adaptation cost: what must change for this project's constraints vs. yours
- Risk: stale version, unresolved issue, license obligation, security concern

### Step 6 — Translate to local work

Output must say:
- **Reuse**: what from GitHub can be used as-is (workflow, config, API pattern)
- **Adapt**: what must change (your module structure, auth model, DB schema, Spring Modulith boundaries)
- **Avoid**: specific parts that don't fit or carry risk
- **Verify**: the exact command, test, request, or manual check that confirms it works

### Step 7 — If evidence is weak, say so

If no strong GitHub match exists:
- Label the recommendation "local-only / first-principles"
- Do not stretch a weak keyword match into a confident recommendation
- Suggest official docs as the next step

---

## Required Output Format

Every GitHub research answer must include:

1. **Local problem profile** — goal, symptom, versions, constraints
2. **Search path** — queries used, surfaces searched, tools used
3. **Project candidates** (when a repo itself is the solution) — compact table:
   | Repo | Stars | License | Last active | Fit | Key risk |
4. **Key evidence** — direct links to issues, PRs, code, examples, releases
5. **Recommended solution** — reuse / adapt / avoid / verify
6. **Rejected options** — what was considered and why it doesn't fit
7. **Verification standard** — test command, build check, or manual step
8. **Confidence level** — High / Medium / Low / Local-only, with brief reason

No link-only answers. No "common GitHub pattern" without a linked example.
No recommendation without a verification step.

---

## Safety Boundaries

- Public repositories only. Never access private repos, org-internal repos, or
  any repository scoped to private credentials without explicit user authorization.
- Never include JWT secrets, Keycloak admin credentials, database passwords,
  bearer tokens, or any secret value in search queries, prompts, or outputs.
- Prefer reading and adapting patterns over copying code verbatim. When code
  reuse is necessary, verify the license and note any attribution obligation.
  MIT is fine. GPL/AGPL requires explicit review before use.
- For security, auth, payment, and infrastructure topics: cross-check GitHub
  findings against current official docs (Spring Security, Keycloak, PostgreSQL,
  Nuxt). Open-source examples may lag behind security advisories.
- Do not claim a fix is correct based only on Stars or community upvotes.
  Require a reproducible verification step.

---

## Stack-Specific Query Hints

Use these as starting points, adjusting for the actual error or problem:

| Problem area | Good `gh` query shape |
|---|---|
| Spring Boot 4 migration | `spring-boot 4 migration "spring.security"` |
| Spring Modulith boundary | `spring-modulith internal package import violation` |
| Keycloak 26 realm config | `keycloak 26 realm import "client-secret"` |
| PostgreSQL 18 RLS multi-tenant | `postgresql "row level security" spring jpa` |
| Testcontainers parallel isolation | `testcontainers postgresql flyway parallel "junit 5"` |
| REST Assured 6 + JUnit 6 | `rest-assured 6 junit jupiter spring boot` |
| Nuxt 4 `$fetch.raw` headers | `nuxt 4 "$fetch.raw" ETag response headers` |
| Playwright storageState auth | `playwright "storageState" keycloak oidc setup` |
| GitHub Actions Maven + pnpm | `github-actions maven pnpm monorepo` |
