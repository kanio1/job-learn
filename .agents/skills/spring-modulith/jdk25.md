# JDK 25 in this lab

Compiler release is **25**. Spring Boot 4.0 documents Java 17 through 26, so 25 is in range. Do not treat preview JEPs as production API.

## Use (already house style)

- `record` for HTTP DTOs, commands, and small value objects (`CurrencyCode`, `CreateMerchantRequest`, …)
- Text blocks, sealed types, and pattern `switch` when they match neighboring code
- Flexible constructor bodies (JEP 513) when a constructor already needs validation before `this(…)` / `super(…)`
- Virtual threads: **do not** switch the servlet container or add `Executors.newVirtualThreadPerTaskExecutor()` unless a spec asks; nothing in `application.yml` enables them today

## Do not introduce without an explicit request

| Feature | JEP | Why |
|---|---|---|
| Compact source files / instance main | 512 | Not an application class shape this repo uses |
| `import module` | 511 | No `module-info.java`; keep classpath + Modulith packages |
| Scoped Values as request context | 506 | Security/tenant context is Spring Security + existing filters |
| Stable Values, structured concurrency, primitive patterns, PEM encodings, Vector API | 502, 505, 507, 470, 508 | Preview or incubator |

Do not add JPMS `module-info.java` to “be more modular” — Spring Modulith packages are the modularity model.

## Effective Java

Quality literacy and the EJ tracker belong to `java25-effective-java-mentor`. This file only blocks language features that would fight Modulith, Spring, or the existing style.
