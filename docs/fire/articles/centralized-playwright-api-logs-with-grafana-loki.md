# Centralized Playwright API Logs with Grafana Loki

**Author:** Indra A.  
**Published:** 2025-05-27  
**URL:** https://medium.com/@indraaristya/centralized-playwright-api-logs-with-grafana-loki-368c21a76aba

## Firecrawl Metadata

```json
{
  "author": "Indra A.",
  "article:published_time": "2025-05-27T15:05:10.642Z",
  "publishedTime": "2025-05-27T15:05:10.642Z",
  "og:title": "Centralized Playwright API Logs with Grafana Loki",
  "og:description": "Log Playwright API test requests and responses to Grafana Loki with structured, redacted, and fast async logging",
  "sourceURL": "https://medium.com/@indraaristya/centralized-playwright-api-logs-with-grafana-loki-368c21a76aba",
  "statusCode": 200,
  "scrapeId": "019fb3ca-95c8-740a-af57-ec9bcd609585"
}
```

---

Playwright is one of a powerful tool for end-to-end and API testing — but sometimes the scenarios we've implement were failing in the CI pipeline and understanding _why_ they failed can be frustrating. What if you could log every request and response to a central, searchable system like Grafana Loki? In this post, I'll show you how to integrate Loki logging into your Playwright API tests, so you can debug faster without increase your test duration.

### Why Log API Test Data?

API test often failed due to several reasons, such as invalid auth header, body request is incorrect, or the flaky/error on the backend itself. When we run the test in local device one-by-one scenario, it will be easy to track if any error happened from the API. But, what if the test was running in pipeline, then implement parallelization which makes it harder to debug? This is why log API test data is needed.

By logging the API test data, it supposed to help us to know and understand what the error is; so we can fix the issue faster.

### Setup Loki and Grafana

Loki is a log aggregation system designed to store and query logs from any applications. It was started by Grafana Labs so it will work well with Grafana — which the tools to visualize and querying the logs that push to Loki. Before we update the logger in the automation test, let's setup the Loki and Grafana.

