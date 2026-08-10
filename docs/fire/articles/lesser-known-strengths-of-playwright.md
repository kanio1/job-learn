# Lesser known Strengths of Playwright

| Field | Value |
|-------|-------|
| **Author** | Anjali Kulkarni |
| **Published** | 2025-08-23T09:26:23.938Z |
| **URL** | https://medium.com/technology-hits/lesser-known-strengths-of-playwright-9638f77089cd |
| **Scraped with** | Firecrawl `firecrawl_scrape` (`formats: ["markdown"]`, `onlyMainContent: true`) |

## Firecrawl metadata

```json
{
  "title": "Lesser known Strengths of Playwright",
  "og:title": "Lesser known Strengths of Playwright",
  "author": "Anjali Kulkarni",
  "publishedTime": "2025-08-23T09:26:23.938Z",
  "article:published_time": "2025-08-23T09:26:23.938Z",
  "og:url": "https://medium.com/technology-hits/lesser-known-strengths-of-playwright-9638f77089cd",
  "description": "API Automation with APIRequestContext",
  "statusCode": 200,
  "sourceURL": "https://medium.com/technology-hits/lesser-known-strengths-of-playwright-9638f77089cd"
}
```

---

**API Automation with APIRequestContext**

API Automation with Playwright : By Author

Playwright is more than just a UI automation tool. Typically, most teams would build a framework for UI automation and then import libraries to add support for API automation. Playwright simplifies this by providing inbuilt support for API testing, making it a complete framework for both frontend and backend automation. It is this aspect that we will explore in this blog today.

Playwright has some unique features which make it an appropriate choice for API Automation :

· Unified framework: UI and API tests in the same suite

· Seamless integration between UI flows and API validations

· Built-in support for HTTP methods (GET, POST, PUT, DELETE)

· Support for authentication (bearer tokens, cookies)

Ability to mock and intercept network calls

**Step 1: Creating object for API Automation**

Playwright provides two kinds of contexts depending on what is being automated.

**BrowserContext** used for UI interactions. It works like an independent browser profile with its own cookies, local storage, and cache. This allows tests to run in isolation without interfering with each other.

**APIRequestContext** is a special object for interacting directly with APIs. It can send HTTP/HTTPS requests (GET, POST, PUT, DELETE) without the need to launch a browser. This makes it possible to keep API tests independent of UI tests, while still allowing the flexibility to combine both for complete end-to-end scenarios.

```
# Create a new API request context
token = "mytoken123456"
request_context = p.request.new_context(
    base_url="https://api.example.com",
    extra_http_headers={
        "Authorization": f"Bearer {token}",
        "Content-Type": "application/json"
    }
)
```

Here " **base\_url**" is the common prefix for all API calls.

" **extra\_http\_headers**": Sets default headers that are automatically included in every request sent from this context. Common headers are Authorization, Content-Type, and Accept, but you can also define custom headers such as X-API-Key or X-Correlation-ID depending on the API requirements.

APIs can be authorized in various ways and authorization details are specified under "Authorization" attribute of "extra\_http\_headers". Few of the authorization methods are described below:

**Tokens** are digital keys used to prove identity when making API calls. Basic Authentication and Bearer tokens are the most common, but other options include Digest Authentication, API Keys, MAC Tokens, and custom tokens.

**Basic Authentication** uses a base64-encoded string of username and password to form an authentication string.

```
credentials = "YWRtaW46c2VjcmV0"
extra_http_headers={
    "Authorization": f"Basic {credentials}",
    "Content-Type": "application/json"
    }
```

**Bearer Tokens,** are short lived tokens used to gain trust of the server until they expire or are revoked.

```
token = "mytoken123456"
extra_http_headers={
    "Authorization": f"Bearer {token}",
    "Content-Type": "application/json"
}
```

**Custom Headers**: Some APIs do not use standard authorization. Instead they might require different methods for e.g. X-API-Key or username and password directly. These are added directly to the "extra\_http\_headers" in place of the "Authorization" header. For e.g.:

```
extra_http_headers={
"X-API-Key": "123456"
}
```

OR

```
#Headers: Auth-User: admin, Auth-Pass: secret
extra_http_headers={
    "Auth-User": "admin",
    "Auth-Pass": "secret"
}
```

**Step 2: Calling the endpoint**:

```
response = await request_context.get("API ENDPOINT PATH")
```

Here,

· **request\_context** object will make API calls (GET, POST, etc)

· **API ENDPOINT PATH** is appended to the base\_url defined in the request context to form a complete URL.

· **response** object contains details such as status code, headers, and body.

**Step 3: Assertions for API responses:**

· **Status Code Assertions:** Verify that response returned expected HTTP status

```
assert response.status == 200
```

· **Response Body Assertions:** Check specific values in the response body

```
response = ""
data = await response.json()
assert data["id"] == 1
assert data["username"] == "john_doe"
```

· **Headers Assertions:** Confirm expected headers are present in response.

```
assert response.headers["content-type"] == "application/json"
```

**Schema / Structure Assertions:** Validate that response matches defined schema.

```
from jsonschema import validate
schema = {
    "type": "object",
    "properties": {
        "id": {"type": "integer"},
        "username": {"type": "string"}
    },
    "required": ["id", "username"]
}

data = await response.json()
validate(instance=data, schema=schema)
```

Apart from sending HTTP/S requests and validating responses, APIRequestContext can also be used for:

- **Setting up test data**: e.g. create a user directly via API instead of through the UI.
- **Combining UI and API tests for end-to-end flows:** e.g. login via API → get token → set token in browser storage → continue the UI flow.

**Network Interception & Mocking**

Playwright provides built-in request interception and mocking. When combined with APIRequestContext, this enables API automation even without actual API endpoints. It can be used to test UI behavior when endpoints are not yet implemented, when API servers are down, or for negative testing scenarios, this is where playwright shines.

At a high level, this works as follows:

- Define a route handler to intercept or mock requests.
- API calls are routed through this handler, bypassing the server and returning the custom response defined in the handler.

Find a detailed explanation of request interception and mocking in this blog: [https://medium.com/technology-hits/lesser-known-strengths-of-playwright-0b16ca715cf3](https://medium.com/technology-hits/lesser-known-strengths-of-playwright-0b16ca715cf3)

In a nutshell, APIRequestContext can be used to send requests programmatically, or with the context.route() method to intercept, mock, or block responses. This makes it possible to build an automation framework that handles real requests while also simulating negative and error scenarios with mocked responses. The same approach is valuable when the automation suite matures and needs integration with CI/CD tools like Jenkins or GitLab, making Playwright a powerful full-stack test automation tool.
