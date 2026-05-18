# WireMock Conventions

This folder reserves a future location for external-service stub tests.

Phase 0 does not implement PSP mock behavior.

Future WireMock tests should:
- Prefer dynamic ports.
- Isolate stubs and request journals per test or suite.
- Reset only owned mock state.
- Avoid fixed-port assumptions that block parallel execution.
- Keep PSP contract behavior tied to a future PSP feature specification.
