# Playwright in Practice: Writing Better Tests for Beginners with Page Object Pattern, Fixtures (TS)

| Field | Value |
|-------|-------|
| **Author** | Michał Ślęzak |
| **Published** | 2025-10-20T08:50:00+00:00 |
| **URL** | https://testingplus.me/playwright-in-practice-page-object-pattern/ |
| **Scraped with** | Firecrawl `firecrawl_scrape` (`formats: ["markdown"]`, `onlyMainContent: true`) |

## Firecrawl metadata

```json
{
  "title": "Playwright in Practice: Writing Better Tests for Beginners with Page Object Pattern, Fixtures (TS) - Your Gateway to Efficient Test Automation",
  "og:title": "Playwright in Practice: Writing Better Tests for Beginners with Page Object Pattern, Fixtures (TS) - Your Gateway to Efficient Test Automation",
  "author": "michal slezak",
  "publishedTime": "2025-10-20T08:50:00+00:00",
  "article:published_time": "2025-10-20T08:50:00+00:00",
  "og:url": "https://testingplus.me/playwright-in-practice-page-object-pattern/",
  "description": "Learn how to write cleaner, more maintainable Playwright tests with Page Object Pattern, fixtures, and TypeScript. A beginner-friendly refactoring guide.",
  "statusCode": 200,
  "sourceURL": "https://testingplus.me/playwright-in-practice-page-object-pattern/"
}
```

---

