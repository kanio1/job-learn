#!/bin/bash
set -e
cd /home/suso/job-learn/apps/frontend
# Force remove all possible caches
rm -rf node_modules/.cache node_modules/.vite .nuxt/analyze 2>/dev/null || true
# Run vitest with no-cache flag
node_modules/.bin/vitest run --reporter=verbose --no-color --cache=false 2>&1 | tee /home/suso/job-learn/vitest-result.txt
echo "EXIT_CODE=$?" >> /home/suso/job-learn/vitest-result.txt
