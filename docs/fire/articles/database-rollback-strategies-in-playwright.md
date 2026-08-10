# Database Rollback Strategies in Playwright

| Field | Value |
|-------|-------|
| **Author** | Irfan Mujagic |
| **Published** | 2025-10-21 |
| **URL** | https://www.thegreenreport.blog/articles/database-rollback-strategies-in-playwright/database-rollback-strategies-in-playwright.html |
| **Scraped with** | Firecrawl `firecrawl_scrape` (`formats: ["markdown"]`, `onlyMainContent: true`) |

## Firecrawl metadata

```json
{
  "title": "The Green Report | Database Rollback Strategies in Playwright",
  "og:title": "The Green Report | Database Rollback Strategies in Playwright",
  "author": "Irfan Mujagic",
  "publishedTime": "2025-10-21",
  "og:url": "https://www.thegreenreport.blog/articles/database-rollback-strategies-in-playwright/database-rollback-strategies-in-playwright.html",
  "description": "A blog dedicated to Quality Assurance in Software Engineering",
  "statusCode": 200,
  "sourceURL": "https://www.thegreenreport.blog/articles/database-rollback-strategies-in-playwright/database-rollback-strategies-in-playwright.html"
}
```

---

### Database Rollback Strategies in Playwright

Oct 21st 202516 min read

