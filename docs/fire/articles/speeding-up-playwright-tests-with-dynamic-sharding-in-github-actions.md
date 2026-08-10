# Speeding Up Playwright Tests with Dynamic Sharding in GitHub Actions

**Author:** Lewis Nelson  
**Published:** 2025-05-06  
**URL:** https://lewis-38728.medium.com/speeding-up-playwright-tests-with-dynamic-sharding-in-github-actions-91906aa9ed8f

## Firecrawl Metadata

```json
{
  "author": "Lewis Nelson",
  "article:published_time": "2025-05-06T13:42:16.127Z",
  "publishedTime": "2025-05-06T13:42:16.127Z",
  "og:title": "Speeding Up Playwright Tests with Dynamic Sharding in GitHub Actions",
  "og:description": "Running end-to-end (E2E) tests with Playwright works well out of the box. But as your test suite grows, CI runtimes tend to grow with it …",
  "sourceURL": "https://lewis-38728.medium.com/speeding-up-playwright-tests-with-dynamic-sharding-in-github-actions-91906aa9ed8f",
  "statusCode": 200,
  "scrapeId": "019fb3cc-7ec8-720f-915d-53234a729514"
}
```

---

[**TLDR; the final solution**](https://lewis-38728.medium.com/speeding-up-playwright-tests-with-dynamic-sharding-in-github-actions-91906aa9ed8f#1d84)

Running end-to-end (E2E) tests with [Playwright](https://playwright.dev/) works well out of the box. But as your test suite grows, CI runtimes tend to grow with it. Splitting test execution across shards is a common way to speed things up, but hardcoding shard counts quickly becomes inflexible.

In this post, I'll walk through how I implemented **dynamic test sharding for Playwright in GitHub Actions**. This approach helped us significantly reduce test run times, avoid redundant builds, and keep reporting streamlined.

We'll cover:

- Automatically determining the number of shards
- Reusing a single build across test jobs (or using Docker instead)
- Running Playwright tests in parallel
- Merging test reports
- Cleaning up artifacts

## **Why Dynamic Sharding?**

Playwright supports sharding via its `--shard` CLI flag:

```
npx playwright test --shard=1/3
```

The issue is: how do you decide on the number of shards? Hardcoding shard counts works temporarily, but when your test suite changes frequently, it becomes a maintenance headache. You either over-parallelise (wasting CI resources) or underutilise and slow down your runs.

With dynamic sharding, we:

1. Count the number of tests
2. Compute how many shards are needed (e.g., 40 tests per shard)
3. Run tests in parallel using GitHub Actions' matrix strategy

## **GitHub Actions Setup**

The workflow is structured around five main jobs:

1. Generate the dynamic matrix
2. Build the app once
3. Run tests in shards
4. Merge the reports
5. Clean up intermediate artifacts

Let's walk through each step.

## **1. Generate the Test Shards Matrix**

This job uses Playwright's `--list` command to get the total number of tests and calculates how many shards to split them into:

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

- name: Generate shards matrix JSON
  id: set-matrix
  run: |
    jq -cn --argjson count "${{ env.SHARD_COUNT }}" \
      '[range(1; $count + 1) | { "shard-index": ., "total-shards": $count }]' > matrix.json
    echo "matrix=$(cat matrix.json)" >> "$GITHUB_OUTPUT"
```

The `SHARD_COUNT` calculation uses a ceiling division by 40 — the target number of tests per shard. You can adjust this depending on your runtime characteristics.

## **2. Build Once, Use Many Times**

Instead of building the app in every test shard (which would be wasteful), we do it once and upload the result:

```
build:
  runs-on: ubuntu-latest
  steps:
    - uses: actions/checkout@v4
    - run: pnpm build
    - uses: actions/upload-artifact@v4
      with:
        name: tests-build
        path: .next
```

This artifact is later downloaded by each test shard. It saves a significant amount of time in larger repositories.

**Alternative:** If you prefer not to use artifacts, you could build a Docker image in this step, push it to a registry, and then pull it in each test job. This keeps the environment completely consistent and avoids relying on GitHub artifact storage limits.

## **3. Run Sharded Tests in Parallel**

We use the matrix generated earlier to launch one job per shard. Each job:

- Downloads the shared build
- Installs Playwright
- Starts the server
- Runs its test slice
- Uploads its report

```
test:
  strategy:
    matrix:
      include: ${{ fromJSON(needs.generate-shards-matrix.outputs.matrix) }}
  steps:
    - uses: actions/checkout@v4
    - uses: actions/download-artifact@v4
      with:
        name: tests-build
        path: .next
    - run: pnpm playwright install chromium
    - run: pnpm start & echo $! > server.pid
    - run: pnpm test --shard="${{ matrix.shard-index }}/${{ matrix.total-shards }}"
    - run: |
        kill "$(cat server.pid)" || true
        rm server.pid
    - uses: actions/upload-artifact@v4
      with:
        name: test-report-${{ matrix.shard-index }}
        path: blob-report
```

## **4. Merge Reports**

Once all the shard jobs finish, a separate job merges their outputs into a single HTML report:

```
merge-reports:
  # Ensures we only run when tests failed or passed explicitly
  if: ${{ always() && cancelled() == false && needs.test.result != 'skipped' }}
  needs: [test]
  steps:
    - uses: actions/checkout@v4
    - uses: actions/download-artifact@v4
      with:
        path: blob-reports
        pattern: test-report-*
        merge-multiple: true
    - run: pnpm playwright merge-reports --reporter html ./blob-reports
    - uses: actions/upload-artifact@v4
      with:
        name: playwright-html-report
        path: playwright-report
```

## **5. Clean Up**

Finally, we remove temporary artifacts to keep storage usage low:

```
cleanup:
  # This ensures we can re-run failed workflows
  if: ${{ success() && needs.tests.result == 'success' }}
  needs: [merge-reports]
  steps:
    - uses: geekyeggo/delete-artifact@v2
      with:
        name: |
          test-report-*
          tests-build
```

## **Benefits**

This approach gives us:

- Faster CI cycles through real parallelism
- Dynamic scaling without hardcoding values
- More maintainable workflows — no manual shard updates
- Accurate test reports merged into a single, navigable HTML view
- Efficient use of resources with shared builds or Docker images

## **Final Thoughts**

Dynamic sharding with Playwright and GitHub Actions is a relatively small infrastructure improvement that brings outsized benefits. If your test suite is growing and you're looking for a way to keep CI fast and maintainable, this setup might be worth adopting.

You can go further with enhancements like:

- Duration-based test balancing
- Slack notifications or GitHub comment bots for test summaries
- Hosting reports on S3 or GitHub Pages
- Dockerizing the test environment for even more reproducibility

If you've implemented something similar or have questions about this approach, feel free to reach out. You can contact me at [lewnelson.com/contact](https://lewnelson.com/contact).

## TLDR;

The final solution:

```
name: Playwright tests

# Runs on push to all branches
on:
  push:
    branches:
      - '**'

jobs:
  # 🔢 Generate Dynamic Matrix Based on Total Tests
  generate-shards-matrix:
    runs-on: ubuntu-latest
    outputs:
      matrix: ${{ steps.set-matrix.outputs.matrix }}
    steps:
      - uses: actions/checkout@v4

      - name: Get Total Number of Tests
        id: test-count
        run: |
          TEST_LIST_OUTPUT=$(pnpm test --list)
          TOTAL_TESTS=$(echo "$TEST_LIST_OUTPUT" | grep 'Total:' | awk '{print $2}')
          echo "TOTAL_TESTS=$TOTAL_TESTS" >> "$GITHUB_ENV"

      - name: Total shards
        run: |
          SHARD_COUNT=$(( (TOTAL_TESTS + 39) / 40 ))
          echo "SHARD_COUNT=$SHARD_COUNT" >> "$GITHUB_ENV"
        env:
          TOTAL_TESTS: ${{ env.TOTAL_TESTS }}

      - name: Generate shards matrix JSON
        id: set-matrix
        run: |
          jq -cn --argjson count "${{ env.SHARD_COUNT }}" '[range(1; $count + 1) | { "shard-index": ., "total-shards": $count }]' > matrix.json
          echo "matrix=$(cat matrix.json)" >> "$GITHUB_OUTPUT"

  # 🧱 Build the App Once for All Shards
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Build application
        run: pnpm build

      - name: Upload build artifact
        uses: actions/upload-artifact@v4
        with:
          name: tests-build
          path: .next
          include-hidden-files: true
          overwrite: true

  # 🚀 Run Sharded Tests
  test:
    needs:
      - generate-shards-matrix
      - build
    strategy:
      matrix:
        include: ${{ fromJSON(needs.generate-shards-matrix.outputs.matrix) }}
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Download build artifact
        uses: actions/download-artifact@v4
        with:
          name: tests-build
          path: .next

      - name: Install playwright
        run: pnpm playwright install chromium

      - name: Start Server
        run: pnpm start & echo $! > server.pid

      - name: Run Tests (Shard ${{ matrix.shard-index }}/${{ matrix.total-shards }})
        env:
          SHARD_INDEX: ${{ matrix.shard-index }}
          TOTAL_SHARDS: ${{ matrix.total-shards }}
        run: pnpm test --shard="$SHARD_INDEX/$TOTAL_SHARDS"

      - name: Stop Server
        if: always()
        run: |
          kill "$(cat server.pid)" || true
          rm server.pid

      - name: Upload Report Blob
        if: always()
        uses: actions/upload-artifact@v4
        with:
          name: test-report-${{ matrix.shard-index }}
          path: blob-report

  # 🧪 Merge Reports
  merge-reports:
    # Ensures we only run when tests failed or passed explicitly
    if: ${{ always() && cancelled() == false && needs.test.result != 'skipped' }}
    needs:
      - test
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Download All Shard Reports
        uses: actions/download-artifact@v4
        with:
          path: blob-reports
          pattern: test-report-*
          merge-multiple: true

      - name: Merge to HTML Report
        run: pnpm playwright merge-reports --reporter html ./blob-reports

      - name: Upload HTML Report
        uses: actions/upload-artifact@v4
        with:
          name: playwright-html-report
          path: playwright-report

      - name: Fail merge job if any test job failed
        run: |
          if [[ "${{ needs.tests.result }}" == "failure" ]]; then
            echo "A test job failed, failing this job so it can be retried."
            exit 1
          fi

  # 🧹 Cleanup Artifacts
  cleanup:
    # This ensures we can re-run failed workflows
    if: ${{ success() && needs.tests.result == 'success' }}
    needs: [merge-reports]
    runs-on: ubuntu-latest
    steps:
      - uses: geekyeggo/delete-artifact@v2
        with:
          name: |
            test-report-*
            tests-build
```
