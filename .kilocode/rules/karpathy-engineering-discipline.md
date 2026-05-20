# Karpathy-Inspired Engineering Discipline

This rule adapts the useful parts of `multica-ai/andrej-karpathy-skills` for the Payment Quality Engineering Lab. The source repository declares MIT licensing in its README and skill metadata; this file is a project-specific adaptation, not a verbatim copy.

Apply this rule to non-trivial code, test, documentation, configuration, and agent-workflow changes. For trivial typo fixes or obvious one-line edits, keep the spirit of the rule without adding process noise.

## Think Before Coding

- Surface assumptions before changing files; do not silently choose one interpretation when the task is ambiguous.
- Name material ambiguities, missing inputs, and trade-offs early.
- Push back when the requested path conflicts with project scope, Phase 0 guardrails, maintainability, or testability.
- Prefer a brief implementation intent plus verification checks over hidden reasoning.

## Simplicity First

- Solve the asked problem, not a speculative future version of it.
- Avoid new abstractions, configurability, frameworks, or extension points unless the current requirement needs them.
- Prefer the smallest correct implementation that remains readable for a learner studying the repository later.
- Reject fake completeness: no empty business modules, decorative architecture, or placeholder flows that imply behavior not yet specified.

## Surgical Changes

- Touch only files and lines that are traceable to the task.
- Do not refactor, reformat, rename, or tidy unrelated code while solving a local problem.
- Match the surrounding style even when a different style is personally preferred.
- Remove only artifacts made obsolete by your own change; mention unrelated dead code or cleanup opportunities instead of deleting them.

## Goal-Driven Execution

- Convert work into observable outcomes before implementation: what will be true when this is done?
- For bugs, try to reproduce or characterize the failure, fix the cause, then verify the same path.
- For refactors, establish baseline verification before the change when feasible, then verify afterward.
- For multi-step work, keep an explicit plan with checks tied to each meaningful milestone.
- If verification is skipped or impossible, state the reason and the residual risk.

## Educational Fit

- Leave code, tests, and docs understandable to a Senior QA Automation/SDET learner, not just acceptable to an agent.
- Make important design choices visible in names, structure, tests, or concise documentation.
- Do not hide magic decisions behind automation; when automation creates artifacts, ensure the resulting files can be studied by a human.
- Prefer examples and verification commands that teach how the system behaves without introducing payment business functionality ahead of specification.