![](https://www.thegreenreport.blog/articles/database-rollback-strategies-in-playwright/images/banner.webp)

medium

playwright1.56.1

typescript5.7.2

prisma6.17.1

database

integration

If you've worked with automated testing for any length of time, you've probably encountered the pain
of flaky tests caused by leftover test data. One test creates a user with email "test@example.com",
another test tries to create the same user and fails. Or worse, tests pass when run individually but
fail when run together. These problems stem from poor test data management, and database rollback
transactions offer an elegant solution.


In this post, you'll learn how to implement database rollback strategies in Playwright to ensure
your tests remain independent, fast, and reliable. We'll explore practical approaches with simple
code examples that you can adapt to your own projects.


#### Understanding the Problem

When you write automated tests that interact with a database, each test typically creates, updates,
or deletes data. Without proper cleanup, this data accumulates and causes several issues. Tests
become dependent on execution order, making your test suite fragile. Running tests in parallel
becomes risky because tests interfere with each other. Your database gradually fills with junk data,
making it harder to debug failing tests.


The traditional solution is to manually clean up after each test by deleting the records you
created. This works but has downsides. You need to remember every piece of data your test touched.
The cleanup code can become complex, especially for related records across multiple tables. Most
importantly, manual cleanup is slow because each deletion is a separate database operation.


Database transaction rollback solves these problems elegantly. You start a transaction before your
test runs, let the test do whatever it needs with the database, then roll back the transaction
afterward. All changes disappear instantly, leaving your database exactly as it was before the test.


#### Prerequisites

Before implementing rollback strategies in [Playwright](https://playwright.dev/), you'll need a few things in place. You should
have Playwright already installed and configured in your project. You'll need a way to connect to
your database, whether that's through an ORM like [Prisma](https://www.prisma.io/) or [Sequelize](https://sequelize.org/), or a direct database client
like pg for PostgreSQL or mysql2 for MySQL. Finally, you should have basic familiarity with database
transactions and how they work.


The examples in this post use PostgreSQL and Prisma, but the concepts apply to any database that
supports transactions. You can easily adapt the code to your specific database and connection
library.


#### Approach 1: Database-Level Rollback with Setup/Teardown Hooks

##### Setting Up Database Connections

The first step is establishing a database connection that your tests can use. In Playwright, you
typically do this in a global setup file or a test fixture. Here's a simple example using Prisma:


```typescript

import { PrismaClient } from '@prisma/client';

export const prisma = new PrismaClient();

```

If you're using a direct database client instead of an ORM, the setup looks similar:


```typescript

import { Pool } from 'pg';

export const pool = new Pool({
  host: 'localhost',
  database: 'testdb',
  user: 'testuser',
  password: 'testpass'
});

```

##### Implementing Transaction Rollback in beforeEach/afterEach

Now comes the core implementation. You'll use Playwright's test hooks to start a transaction before
each test and roll it back afterward. Here's how it looks with Prisma:


```typescript

import { test } from '@playwright/test';
import { prisma } from './fixtures/database';

test.beforeEach(async () => {
  await prisma.$executeRaw`BEGIN`;
});

test.afterEach(async () => {
  await prisma.$executeRaw`ROLLBACK`;
});

test('user registration creates new user', async () => {
  const user = await prisma.user.create({
    data: {
      email: 'test@example.com',
      name: 'Test User'
    }
  });

  expect(user.id).toBeDefined();
  expect(user.email).toBe('test@example.com');
});

```

Every change made during the test gets rolled back automatically. The user record never actually
persists in the database. When the next test runs, it starts with a clean slate.

With a direct database client, the pattern is the same:


```typescript

import { test } from '@playwright/test';
import { pool } from './fixtures/database';

let client;

test.beforeEach(async () => {
  client = await pool.connect();
  await client.query('BEGIN');
});

test.afterEach(async () => {
  await client.query('ROLLBACK');
  client.release();
});

test('creates order successfully', async () => {
  const result = await client.query(
    'INSERT INTO orders (user_id, total) VALUES ($1, $2) RETURNING *',
    [1, 99.99]
  );

  expect(result.rows[0].total).toBe('99.99');
});

```

##### Handling Nested Transactions

Sometimes your application code uses transactions internally. When you're already inside a test
transaction, starting another transaction can cause problems. Most databases handle this through
savepoints. Here's how to manage nested transactions:


```typescript

test.beforeEach(async () => {
  await prisma.$executeRaw`BEGIN`;
});

test('handles nested transaction in application code', async () => {
  // Your app code might do this internally
  await prisma.$transaction(async (tx) => {
    await tx.user.create({ data: { email: 'nested@example.com' } });
    await tx.order.create({ data: { userId: 1, total: 50 } });
  });

  // Everything still rolls back in afterEach
});

test.afterEach(async () => {
  await prisma.$executeRaw`ROLLBACK`;
});

```

Prisma and most ORMs handle savepoints automatically, so nested transactions just work. If you're
using raw SQL, you might need to use SAVEPOINT and ROLLBACK TO SAVEPOINT explicitly.


#### Approach 2: Using Prisma with Playwright

##### Configuring Prisma for Test Transactions

Prisma offers some special features that make transaction testing even easier. You can create a
custom Prisma client instance specifically for testing that's wrapped in a transaction. First, set
up a test helper:


```typescript

import { PrismaClient } from '@prisma/client';

const prisma = new PrismaClient();

export async function getTestClient() {
  return prisma;
}

export async function cleanupTestClient() {
  await prisma.$disconnect();
}

```

##### Creating a Test Helper Class

For more complex scenarios, wrapping the transaction logic in a helper class keeps your tests clean.
Here's a simple but powerful helper:


```typescript

import { PrismaClient } from '@prisma/client';

export class TestDatabase {
  private prisma: PrismaClient;

  constructor() {
    this.prisma = new PrismaClient();
  }

  async startTransaction() {
    await this.prisma.$executeRaw`BEGIN`;
  }

  async rollback() {
    await this.prisma.$executeRaw`ROLLBACK`;
  }

  getClient() {
    return this.prisma;
  }

  async cleanup() {
    await this.prisma.$disconnect();
  }
}

```

Now your tests become much cleaner:


```typescript

import { test } from '@playwright/test';
import { TestDatabase } from './helpers/TestDatabase';

const testDb = new TestDatabase();

test.beforeEach(async () => {
  await testDb.startTransaction();
});

test.afterEach(async () => {
  await testDb.rollback();
});

test('creates product with category', async () => {
  const db = testDb.getClient();

  const category = await db.category.create({
    data: { name: 'Electronics' }
  });

  const product = await db.product.create({
    data: {
      name: 'Laptop',
      price: 999,
      categoryId: category.id
    }
  });

  expect(product.categoryId).toBe(category.id);
});

```

##### Example: Testing a User Registration Flow

Let's look at a complete example testing a realistic user registration flow. This shows how rollback
keeps your tests isolated even when dealing with multiple related records:


```typescript

import { test, expect } from '@playwright/test';
import { TestDatabase } from './helpers/TestDatabase';

const testDb = new TestDatabase();

test.beforeEach(async () => {
  await testDb.startTransaction();
});

test.afterEach(async () => {
  await testDb.rollback();
});

test('user registration creates user and profile', async () => {
  const db = testDb.getClient();

  // Create user
  const user = await db.user.create({
    data: {
      email: 'newuser@example.com',
      password: 'hashedpassword123',
      profile: {
        create: {
          firstName: 'John',
          lastName: 'Doe',
          bio: 'Software developer'
        }
      }
    },
    include: { profile: true }
  });

  expect(user.email).toBe('newuser@example.com');
  expect(user.profile.firstName).toBe('John');

  // Verify user can be found
  const foundUser = await db.user.findUnique({
    where: { email: 'newuser@example.com' }
  });

  expect(foundUser).not.toBeNull();
});

test('cannot create duplicate email', async () => {
  const db = testDb.getClient();

  await db.user.create({
    data: { email: 'duplicate@example.com', password: 'pass123' }
  });

  await expect(
    db.user.create({
      data: { email: 'duplicate@example.com', password: 'pass456' }
    })
  ).rejects.toThrow();
});

```

Both tests run independently. The first test creates a user that gets rolled back. The second test
also creates a user, tests the duplicate constraint, and that user also gets rolled back. Neither
test affects the other.


#### Approach 3: API-Level Seeding and Cleanup

##### When Rollback Isn't Enough

Database rollback works perfectly for testing your data layer and business logic. However, sometimes
you need to test the complete stack including UI interactions through Playwright's browser
automation. In these cases, you want to see the data changes reflected in the UI, which means you
can't roll back until after the browser interactions complete.


For end to end tests, you often need a hybrid approach. You set up initial data through API calls or
direct database inserts, run your UI test, then clean up afterward. Rollback can still help by
wrapping the entire test including the UI interactions.


##### Combining API Calls with Database Rollback

Here's how to combine browser testing with database rollback:


```typescript

import { test, expect } from '@playwright/test';
import { TestDatabase } from './helpers/TestDatabase';

const testDb = new TestDatabase();

test.beforeEach(async () => {
  await testDb.startTransaction();
});

test.afterEach(async () => {
  await testDb.rollback();
});

test('user can log in and see dashboard', async ({ page }) => {
  const db = testDb.getClient();

  // Set up test data
  const user = await db.user.create({
    data: {
      email: 'uitest@example.com',
      password: 'hashed_password'
    }
  });

  // Commit this data so the app can see it
  await db.$executeRaw`COMMIT`;
  await db.$executeRaw`BEGIN`;

  // Now run UI test
  await page.goto('http://localhost:3000/login');
  await page.fill('input[name=\"email\"]', 'uitest@example.com');
  await page.fill('input[name=\"password\"]', 'password123');
  await page.click('button[type=\"submit\"]');

  await expect(page.locator('h1')).toContainText('Dashboard');

  // Everything gets rolled back in afterEach
});

```

This approach commits the setup data so your application can see it, then starts a new transaction
for any data created during the test. Both transactions eventually get rolled back.


#### Best Practices

##### When to Use Rollback vs. Manual Cleanup

Transaction rollback shines for unit tests and integration tests of your data access layer. When
you're testing API endpoints or service functions that interact with the database, rollback keeps
tests fast and isolated. You don't need to worry about cleanup logic because the rollback handles
everything automatically.

However, rollback has limitations. Full end to end UI tests sometimes need the data to persist
through the entire test execution. Tests that verify email sending, file uploads, or third party API
calls need different cleanup strategies because those side effects happen outside the database
transaction.

A good rule of thumb is to use rollback for any test where the database is the only stateful
component. Use manual cleanup or other strategies when your test has side effects beyond the
database.

##### Handling External Dependencies

Some operations cannot be rolled back because they affect systems outside your database. When your
test sends an email, that email is sent regardless of database rollback. Uploaded files remain on
disk. API calls to external services cannot be undone by your database transaction.

For these scenarios, you have a few options. You can use mock services in your tests to prevent the
actual side effects. Many teams set up separate test instances of external services that can be
easily cleaned. For file uploads, you might use a temporary directory that gets cleared between test
runs.

The key is identifying which side effects need special handling and addressing them separately from
your database cleanup strategy.

##### Parallel Test Execution Considerations

Playwright excels at running tests in parallel to speed up your test suite. Transaction rollback
actually makes parallel execution safer because each test runs in its own transaction. Changes made
by one test don't affect others, even when they run simultaneously.

However, you need to ensure each test gets its own database connection. Connection pooling usually
handles this automatically, but be aware of your database's connection limits. If you have 10
parallel tests and each needs a connection, your pool needs at least 10 connections available.

One potential issue is lock contention. If multiple tests try to modify the same database rows
simultaneously, some tests might wait for locks held by others. This slows down execution but
doesn't cause correctness issues. You can minimize contention by designing tests to work with
different data sets when possible.

##### Test Isolation Patterns

Good test isolation goes beyond just cleaning up data. Each test should set up exactly the data it
needs rather than depending on a shared baseline dataset. This makes tests more maintainable because
you can understand what a test does by reading just that test, without needing to know about global
setup.

Consider using factory functions or builder patterns to create test data consistently:


```typescript

async function createTestUser(db, overrides = {}) {
  return db.user.create({
    data: {
      email: 'test@example.com',
      password: 'password123',
      ...overrides
    }
  });
}

test('user profile update', async () => {
  const db = testDb.getClient();
  const user = await createTestUser(db, { email: 'specific@example.com' });

  // Test continues...
});

```

This pattern keeps your tests DRY while maintaining independence. Each test explicitly creates the
user it needs with any specific properties required for that test.

#### Common Pitfalls and Solutions

##### Connection Pool Exhaustion

If your test suite runs hundreds or thousands of tests, you might exhaust your database connection
pool. This happens when tests don't properly release connections or when you create too many
simultaneous connections during parallel execution.

The solution is ensuring your database client properly manages connections. Most libraries handle
this automatically, but verify your afterEach hook releases connections:


```typescript

let client;

test.beforeEach(async () => {
  client = await pool.connect();
  await client.query('BEGIN');
});

test.afterEach(async () => {
  await client.query('ROLLBACK');
  client.release(); // Important: release the connection
});

```

You can also increase your connection pool size if needed, though this should be a last resort after
ensuring proper connection management.

##### Auto-Commit Issues

Some database operations automatically commit regardless of your transaction state. These typically
include DDL statements like CREATE TABLE or ALTER TABLE. If your tests modify the database schema,
those changes cannot be rolled back.

The solution is keeping schema changes separate from your test data changes. Run migrations in a
global setup phase before any tests execute. Your tests should only insert, update, and delete data,
never modify the schema.

##### Transaction Timeout Problems

Long running tests can exceed your database's transaction timeout settings. If a test takes several
minutes to complete, the database might automatically roll back the transaction before your test
finishes.

For most unit and integration tests, this isn't an issue because they complete quickly. If you have
legitimately long running tests, consider whether they belong in your regular test suite or should
be part of a separate performance or load testing suite. You can also increase transaction timeout
settings in your test database configuration, though this might hide performance problems in your
tests.

##### Performance Comparison

The speed difference between rollback and manual cleanup can be dramatic. In a test suite with 100
tests, manual cleanup might take 2 to 3 seconds per test for a total runtime of 3 to 5 minutes. With
transaction rollback, each test might complete in under 1 second, bringing the total runtime down to
1 to 2 minutes.

The exact numbers depend on your database, schema complexity, and test operations. However, rollback
consistently outperforms manual cleanup because it's a single database operation versus multiple
DELETE statements.

Database recreation, where you drop and recreate the entire database between tests, is even slower.
This approach can take 10 to 30 seconds per test, making it impractical for any substantial test
suite.

#### Conclusion

Database transaction rollback transforms how you write and maintain automated tests. Your tests
become faster, more reliable, and easier to understand. You eliminate entire categories of flaky
test problems caused by data pollution and test interdependencies.

The implementation is straightforward regardless of your database or ORM. Start a transaction before
each test, run your test, roll back afterward. This simple pattern solves complex test data
management problems that have plagued testing for decades.

Start with the basic approach using beforeEach and afterEach hooks. As your test suite grows,
consider extracting the transaction logic into helper classes or fixtures. Choose the approach that
best fits your project's complexity and your team's preferences.

The key is consistency. Once you adopt transaction rollback, apply it consistently across your test
suite. Your future self will thank you when you're debugging a complex feature and your tests remain
stable and trustworthy.

You can find a complete working example with all the code from this post in our [GitHub repository](https://github.com/Crypted39/the-green-report-examples/tree/master/database-rollback-strategies-in-playwright)
linked below.
