# Is It Worth Mocking WebSockets by Playwright?

## Rather yes.

At first glance, there is nothing complicated about testing WebSockets. You can filter connections of this protocol type (WS) in a browser's DevTools, API's logic can be traced in WebSocket's messages, and many popular testing tools, including Postman, support WebSockets. But things get more complicated when it comes to automation testing.

## What the hell are WebSockets?

WebSocket is a communication protocol that provides bidirectional communication between a client and a server over a persistent connection. It enables real-time data exchange between a client (usually a web browser) and a server, and allows efficient, low-latency communication.

> WebSocket **s** (in the plural) is a common name for multiple connections of this protocol type (even if only a single connection is made on the web page), like: «this site works on WebSockets».

Unlike traditional HTTP, where the client initiates a request and waits for a response, WebSocket allows both parties to send messages independently at any time, making it ideal for applications like chats, online gaming, collaborative tools, financial and trading services, and IoT applications.

However, the benefits of the WebSocket protocol pose challenges when it comes to automated tests.

- **Connection Handling.** Due to the need to establish a connection between both parties (client and server), it is pretty problematic to intercept this connection.
- **Asynchronous Communication.** WebSocket communications are often asynchronous, making it difficult to design a regular sequential test.
- **Message Formats.** WebSocket messages may use various formats (e.g., JSON, plain text, binary), requiring parsers and serializers to be included in tests.
- **Tooling Limitations.** Not many automation testing frameworks support interception and modification of WebSocket connection. To be honest, I did not know any before Playwright 1.48.

## Evolution of testing WebSockets in my application

My current testing project has a lot of functionality related to WebSocket connections. Due to the above-mentioned challenges (primarily due to tooling), its autotests have undergone evolutionary changes.

- Initially, we did not have any automation testing for WebSocket functionality. These features were tested manually, which slowed down our regression testing.
- Then, we implemented console logging of some WebSockets messages. Because Playwright allows us to listen to the browser's console messages, we could check WebSocket API messages (and thus the application logic), but it was extremely overcomplicated and unhandy.
- Then, we implemented a fallback to HTTP API for some WebSocket features. This made it possible to intercept and modify requests and responses for testing reasons. Again, it was overcomplicated, but it has expanded the automation testing opportunities.
- During 2021–2022, we hoped that Playwright would implement a feature-issue of WebSocket interception, but after a year, we gave up and started to implement our own mocking of WebSocket API.

There were two ways to mock WebSockets: **run a mock server** (like Camouflage) **or use a mocking library within the application** (like Mock Service Worker) — we chose the second one.

Adding **MSW (Mock Server Worker)** inside the application allowed us not only to mock WebSocket API but also to speed up the frontend development; our development and QA teams started using the same test doubles for testing.

The only flaw was that our application had a separate bundle for testing with mocks, while in production, it was deployed as a slightly different bundle.

## How to mock WebSockets with Playwright?

Firstly, why do mocking WebSockets through Playright if mocking through MSW works well? Because that is how we put the test infrastructure at the testing framework level. We can build and test our application as it will be deployed on production.

> WebSocket routing was added to Playwright since version 1.48.

The manner of mocking WebSockets with Playwright is quite straightforward. Everything is carried out by the WebSocketRoute class. As soon as you interfere in WebSocket communication by the onMessage method, WebSocket's messages will stop forwarded between page and server ⇒ you should handle the communication by yourself.

Here are a few tricky things before the start:

1. `routeWebSocket()` **method should be called before navigating the page;**
2. **The WS URL is more accessible when set by RegExp.** In most cases, a WebSocket connection is established with `ws://` or `wss://` schemes;
3. When using `routeWebSocket()` in your tests, **Playwright takes complete control over the WebSocket connection ⇒ you will not see the WS handler in a browser's DevTools Network tab anymore**.

So, here is a code if you want to catch a specific message **from page to server:**

```
await page.routeWebSocket(/.+\/api/, (ws) => {
  ws.onMessage((message) => {
    if (message === '{"command":"ping"}') {
      ws.send('{"command":"fooBar"}');
    }
  });
});
```

Where `ws.send()` method sends a message **to the page.**

And here is a code if you want to catch a specific message **from server to page:**

```
await page.routeWebSocket(/.+\/api/, (ws) => {
  const server = ws.connectToServer();
  server.onMessage((message) => {
    if (message === '{"command":"pong"}') {
      ws.send('{"command":"fooBar"}');
    } else {
      ws.send(message);
    }
  });
});
```

Where `ws.send()` method sends a message to the page from the server.

Unfortunately, I could not figure out if it is possible to intercept and mock messages in both ways in one `test()`. Even so, there is nothing to worry about if these test cases can be splitted.

To sum it up, Playwright's WebSockets has its limitations in the place of invoking the router and **requires advanced skills from a test engineer**, but if you have a simple application or a plain feature for testing, then WebSockets' out-of-the-box functionality is pretty enough.
