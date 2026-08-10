In modern web development, end-to-end testing is essential for ensuring that applications behave as expected. When using new features like [React Server Components](https://react.dev/reference/rsc/server-components), we often rely on third-party APIs to fetch data on the server. While this approach offers significant benefits for performance and scalability, it can also introduce challenges for testing. Live API responses may change over time, causing tests to fail even when the application logic remains correct.

In this article, I'll introduce a new approach to server-side API mocking that makes tests fast and reliable with minimal setup. As a tech stack, I will use [Playwright](https://playwright.dev/) and [Next.js](https://nextjs.org/), though the method works with any framework or test runner. Let’s dive in!

## The Problem: Testing Server-Side Data Fetching

Consider the following server component, that fetches and renders a list of users from a third-party API:

```jsx
export async function UserList() {
  const res = await fetch('https://jsonplaceholder.typicode.com/users');
  const users = await res.json();

  return (
    <ul>
      {users.map((user) => (
        <li key={user.id}>{user.name}</li>
      ))}
    </ul>
  );
}
```
When browser requests this page, the server performs a subsequent call to `/users` API and returns rendered HTML:

![The flow](https://dev-to-uploads.s3.amazonaws.com/uploads/articles/ojp4ymqzbg87r8pd44f7.png)

The page in the browser:

![Rendered User list](https://dev-to-uploads.s3.amazonaws.com/uploads/articles/aponbpmn0wwidq8si4cw.png)

A basic Playwright test might look like this:

```js
test('show user list', async ({ page }) => {
  await page.goto('/');

  await expect(page.getByRole('listitem').first()).toHaveText('Leanne Graham');
});
```

This test passes when the first item in the API response is **Leanne Graham**. But what happens if the API returns a different order or data? The test will fail even though the application itself is functioning correctly.

Example of failed test, when elements are returned in a reversed order:

![Failed test](https://dev-to-uploads.s3.amazonaws.com/uploads/articles/gw2o7yu96g80zqdqhs5s.png)

The test would be more reliable, if it could mock the `GET /users` request and provide a static list of users: 

![Server-side mock](https://dev-to-uploads.s3.amazonaws.com/uploads/articles/2o7dpja2sodjx044e7k6.png)

When requests are made from the browser context, Playwright's [API mocking](https://playwright.dev/docs/mock) feature works fine. However, this approach doesn't intercept server-side requests.

## Existing Approaches

Several efforts have been made to address this challenge:

### Playwright Proxy Approach

An in-progress [pull request](https://github.com/microsoft/playwright/pull/34520) in the Playwright repo introduces server-side mocking by running an HTTP proxy alongside the test process. The app is configured to route outgoing requests through this proxy. If a request matches a specified URL pattern, a user-defined handler applies the mock response. 

![Mock with HTTP proxy](https://dev-to-uploads.s3.amazonaws.com/uploads/articles/qbwx33c91rywzo6cbcak.png)

While this approach is highly flexible, it also presents challenges. For instance, if your app is deployed on platforms like Vercel and tests run in GitHub workflows, setting up a tunnel to connect the app to the proxy can be complex and error-prone.

### Mock Service Worker (MSW)

[MSW](https://mswjs.io/) is a popular tool for mocking HTTP requests. It is also working on server-side mocking support, as seen in this [pull request](https://github.com/mswjs/msw/pull/1617). Instead of using an HTTP proxy, MSW relies on WebSockets as a transport layer to address connectivity issues:

![MSW flow](https://dev-to-uploads.s3.amazonaws.com/uploads/articles/zwa5469rm7wa461pk0ms.png)

However, this approach has its own limitations, as noted in the pull request:  

> You cannot have multiple tests that override request handlers for the same app at the same time.  

This means that tests with server-side mocks cannot run in parallel, which is a major drawback for end-to-end testing.  

Overall, existing approaches aim to use an arbitrary function as the mocked request handler but introduce connectivity and parallelization challenges.

## Proposed Solution

While experimenting with these solutions, I came to a simpler idea:
 
*What if we pass the mocking data within the navigation request using a custom HTTP header?*

![Mock header flow](https://dev-to-uploads.s3.amazonaws.com/uploads/articles/srhce45md4t0nyp5ma3a.png)

### How It Works

- **Embed Mock Data:** Instead of routing server-side requests through an external proxy, we encode static mock responses as JSON and attach them in a custom header (e.g., `x-mock-request`).

- **Server-Side Parsing:** On the server, we intercept outgoing API calls, read the custom header, and apply the corresponding mock if the request matches the predefined schema.

This approach solves both the connectivity and parallelization issues:
- There’s no need to set up tunnels or spin a separate proxy server.
- Each test can pass its own mock data via HTTP headers without conflict.

Of course, there are limitations:
1. **Static Data Only:** The mock must be serializable to JSON. This means you can only provide static responses (e.g., `{ status: 200, body: 'Hello' }`) rather than dynamic, function-based mocks.
2. **Header Size Limits:** HTTP headers typically support 4KB to 8KB of data. This approach is best suited for small payloads.

In many real-world scenarios, these limitations are acceptable. Most mocks are lightweight and static, making this a practical solution for ensuring test stability.

## Implementation

Below is a step-by-step guide how to implement this solution with Playwright and Next.js.

### Define Schemas

First, define the schemas for the request and the response. For instance, to mock a server-side GET request to `https://jsonplaceholder.typicode.com/users`, you can set up the following:

Request Schema:
```js
const reqSchema = {
  method: 'GET', 
  url: 'https://jsonplaceholder.typicode.com/users',
};
```

Response Schema:
```js
const resSchema = {
  status: 200,
  body: [
    { id: 1, name: 'John Smith' }
  ]
};
```

Combine Schemas and Build the Header:
```js
const mockSchema = { reqSchema, resSchema };
const mockSchemaString = JSON.stringify(mockSchema);
const headers = {
  'x-mock-request': mockSchemaString
};
```

### Playwright Integration

To attach custom HTTP headers to the navigation request, use Playwright's [page.setExtraHTTPHeaders](https://playwright.dev/docs/api/class-page#page-set-extra-http-headers):

```js
test('show user list', async ({ page }) => {
  await page.setExtraHTTPHeaders({
    'x-mock-request': mockSchemaString
  });

  await page.goto('/');
});
```

With this configuration, every navigation and subsequent request from the page will include the mocking header.

### Handling on the Server

On the server side, the following steps are required:

1. Read incoming headers
2. Get `x-mock-request` value and extract mock schemas
3. Intercept outgoing request
4. Apply mock schemas and return the mocked response

#### Read Incoming Headers and Extract Schemas

To read the incoming headers, you can use Next.js's [`headers()`](https://nextjs.org/docs/app/api-reference/functions/headers) helper. When `x-mock-request` header is found, use `JSON.parse()` to extract mock schemas:
```js
import { headers } from 'next/headers';
 
// ...

const headersList = await headers();
const mockHeader = headersList.get('x-mock-request');
const mockSchemas = JSON.parse(mockHeader);
```

#### Intercept Outgoing Requests

To intercept all outgoing requests in Next.js app, you can overwrite the `globalThis.fetch` function:

```js
const originalFetch = globalThis.fetch;
globalThis.fetch = async (input, init) => {
  // inspect and potentially mock outgoing request
};
```

Inside the intercepted function, you can read the incoming headers and apply the mocks. Full code of the function:

```js
function interceptGlobalFetch() {
  const originalFetch = globalThis.fetch;
  globalThis.fetch = async (input, init) => {
    // Read incoming headers and extract mocks
    const headersList = await headers();
    const mockHeader = headersList.get('x-mock-request');
    const mockSchemas = JSON.parse(mockHeader);

    // Match the request against schemas
    const request = new Request(input, init);
    const matchedSchema = mockSchemas.find(schema => matchRequest(request, schema));

    // Return mocked response or make a real request
    return matchedSchema
      ? buildMockedResponse(request, matchedSchema)
      : originalFetch(request)
  };
}
```

The global `fetch` should be instrumented at server startup, before any requests are made. Next.js provides a dedicated file for this task, called [instrumentation.js](https://nextjs.org/docs/app/building-your-application/optimizing/instrumentation): 

```js
// instrumentation.js

export async function register() {
  if (process.env.NEXT_RUNTIME === 'nodejs' && process.env.NODE_ENV !== 'production') {
    interceptGlobalFetch();
  }
}
```

> **Note:** Interception should be enabled only in the `nodejs` runtime and in non-production environments.

## Testing the Whole Flow

Once the server-side interception is in place, you can run your Playwright test with the server-side mock. Here’s an example:

```js
test('show user list', async ({ page }) => {
  // Set up server-side mock
  await page.setExtraHTTPHeaders({
    'x-mock-request': buildMockHeader()
  });

  // Navigate to the page
  await page.goto('/');

  // Assert page content according to mock data
  await expect(page.getByRole('listitem').first()).toHaveText('John Smith');
});
```

The `buildMockHeader()` helper just combines request and response schemas:
```js
function buildMockHeader() {
  const reqSchema = {
    method: 'GET', 
    url: 'https://jsonplaceholder.typicode.com/users',
  };

  const resSchema = {
    status: 200,
    body: [
      { id: 1, name: 'John Smith' }
    ]
  };

  return JSON.stringify([ { reqSchema, resSchema } ]);
}
```

Running the test:
```
> npx playwright test

Running 1 test using 1 worker
  1 passed (1.3s)
```

The page's screenshot shows a list with a mocked data - a single user `John Smith`:

![Page screenshot](https://dev-to-uploads.s3.amazonaws.com/uploads/articles/5oy0hclxhj0s2gxg3vq0.png)

With such a mock, the test no longer depends on the API response while ensuring that the server component correctly renders the data.

## Wrapping into a Library

To reduce the boilerplate code for server-side mocking, I bundled the functionality into a separate package called [request-mocking-protocol](https://github.com/vitalets/request-mocking-protocol). It hides the implementation details and provides a friendly API for setting up mocks on client and server side.

### Example Usage with the Library

The following example demonstrates how to use the library in a Playwright test:

```ts
test('show user list', async ({ page, mockServerRequest }) => {
  // Set up server-side mock
  await mockServerRequest.GET('https://jsonplaceholder.typicode.com/users', {
    body: [{ id: 1, name: 'John Smith' }],
  });

  // Navigate to the page
  await page.goto('/');

  // Assert page content according to mock data
  await expect(page.getByRole('listitem').first()).toHaveText('John Smith');
});
```

The custom fixture `mockServerRequest` is defined as follows:

```js
import { test as base } from '@playwright/test';
import { MockClient } from 'request-mocking-protocol';

export const test = base.extend({
  mockServerRequest: async ({ context }, use) => {
    const mockClient = new MockClient();
    mockClient.onChange = async (headers) => context.setExtraHTTPHeaders(headers);
    await use(mockClient);
  },
});
```

Under the hood, the library builds the mocking schemas and exposes them as HTTP headers.

On the server, you can set up the interceptor with a single call of `setupFetchInterceptor()`:
```js
// instrumentation.js
import { headers } from 'next/headers';

export async function register() {
  if (process.env.NEXT_RUNTIME === 'nodejs' && process.env.NODE_ENV !== 'production') {
    const { setupFetchInterceptor } = await import('request-mocking-protocol/fetch');
    setupFetchInterceptor(() => headers());
  }
}
```

## Recap

In this article, I introduced an alternative approach to server-side request mocking that uses HTTP headers to transfer mock data. This setup is simpler because it eliminates the need for additional proxies. Each test carries its own mock data, allowing for parallel execution and improved scalability.

The approach does have some limitations. It only supports static mocks — arbitrary JavaScript functions are not allowed. Additionally, HTTP headers have size limits, making this method best suited for smaller payloads.

Despite these trade-offs, the solution looks promising. I've packaged it into a [library](https://github.com/vitalets/request-mocking-protocol) for easier integration with different frameworks. You are welcome to give it a try and share the feedback.

Thanks for reading, and happy testing ❤️
