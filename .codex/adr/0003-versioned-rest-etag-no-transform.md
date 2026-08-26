# ADR 0003 — Preserve versioned REST ETags through the Caddy edge

Status: ACCEPTED
Date: 2026-08-26
Deciders: user + Codex implementation session

## Context

Caddy's `encode gzip zstd` represents a compressed response with an ETag such
as `"v0-gzip"`. The browser correctly keeps this opaque value, and Nitro forwards
it unchanged in `If-Match`. Spring's versioned REST resources deliberately accept
only their origin marker (`"v0"`), so a lifecycle mutation through the TLS edge
returned `400`.

## Decision

Every response that emits an optimistic-concurrency ETag also emits
`Cache-Control: no-transform` (payments and tenant settings retain `no-store`).
Caddy continues to compress ordinary UI and unversioned API responses, but must
not transform a versioned representation, preserving its origin ETag for
`If-Match`.

The Nuxt BFF and browser keep `ETag` and `If-Match` opaque and forward them
unchanged. No suffix stripping, alternate request header, or proxy-specific
Spring ETag grammar is introduced.

## Alternatives rejected

- Strip `-gzip` / `-zstd` in the frontend or BFF: transport-specific, brittle,
  and violates ETag opacity.
- Accept Caddy suffixes in Spring: makes the domain contract proxy-dependent
  and could incorrectly accept malformed validators.
- Disable compression globally: unnecessarily harms UI responses and masks the
  ownership boundary.

## Consequences

- Tests cover origin headers, BFF forwarding, and the browser-facing TLS
  lifecycle chain: unmodified ETag → `If-Match` → successful mutation.
- New ETag-bearing resources must include `no-transform`; non-versioned
  responses keep their existing cache behaviour.
