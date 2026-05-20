# Phase 1 Test Design

Phase 1 is rich test-design material because it combines validation, lifecycle state, persistence, security, concurrency, and frontend feedback without introducing payment complexity.

## Techniques Practiced

- BVA and EP for merchant references and display names.
- State transition testing for DRAFT, ACTIVE, and SUSPENDED.
- Authorization matrix for unauthenticated, denied, partial-authority, and full-authority actors.
- Concurrency thinking for duplicate merchant creation.

## Key Risk

A fake or shared merchant would make later payment tests misleading. Phase 1 therefore establishes unique merchant references and durable ownership boundaries early.
