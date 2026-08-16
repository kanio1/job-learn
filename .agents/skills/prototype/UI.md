# UI prototype

Several **structurally different** variants on one route, switched with `?variant=` and a floating bar.

Prefer **sub-shape A**: existing Nuxt admin page (merchants list, payment detail, refund approvals). Keep real data fetching; swap only the rendered subtree. Use Nuxt UI already in the app.

**Sub-shape B** (new `/prototype/...` route) only when there is no host page.

## Process

1. Default **3** variants, cap 5. Write the plan in a top-of-file comment.
2. Variants must disagree on layout / hierarchy / primary affordance — not colour.
3. Switcher: `?variant=`, arrows + keyboard, hidden from production (`import.meta.dev` or equivalent).
4. User often wants "header from B, sidebar from C" — that is the answer.
5. Fold the winner into the real page under `implement`/`tdd`; do not promote prototype code as-is.

Do not wire variants to real mutations (authorize/refund). Read-only or in-memory stubs.
