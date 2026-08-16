# Logic prototype

One self-contained HTML file. Domain language on buttons and state. No framework, no bundler.

## Process

1. State the question in a visible intro.
2. Isolate logic in a pure `<script>` module (reducer or state machine). No DOM inside it.
3. Layout: title → current state panel → free-play buttons → tabbed guided scenarios (happy path, illegal transition, awkward edge).
4. Hand the file over. "That shouldn't be possible" is the point.
5. Lift the validated machine into the real module; keep the HTML under `.codex/prototypes/`.

## Anti-patterns

No tests, no real database, no generalisation, no mixing DOM into the logic module, no shipping the HTML shell to production.
