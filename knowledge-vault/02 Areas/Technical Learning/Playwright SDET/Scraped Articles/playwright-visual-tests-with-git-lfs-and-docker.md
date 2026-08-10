# Playwright Visual Tests with GIT-LFS and Docker

While working on various projects, I struggled to find a good solution for managing golden screenshots in Playwright. Source code: https://github.com/pajdekPL/playwright-git-lfs

## Why Git LFS for Visual Testing?

Git LFS stores large screenshot files outside regular Git, replacing them with lightweight pointers. Benefits: smaller repo, faster clones, efficient binary file handling.

## Benefits of Version-Controlled Screenshots

Team collaboration, change history, code review integration, accountability, rollback capability, CI/CD integration.

## Setup Instructions

1. Install Git LFS: `brew install git-lfs && git lfs install`
2. Configure `.gitattributes`: `tests/**/*.png filter=lfs diff=lfs merge=lfs -text`
3. Set `snapshotDir: "./screenshots"` in playwright.config.ts

## Usage

Write visual tests with `toHaveScreenshot()`, generate baselines with `npx playwright test --update-snapshots`, commit with regular git commands.

## Building and Running PW in Docker

CI runs on Linux — generate Linux screenshots via Docker:
```
docker run --rm --network host --ipc=host -v "$(pwd)":/work/ -w /work/ -it mcr.microsoft.com/playwright:v1.49.0-noble /bin/bash -c "npm install && bash"
```
Or use `run-pw-docker.sh` script.

## Screenshot Management Strategy

Only store Linux screenshots in repo:
```
*.png
!*-linux.png
```

## Best Practices

Organize screenshots, meaningful names, tagging (@visual-regression), review changes, CI Git LFS setup.

## Common Issues and Solutions

- Large repo: clean unused screenshots
- Team: ensure Git LFS installed, run `git lfs pull`
- CI: install Git LFS in pipeline
- Husky hooks may need Git LFS hook integration

## Conclusion

Git LFS provides efficient management of Playwright visual test screenshots. Use Docker for cross-platform consistency.