![](https://7fffeda0.delivery.rocketcdn.me/wp-content/uploads/2025/10/Teal-Professional-Blog-LinkedIn-Post-Presentation-2.png)

Recently, I’ve been writing about using AI tools with Playwright — for example, how to use Cursor with Playwright ( [link](https://testingplus.me/cursor-playwright-the-best-ai-ide-for-test-automation-part-one/)), Playwright MCP ( [l](https://testingplus.me/playwright-mcp-claude-ai-experimenting-without-overpromising/) [ink](https://testingplus.me/playwright-mcp-page-objects-shadow-dom-refactoring/)), and how AI can help generate simple page objects.

In today’s post, I’ll walk you through a complete refactoring: taking tests that don’t use the Page Object pattern or other object-oriented principles and turning them into maintainable tests. There are plenty of posts on design patterns—such as the Page Object pattern in Playwright—but, in my view, there aren’t many step-by-step resources that explain why we make specific changes, how we implement them, and why those changes are better.

### **Why does this approach make sense?**

From my perspective, when we start learning programming or other technical skills — for example, at university or in technical school – the learning process should begin with the basics.

Sometimes at university or school, we use older technologies, and some people ask why we still use tools that were popular several years ago. But I agree with what my friend told me when I started my joruney with IT 14 years ago.

“University isn’t meant to prepare us directly for today’s job market.  When we study, we learn a subject not just to get a job.  Of course, it is somewhat related, but at university we can explore topics in depth, discover things we wouldn’t see at work — things beyond our projects — and that’s good because it helps us connect the dots and understand the fundamental relationships in IT”.

I agree with that opinion because studying often overlaps with our work and future professional skills, but the process of education should be broader. This lets us look at IT and other fields from a broader perspective—one that goes beyond what would normally interest us. Of course, I’m not saying that we shouldn’t study IT — we can learn it on our own — but academic studies can give us a much broader view.

### **Let’s go furthe** r

That’s why, in this article, I’m going to show the full process — from a basic test to a more advanced one that includes the right design patterns.

## **Test for this article**

![playwright page object pattern a test page](data:image/svg+xml,%3Csvg%20xmlns='http://www.w3.org/2000/svg'%20viewBox='0%200%201024%20705'%3E%3C/svg%3E)

**Demo site: playwrightworkshops.com**

In today’s post, we’ll create a Playwright test for adding a review comment to a product on a demo page.

We’ll cover the following test scenarios:

1. Basic version without any design patterns
2. Adding types to the test and Faker
3. Adding types, fixtures, Faker, and Page Objects

The first scenario is simple – we’ll use Playwright to add a review for a product.

![](data:image/svg+xml,%3Csvg%20xmlns='http://www.w3.org/2000/svg'%20viewBox='0%200%201024%20434'%3E%3C/svg%3E)

It’s a simple test, deliberately written without advanced concepts, so we can understand step by step how to introduce them later.

For example:

- The URL is hardcoded in the test — it’s better to configure it in _playwright.config.ts_.
- Playwright recommends using `getByRole()`, but well-chosen CSS selectors can also work if they’re unique and resistant to small changes. Also, if it’s possible, use `data-testid` or `data-test` attributes can work together `getByRole()` for better stability.

Playwright promotes `getByRole()` because it supports web-first assertions that reflect the user’s point of view. From my perspective, CSS selectors or XPath can also work if they are stable, short, and unique.

In this test, we select the _mug_ category and then a specific product. This data could also be passed dynamically.

An interesting part of this test is the `startNumber` variable, which randomly selects a value from 1 to 5. It adds some variation to how many stars are chosen in the review.

### **What is wrong with this test?**

- We don’t use any design patterns, such as the Page Object Pattern or the Factory pattern. We’ll talk about it later.
- When you run this test more than once, you’ll encounter an issue related to WordPress’s anti-spam mechanism.
- The assertions are basic — we don’t check the content, only whether the element with the selector `.commentlist .review` is visible.
- There are potential traps – our tests should be stable and resistant to such situations.

This example shows how we learn test automation step by step.

Let’s move on to the next version of this test, which uses Faker, better assertions, and TypeScript types.

## **Refactoring — first attempt:** **This version of the test uses Faker, TypeScript types, and improved assertions.**

What’s Faker?

![playwright page object pattern faker github](data:image/svg+xml,%3Csvg%20xmlns='http://www.w3.org/2000/svg'%20viewBox='0%200%20924%20982'%3E%3C/svg%3E)

[https://github.com/faker-js/faker](https://github.com/faker-js/faker)

Faker is a library used for generating random data, and it’s very popular in the  JS/TS world. Of course, there are many other similar libraries available.

Installation is easy — just run the following command:

![playwright page object pattern faker](data:image/svg+xml,%3Csvg%20xmlns='http://www.w3.org/2000/svg'%20viewBox='0%200%20890%20646'%3E%3C/svg%3E)

As you can see in the code above, we have a few examples of how to generate specific data — for example, a username or an email address. If you’d like to dive deeper, I encourage you to check the documentation.

**Why do we use Types?**

![playwright page object pattern types](data:image/svg+xml,%3Csvg%20xmlns='http://www.w3.org/2000/svg'%20viewBox='0%200%201014%20514'%3E%3C/svg%3E)

A type in TypeScript is a compile-time construct that defines the shape and structure of data — specifying which properties an object can have and what kinds of values they can hold.

**Types** are a useful construct in TypeScript, especially in the context of automated testing, where they help model the data layer we want to work with or retrieve.

In this example, we define the type directly in our test code to demonstrate good coding practices step by step. However, in a production scenario, the `Review` Type would typically be placed in a separate TypeScript file to maintain a cleaner and more organized project structure.

Defining a `Review` the type itself is straightforward — we specify fields and their expected types, and then provide sample (mock) values in our tests. The code is quite clear, but one interesting part is the use of `faker.number.int()`, which allows us to generate pseudo-random integer values within a given range.

![playwright page object pattern - test with fixtures](data:image/svg+xml,%3Csvg%20xmlns='http://www.w3.org/2000/svg'%20viewBox='0%200%201024%20789'%3E%3C/svg%3E)

In this version, we use almost the same code as before. Apart from adding types, we also replace this value instead of keeping it hardcoded.

On line 156, we define a useful variable that uses a partial Review type. In this case, we only needed the `comment` and `starNumber` fields for assertions, since the final screen displays only those values. Thanks to using the `Partial<Review>` type, we didn’t have to define an additional type.

## **Next refactoring attempt: Adding in Playwright project page object pattern and fixtures**

In this version, our test will include Page Objects for all the pages required by the scenario. In this case, we need at least three page objects:

### **Implement Page Object Pattern in Playwright**

The Page Object pattern is one of the most popular design patterns in test automation. Instead of defining selectors directly in the test, we can create an abstraction layer that represents the page.

There are plenty of ways to implement this pattern. It can also be related to web applications. In this post, I’ll show one of the simplest ways to do that — simple, yet still useful, clear, and suitable even for more advanced projects. I recommend experimenting with different approaches while learning test automation — sometimes using one method, and in other cases another.

Talking with developers can be very beneficial. Even if they aren’t familiar with test automation frameworks, they still can share valuable ideas on how to improve certain areas.

### **Where should we put assertions in the Page Object Pattern?**

This subject can be tricky, and people often have different opinions about it. From my perspective, assertions should stay in the test itself. I agree with Martin Fowler (author of the Page Object pattern) recommends keeping assertions in tests, not inside Page Objects. Interestingly, he’s not a QA Engineer but a software architect and developer – which shows that we can (and should) draw inspiration from developers as well.

**There is a link to Martin Fowler’s article:**

[https://martinfowler.com/bliki/PageObject.html](https://martinfowler.com/bliki/PageObject.html)

Additionally, according to the Single Responsibility Principle, each layer of abstraction should have only one responsibility. Assertions should remain in tests, not inside Page Objects — this keeps the Single Responsibility Principle

- ShopHomePage – a Page Object associated with the home page with methods like go() or goToAllProducts(). These methods navigate to the home page or to the “All Products” section from the menu.

- AllProductsPage – This Page Object includes methods for selecting a category or a specific product. The advantage is that we can pass the name of a category or product as a parameter, instead of creating separate methods like `selectMugsCategory()`. There’s still room for improvement — for example, we could load these values dynamically.

- ProductDetailsPage – This Page Object contains a method for retrieving the current review object. This allows us to compare it with the expected review data or to use a `Partial<Review>` type, as we did in the previous test — it’s up to us.

Let’s move to improve our test. This test uses Faker and page objects. First, we need to define how to create page objects and utilize the page object pattern.

### **ShopHomePage** – simple implementation of the page object pattern

![](data:image/svg+xml,%3Csvg%20xmlns='http://www.w3.org/2000/svg'%20viewBox='0%200%201024%20507'%3E%3C/svg%3E)

- `go()` — Navigates to the home page.
- `goToAllProducts()` — Navigates to the “All Products” page from the navigation bar.

### **DetailsShopPage** – simple implementation of the page object pattern

![playwright page object pattern details product page](data:image/svg+xml,%3Csvg%20xmlns='http://www.w3.org/2000/svg'%20viewBox='0%200%201024%201011'%3E%3C/svg%3E)

This Page Object contains two methods:

- addReview – This method takes a Review object as a parameter. Thanks to that, we can pass all values defined by this type, making the method generic and reusable.

- getCurrentReview – This method retrieves the review’s description and star rating based on selectors. In this case, we use the `Partial<Review>` type, since only these fields are visible on the screen, but the complete review object can contain more data.

### **AllProductsPage** – simple implementation of the page object pattern

![playwright page object pattern all products page](data:image/svg+xml,%3Csvg%20xmlns='http://www.w3.org/2000/svg'%20viewBox='0%200%201024%20518'%3E%3C/svg%3E)

`AllProductsPage` contains two methods for selecting a category and a product. They use parameter values instead of hardcoded ones, which makes the code more flexible and reusable.

### **playwright.config.ts**

![](data:image/svg+xml,%3Csvg%20xmlns='http://www.w3.org/2000/svg'%20viewBox='0%200%201024%20154'%3E%3C/svg%3E)

Another useful thing we can add to the project is a base URL for the application. In a professional setup, this value should be provided through an environment variable. However, we can also define a default value in case `process.env.BASE_URL` is empty.

## **Fixtures – What They Are and Why You Should Use Them**

Fixtures are a powerful mechanism in Playwright that help reduce code duplication. Playwright provides several built-in fixtures (such as `page`, `browser`, and `request`), but we can also define our own — for example, for Page Objects. Fixture names must not overlap with Playwright’s built-ins, like `page` or `browser`.

Thanks to that:

• The code becomes shorter,

• Page Objects become more reusable,

• It’s easier to manage the initialization of common objects.

Fixtures can help initialize not only Page Objects but also related components such as services, helpers, or factories.

Fixtures are one of Playwright’s most powerful features.

They let you **prepare and share test dependencies** such as Page Objects, test data, or API clients — without repeating code across tests.

You can think of them as **controlled setups**: Playwright creates them automatically before each test and injects them into your test context.

### **Why does it matter?**

Without fixtures, every test would need to create page objects manually:

```ts

```

With fixtures, we can define them once and reuse them in every test.

### **Example – Defining custom fixtures**

```ts

```

### **Benefits of using fixtures:**

- Less duplicated code.
- Cleaner, centralized initialization.
- Easier configuration per test (e.g., test.use({ storageState })).
- Great for combining Page Objects, API clients, and helper utilities.

![playwright page object pattern fixtures](data:image/svg+xml,%3Csvg%20xmlns='http://www.w3.org/2000/svg'%20viewBox='0%200%201024%20666'%3E%3C/svg%3E)

Next, we need to declare our Page Objects, and then add the code that creates their instances. To make these objects accessible inside our tests, we use `base.extend`, which allows us to pass variables like `shopHomePage` to the test context:

![](data:image/svg+xml,%3Csvg%20xmlns='http://www.w3.org/2000/svg'%20viewBox='0%200%201024%2043'%3E%3C/svg%3E)

## **Finally, the test looks like this:**

![](data:image/svg+xml,%3Csvg%20xmlns='http://www.w3.org/2000/svg'%20viewBox='0%200%201024%20322'%3E%3C/svg%3E)

## **Can this code be improved even further?**

Absolutely! There are always ways to make our tests cleaner and more effective. Below are several ideas that could make this test even better.

### **Generating a product before a test**

We can generate product data using a REST API to make our tests fully independent.

As you probably know, dealing with test data is one of the biggest challenges in test automation. Sometimes we don’t have access to a REST API, or it’s too complex to use, but the goal should always be to keep our tests independent and not rely on existing static data.

Of course, if your application uses stable predefined data, you may not need to generate it every time — it depends on your project. However, when we create fresh data before each test, we avoid conflicts from reusing the same records and significantly improve overall test stability.

### **Cleaning up test data**

After running tests, we should clean up the test environment to maintain a clean state. This helps prevent unnecessary data accumulation, which can eventually slow down the environment.

### **ReviewFactory — creating review objects**

The Factory pattern is another useful design pattern in test automation. A factory class contains methods for creating specific objects — in our case, a Review object generated via an API.

You can also use such a class as a fixture to keep your project cleaner and more organized.

### **Cleaning Up Test Data**

After each test, clean up your environment — especially if your tests create new entities (like reviews, users, or products).

You can use test.afterEach() or API-based cleanup:

```ts

```

### **Navigation methods**

If the top navigation menu is available on every page, we can extract it into a separate component or include it in a base Page Object. This makes navigation easier to maintain and reuse.

### **Using seeds in Faker**

Faker allows us to set seeds, which ensures repeatability when generating random data. This is useful when debugging — we can reproduce the same data used in a failing test, making it easier to investigate issues related to specific inputs.

### **Faker – Generating Deterministic Test Data**

Faker is great for generating random data, but in automated tests, randomness can cause flaky results.

That’s why you should use **seeded randomness** to make your data deterministic and reproducible.

```ts

```

With a fixed seed, if a test fails, you can easily reproduce the same data and debug it reliably.

### **Parameterized tests**

Instead of hardcoding values like “Mugs” or product names, we can parameterize our tests to run with different datasets or configurations.

Instead of duplicating nearly identical tests, you can parameterize them for multiple datasets:

```ts

```

It makes your test suite more flexible and reduces maintenance.

### **Types and enums for stability**

An enum in TypeScript is a great way to avoid passing raw string values. Defining category names in an enum helps prevent typos and makes the code easier to maintain.

Instead of passing raw strings or magic numbers, define strong types and enums.

```plain

```

**Benefits:**

- Eliminates typos and hardcoded strings.
- Improves autocompletion and readability.
- Keeps the code consistent and predictable.

**expect()** **vs** **expect.soft()**

• expect() — stops the test execution when it encounters a failure.

• expect.soft() — collects all assertion errors without stopping the test.

When comparing multiple fields, expect.soft() can be especially helpful. At the end of the test, you can verify that no errors were collected:

```ts

```

## **Summary**

In today’s post, I demonstrated how to transform a simple test into a more structured, maintainable, and scalable one.

This kind of case study can be beneficial for those just starting their journey with test automation. It’s more valuable than simply showing the final refactored version, because walking through each step helps us understand the reasoning behind every improvement.

### See What’s On: Workshops & Consultations

Level up your skills — see current workshops!

[Check it out!](https://testingplus.me/test-automation-workshops/)

![](data:image/svg+xml,%3Csvg%20xmlns='http://www.w3.org/2000/svg'%20viewBox='0%200%20819%201024'%3E%3C/svg%3E)
