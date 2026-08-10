Hello dear quality lovers,

It's no secret that Allure stands out as one of the most popular frameworks for test reporting. Today we integrate Allure Reports with Playwright.

## How to install

### Playwright quick installation

```
npm init playwright@latest
```

### Allure-playwright library installation

```
npm i -D allure-playwright
```

### Allure command line installation

```
npm i -D allure-commandline
```

Or globally: `npm i -g allure-commandline`

### Java requirement

Running Allure reports requires Java 8+. Install Java, add to PATH, verify with `java -version`.

## Extend Playwright configuration

Add `allure-playwright` to the reporter array in `playwright.config.ts`.

## Run tests and reports

```
npx playwright test
```

After tests complete, find `allure-results` folder.

Generate report:

```
allure generate --clean
```

Run server:

```
allure serve
```

## Additional configuration and attributes

Report options include `outputFolder`, `detail`, `suiteTitle`.

### Allure suite

Add `allure.suite` attribute or use `test.describe` names for suite grouping.

Other attributes: `allure.step`, `allure.label`, `allure.story`, `allure.link`, `allure.issue`, `allure.attachment`, `allure.parameters`.

### Allure issue

```javascript
await allure.issue('Bug description', 'https://github.com/org/repo/issues/29');
```

### Allure attachment

Screenshots/videos attach by default when configured in playwright.config.

Custom JSON attachment:

```javascript
await allure.attachment("ATTACH_ACTUAL_PAGES", JSON.stringify(actualPagesData), {
    contentType: "application/json",
});
```

### Allure parameters

```javascript
await allure.parameter("KEY", key.toString());
```

Parameters support `mode: "hidden" | "masked"` and `excluded: true`.

## Bonus: Run tests and reports in docker container

Create Dockerfile with Node, Playwright deps, Allure CLI, http-server. Run tests, generate reports, serve on port 8080.

Use docker-compose to map host port 9000 to container 8080.

## Ending

This was a high-level overview. Read the allure-playwright npm documentation for more options.

Dockerfile sample: FROM node:latest WORKDIR /usr/src/app COPY package*.json RUN npm install RUN npx playwright install --with-deps RUN npm install -g allure-commandline http-server COPY . . RUN npm run test || true RUN allure generate ./allure-results --clean -o ./allure-report EXPOSE 8080 CMD http-server allure-report -p 8080.

docker-compose maps host 9000 to container 8080 when local 8080 busy. build --no-cache then up navigates localhost:9000 for Allure server inside container.

allure.issue links failed tests to known GitHub issues visible in report attachments. allure.attachment JSON.stringify page data with application/json content type. allure.parameter KEY with mode hidden masked excluded options for sensitive keyboard test data.

Report config outputFolder detail suiteTitle control hook visibility and suite naming. Disabling suiteTitle uses test.describe names as default suite labels.

Screenshots on failure via playwright.config screenshot only-on. Java 8+ required for allure serve locally java -version verification step.

Kostiantyn Teltov Ukraine QA Tech Lead SDET QA Architect C# JS TS Python Java blogger speaker February 2024 Medium 10 min read.

Playwright multiple reporters array can include html list and allure-playwright simultaneously. allure-results raw folder allure-report generated folder allure generate --clean wipes stale history.

Global vs local allure-commandline: local project version takes precedence when both installed.

Beta allure-playwright note: author tested beta for bug fix verification prefer stable npm i -D allure-playwright for production pipelines.
