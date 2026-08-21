import { mkdir, readdir, readFile, writeFile } from 'node:fs/promises'
import path from 'node:path'
import { spawn } from 'node:child_process'
import { fileURLToPath } from 'node:url'
import { renderMermaid, THEMES } from '@vercel/beautiful-mermaid'

const repoRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../..')
const diagramsDir = path.join(repoRoot, 'docs/testing/diagrams')
const theme = { ...THEMES['vercel-light'], padding: 48 }
const titles = {
  cases: 'All logout paths',
  'deep-sequence': 'Deep Sign out',
  states: 'BFF × SSO states',
}

function splitDiagrams(source, fallbackSlug) {
  const parts = source.split(/^%% diagram: ([a-z0-9-]+)\s*$/m)
  if (parts.length === 1) {
    return [{ slug: fallbackSlug, text: source.trim() }]
  }
  const diagrams = []
  for (let i = 1; i < parts.length; i += 2) {
    const slug = parts[i]
    const text = (parts[i + 1] ?? '').trim()
    if (slug && text) {
      diagrams.push({ slug, text })
    }
  }
  return diagrams
}

function galleryHtml(items) {
  const buttons = items.map((item, index) => (
    `<button type="button" role="tab" aria-selected="${index === 0 ? 'true' : 'false'}" data-pane="${item.slug}">${item.title}</button>`
  )).join('\n        ')
  const panes = items.map((item, index) => (
    `<figure class="pane${index === 0 ? ' is-active' : ''}" data-pane="${item.slug}">
          <img src="${item.file}" alt="${item.title}">
        </figure>`
  )).join('\n        ')
  return `<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>Dual-depth logout</title>
  <style>
    :root {
      --bg: #0a0a0a;
      --fg: #ededed;
      --muted: #888;
      --line: #2a2a2a;
      --card: #141414;
    }
    * { box-sizing: border-box; }
    body {
      margin: 0;
      min-height: 100vh;
      font-family: ui-sans-serif, system-ui, sans-serif;
      background: var(--bg);
      color: var(--fg);
    }
    header {
      max-width: 72rem;
      margin: 0 auto;
      padding: 2rem 1.5rem 1rem;
    }
    h1 { font-size: 1.25rem; font-weight: 600; letter-spacing: -0.02em; margin: 0 0 0.35rem; }
    p { margin: 0; color: var(--muted); font-size: 0.9rem; line-height: 1.45; }
    header a { color: var(--fg); }
    nav[role="tablist"] {
      display: flex;
      gap: 0.35rem;
      max-width: 72rem;
      margin: 0 auto;
      padding: 0 1.5rem 1rem;
    }
    nav button {
      appearance: none;
      border: 1px solid var(--line);
      background: transparent;
      color: var(--muted);
      border-radius: 999px;
      padding: 0.4rem 0.85rem;
      font: inherit;
      font-size: 0.85rem;
      cursor: pointer;
    }
    nav button[aria-selected="true"] {
      background: var(--fg);
      color: var(--bg);
      border-color: var(--fg);
    }
    main { max-width: 72rem; margin: 0 auto; padding: 0 1.5rem 2.5rem; }
    .pane { display: none; margin: 0; background: #fff; border: 1px solid var(--line); border-radius: 12px; overflow: auto; max-height: calc(100vh - 10rem); }
    .pane.is-active { display: block; }
    img { display: block; width: 100%; height: auto; background: #fff; }
  </style>
</head>
<body>
  <header>
    <h1>Dual-depth logout</h1>
    <p>As-built BFF vs Keycloak SSO. Source <a href="dual-depth-logout.mmd">dual-depth-logout.mmd</a> · contract <a href="../session-bff-oidc-contract.md">session-bff-oidc-contract.md</a></p>
  </header>
  <nav role="tablist" aria-label="Diagrams">
        ${buttons}
  </nav>
  <main>
        ${panes}
  </main>
  <script>
    const tabs = [...document.querySelectorAll('[role="tab"]')]
    const panes = [...document.querySelectorAll('.pane')]
    function show(slug) {
      tabs.forEach((tab) => tab.setAttribute('aria-selected', String(tab.dataset.pane === slug)))
      panes.forEach((pane) => pane.classList.toggle('is-active', pane.dataset.pane === slug))
    }
    tabs.forEach((tab) => tab.addEventListener('click', () => show(tab.dataset.pane)))
  </script>
</body>
</html>
`
}

const files = (await readdir(diagramsDir)).filter(name => name.endsWith('.mmd'))
if (files.length === 0) {
  throw new Error(`No .mmd files in ${diagramsDir}`)
}

await mkdir(diagramsDir, { recursive: true })
const galleryItems = []

for (const file of files) {
  const source = await readFile(path.join(diagramsDir, file), 'utf8')
  const stem = file.replace(/\.mmd$/, '')
  const diagrams = splitDiagrams(source, stem)
  for (const diagram of diagrams) {
    const svg = await renderMermaid(diagram.text, theme)
    const outName = diagrams.length === 1 ? `${stem}.svg` : `${stem}-${diagram.slug}.svg`
    const outPath = path.join(diagramsDir, outName)
    await writeFile(outPath, svg)
    console.log(path.relative(repoRoot, outPath))
    galleryItems.push({
      slug: diagram.slug,
      file: outName,
      title: titles[diagram.slug] ?? diagram.slug,
    })
  }
}

const galleryPath = path.join(diagramsDir, 'index.html')
await writeFile(galleryPath, galleryHtml(galleryItems))
console.log(path.relative(repoRoot, galleryPath))

if (process.argv.includes('--open')) {
  spawn('xdg-open', [galleryPath], { detached: true, stdio: 'ignore' }).unref()
}
