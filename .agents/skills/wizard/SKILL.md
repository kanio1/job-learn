---
name: wizard
description: Generate an interactive bash wizard for steps only a human can perform — Keycloak admin, compose secrets, mkcert/TLS, third-party dashboards, one-off cutovers. Do not use for steps the agent can run itself.
---

# Wizard

A **wizard** is a bash script that walks a human through a manual procedure. Copy [template.sh](template.sh); do **not** edit the library above the `STAGES` marker.

Ephemeral by default — write under `.codex/wizards/<slug>.sh` and delete when done. Commit under `scripts/` only when the user wants a repeatable setup path.

## Lab triggers

- Keycloak realm/client/redirect URI in the admin console
- `infra/compose/.env` values the human must copy
- mkcert / browser trust store
- GitHub Actions secrets (optional; local `.env` is the default write target)
- One-off cutover the agent cannot click through

If the agent can do the step with tools, it should — this skill is HITL only.

## Process

1. Read `.env.example`, compose files, `docs/setup/`, and CI `secrets.*` / `vars.*`. List stages and captured values. Confirm.
2. Map each stage to a real URL/path. Never invent a dashboard click path — scrape current docs with Firecrawl if unsure.
3. Copy `template.sh`. Author `stage`s. Default `ENV_FILE` to the actual env path (`infra/compose/.env` when that is the target). Prefer `write_env` over `set_secret` unless CI truly needs it.
4. `bash -n`, `chmod +x`. Do **not** run it end-to-end (it blocks on humans). Tell the user the command.

Do not put passwords into git. Wizards must not echo secrets.