We will using Docker to _up_ the Loki and Grafana; they already provide the sample of `docker-compose` in the [documentation](https://grafana.com/docs/loki/latest/setup/install/docker/).

```
version: '3'

services:
  loki:
    image: grafana/loki:2.9.4
    ports:
      - "3100:3100"
    command: -config.file=/etc/loki/local-config.yaml

  grafana:
    image: grafana/grafana:10.3.3
    ports:
      - "3000:3000"
    environment:
      - GF_SECURITY_ADMIN_USER=admin
      - GF_SECURITY_ADMIN_PASSWORD=admin
    depends_on:
      - loki
```

Create `docker-compose.yml` file, to create & start the Loki and Grafana run `docker compose up -d`command. Both of the containers will be running in detached mode and ready to get your log.

Add New Datasource in Grafana

After that, open _localhost:3000_ in browser and login using the username and password defined in the compose file. Go to Connection → Data Source and choose Add New Connection. Look for **Loki** as the source, and add [_http://loki:3100_](http://loki:3100/) in the URL.

Please note that the _loki_ in the URL is the docker container name that has been set in the `docker-compose.yml` file. So it can be different if you change the name of it.

### Create Logging Helper

In this sample, I will be using my own Playwright portfolios sample. You can find the project in this repository.

[**GitHub - ndrcodes/test-automation-portfolios**](https://github.com/ndrcodes/test-automation-portfolios)

First, let's create the function to log the API request. Previously, in each scenario I am using `request` default function from Playwright. So, the logging function is need to be defined in every scenario one by one which cause a repetitive tasks and code. In this case, I would love to create the logging helper and combined with the sending request function, so we did not need to define the logging function explicitly.

```
const pendingLogs: Promise<void>[] = [];

export function enqueueLog(promise: Promise<void>) {
  pendingLogs.push(promise);
}

export async function flushLogs() {
  await Promise.allSettled(pendingLogs);
  pendingLogs.length = 0;
}
```

```
import axios from 'axios';
import { request, APIRequestContext, APIResponse } from '@playwright/test';
import { enqueueLog } from './logQueue';

interface LokiLabels {
  [key: string]: string;
}

interface RequestData {
    method: string,
    url: string;
    headers: Record<string, string>;
    body?: any;
}

interface ResponseData {
    status: number;
    body?: any;
}

function removeAuthOnHeader(headers: Record<string, string>): Record<string, string> {
  const redactedKeys = ['authorization', 'api-key', 'token'];
  const sanitized: Record<string, string> = {};

  for (const key in headers) {
    sanitized[key] = redactedKeys.includes(key.toLowerCase())
      ? '[REDACTED]'
      : headers[key];
  }

  return sanitized;
}

function removeAuthOnBody(body: any): any {
  if (!body || typeof body !== 'object') return body;

  const redactedKeys = ['password', 'token', 'apikey', 'secret'];
  return Object.fromEntries(
    Object.entries(body).map(([key, value]) => [\
      key,\
      redactedKeys.includes(key.toLowerCase()) ? '[REDACTED]' : value\
    ])
  );
}

export async function logApiCallToLoki(
  requestData: RequestData,
  responseData: ResponseData,
  tags: LokiLabels,
): Promise<void> {
  const lokiUrl = 'http://localhost:3100/loki/api/v1/push';
  const timestamp = Date.now() * 1_000_000;

  const logData = {
    request: {
      method: requestData.method,
      url: requestData.url,
      headers: removeAuthOnHeader(requestData.headers),
      body: removeAuthOnBody(requestData.body)
    },
    response: {
      status: responseData.status,
      body: removeAuthOnBody(responseData.body)
    }
  };

  const payload = {
    streams: [\
      {\
        stream: {\
          job: process.env.JOB_NAME || 'local',\
          environment: tags.environment ?? 'dev',\
          ...tags\
        },\
        values: [\
          [timestamp.toString(), JSON.stringify(logData)]\
        ]\
      }\
    ]
  };

  try {
    await axios.post(lokiUrl, payload);
  } catch (err: any) {
    console.error('Loki logging error:', err.message);
  }
}

export async function createLoggedApiContext(): Promise<APIRequestContext> {
  const apiContext = await request.newContext();

  const originalFetch = apiContext.fetch;

  apiContext.fetch = async function (
    url: string,
    options?: Parameters<APIRequestContext['fetch']>[1]
  ): Promise<APIResponse> {
    const reqData = {
        method: options?.method || 'GET',
        url: url,
        headers: options?.headers,
        body: options?.data || {}
    }

    const response = await originalFetch.call(this, url, options);

    const responseData = {
        status: response.status(),
    }

    try {
        responseData['body'] = await response.json()
    } catch {
        responseData['body'] = {}
    }

    const logging = logApiCallToLoki(reqData, responseData, {
      environment: process.env.ENV || 'local',
    }).catch((e) => {
        console.warn('Loki failed to log the error:', e.message);
    });
    enqueueLog(logging);
    return response;
  };

  return apiContext;
}
```

Above are the queue and the request function. As we can see in `createLoggedApiContext` function, we are creating new context of request, save the request and response data, and also send the test API request. In this function we also modify any sensitive data such as API key and auth token to be updated and changed as **REDACTED** so that the auth not visible in the Loki logging.

The logging also support to add _tag_ or _labels_ in every log. We can modify these labels based on our need; and as the sample I am using job name and environment to be added. We can set `JOB_NAME` and `ENV` value in the environment variable of the test.

The logging method was runs in the background without delaying the test, and we implement the queueing promises as well to prevent the teardown process done while the logging still sending to Loki (because we did not await the push process).

```
import { flushLogs } from "../utils/logQueue";

async function globalTeardown() {
    console.log("Make sure all logs were sent to Loki")
    await flushLogs();
}

export default globalTeardown;
```

To make sure the queue is flushed — which means all logs has been sent to the Loki and no one in queue — we need to create a global teardown function as seen as in the code above. The global teardown function will make sure to _await_ until all logs sent to Loki. Set the global teardown in the `playwright.config.ts` file.

### Implement Logging in Playwright Test

The last part, after we made the logging helper and global teardown above, we need to implement the helper function in the test.

```
import { test, expect } from '@playwright/test';
...
test('TAPI0006,TAPI0007-Success to create new users with valid data and Authorization header', async ({ request }) => {
    const body = await createUser('valid');

    const response = await request.post(`/public/v2/users`, { headers: header, data: body })
    const responseJson = await response.json()
    expect(response.status()).toBe(201)
    expect(responseJson['name']).toEqual(body['name'])
    expect(responseJson['email']).toEqual(body['email'])
    expect(responseJson['gender']).toEqual(body['gender'])
    expect(responseJson['status']).toEqual(body['status'])
    expect(await validateJsonSchema('user', responseJson))
  });
```

Previously, the API test was implemented as the code above. The request function from Playwright is directly used to do the API request.

Now, we need to _import_ the `createLoggedApiContext` and use it in the test as follows.

```
import { test, expect } from '@playwright/test';
import { createLoggedApiContext } from '../../utils/lokiLogger';
...
test('TAPI0006,TAPI0007-Success to create new users with valid data and Authorization header', async ({ }) => {
    const body = await createUser('valid');

    const request = await createLoggedApiContext();
    const response = await request.post(`/public/v2/users`, { headers: header, data: body })
    const responseJson = await response.json()
    expect(response.status()).toBe(201)
    expect(responseJson['name']).toEqual(body['name'])
    expect(responseJson['email']).toEqual(body['email'])
    expect(responseJson['gender']).toEqual(body['gender'])
    expect(responseJson['status']).toEqual(body['status'])
    expect(await validateJsonSchema('user', responseJson))
  });
```

By _import_ and use the helper function we made before, it will preparing the payload to be sent to Loki and do our request. After the request finished, the log payload will be push to the Loki using HTTP request.

### Visualize in Grafana

The log data will be able to seen in Grafana once the test was executed. It is possible to see the log in _Explore_ or create your own _Dashboard_.

Sample Log Data sent to Loki

Screenshot above show us all the API test data that has been sent to Loki. By this, it should be easier for us to check the API response error and understand what is happening to the test.

I only sent the job name and environment name as a tag, so it can be useful for the filtering. You could modify the logging helper to add more tag as the metadata that could help you to identify and analyze the logs, such as add the test case ID, build number, build version, and else.

### Conclusion

Debugging without visibility is painful. By integrating **Grafana Loki** with your **Playwright API tests**, you gain centralized, structured, and searchable logs without adding friction or latency to your test suite.

Whether you're running tests in local device or in CI, this approach helps you catch issues faster, investigate failures, and build more observable testing pipelines. Now your tests don't just pass.

Thank you and happy testing!
