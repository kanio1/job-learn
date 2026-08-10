[Playwright](https://playwright.dev/) is a powerful testing framework, but its HTML reports can feel bare-bones for complex projects. Wouldn't it be nice to display metadata such as commit messages, author details, or links to CI builds right in the report header?

## Understanding Playwright Metadata

The Playwright documentation mentions a metadata field for configuration, but at Playwright v1.49 the docs were outdated. Metadata in reports is indeed possible.

## The Real Metadata Configuration

Supported HTML report fields (from Playwright source):
- revision.id, revision.author, revision.email, revision.subject, revision.timestamp, revision.link
- ci.link, timestamp

Configure in playwright.config.ts with reporter: 'html' and metadata object.

## Automating Metadata Population

### Third-Party packages
Use npm packages that extract Git commit information.

### Leveraging Playwright's Hidden Plugin System
Use the hidden @playwright/test plugins config with gitCommitInfo() plugin to auto-populate commit hash, message, author, email, timestamp, and CI links.

### Using a Custom Function for Metadata
Adapt gitStatusFromCLI() from the gitCommitInfo plugin for independent use in playwright.config.ts.

#### Optimizing for Parallel Tests
Execute metadata function only in main worker (empty TEST_WORKER_INDEX) to avoid slowing each worker.

## Conclusion

Adding metadata to Playwright HTML reports is possible though not well-documented. Metadata fields are limited to specific keys. Automate with hidden plugins or custom scripts. Optimize for parallel tests by running metadata logic only in the main worker.

