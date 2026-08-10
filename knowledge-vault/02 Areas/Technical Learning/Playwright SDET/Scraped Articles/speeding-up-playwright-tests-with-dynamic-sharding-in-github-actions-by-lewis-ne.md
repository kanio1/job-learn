# Speeding Up Playwright Tests with Dynamic Sharding in GitHub Actions

Running end-to-end (E2E) tests with Playwright works well out of the box. But as your test suite grows, CI runtimes tend to grow with it. Splitting test execution across shards is a common way to speed things up, but hardcoding shard counts quickly becomes inflexible.

In this post, I'll walk through how I implemented **dynamic test sharding for Playwright in GitHub Actions**.

## Why Dynamic Sharding?

Playwright supports sharding via its `--shard` CLI flag:

```
npx playwright test --shard=1/3
```

With dynamic sharding, we:

1. Count the number of tests
2. Compute how many shards are needed (e.g., 40 tests per shard)
3. Run tests in parallel using GitHub Actions' matrix strategy

## GitHub Actions Setup

The workflow is structured around five main jobs:

1. Generate the dynamic matrix
2. Build the app once
3. Run tests in shards
4. Merge the reports
5. Clean up intermediate artifacts

## 1. Generate the Test Shards Matrix

```
- name: Get Total Number of Tests
  run: |
    TEST_LIST_OUTPUT=$(pnpm test --list)
    TOTAL_TESTS=$(echo "$TEST_LIST_OUTPUT" | grep 'Total:' | awk '{print $2}')
    echo "TOTAL_TESTS=$TOTAL_TESTS" >> "$GITHUB_ENV"

- name: Total shards
  run: |
    SHARD_COUNT=$(( (TOTAL_TESTS + 39) / 40 ))
    echo "SHARD_COUNT=$SHARD_COUNT" >> "$GITHUB_ENV"
```

## 2. Build Once, Use Many Times

Build once and upload artifact `.next` for all shards to download.

## 3. Run Sharded Tests in Parallel

Matrix strategy runs `pnpm test --shard="$SHARD_INDEX/$TOTAL_SHARDS"` per job.

## 4. Merge Reports

`pnpm playwright merge-reports --reporter html ./blob-reports`

## 5. Clean Up

Remove temporary artifacts with geekyeggo/delete-artifact.

## Benefits

- Faster CI cycles through real parallelism
- Dynamic scaling without hardcoding values
- Accurate test reports merged into a single HTML view

## TLDR; Final solution includes generate-shards-matrix, build, test matrix, merge-reports, and cleanup jobs with full YAML in the article.
