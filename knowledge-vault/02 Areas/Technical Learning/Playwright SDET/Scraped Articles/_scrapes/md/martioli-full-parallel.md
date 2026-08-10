How many workers should we setup in our config ?

How many shards should we use in our CI ?

Do we need fullyParallel true in Playwright config ? What does it even mean?

If you ever asked any of these questions, then you are in luck. Here is a way you can find your answers. Its much easier to understand things when you do them yourself, however if you are here just to quickly see the answers, then skip the setup part and go straight to results

## **SETUP:**

Install playwright using the following command

```cmd
npm init playwright@latest
```

Now at this point. Lets write some tests with the purpose to discover ourselves how playwright will split our tests in order to achieve full parallelization. Create a file for your test (my example I named it `para_1.spec.ts`. Name it however you want and add tests with testInfo logging for parallelIndex and shard.

We have:

- 2 Test Suites (describe block)
- Each test suite has 4 tests. A total of 8 tests in our first spec file

> Pro tip: we can have a look under the hood at our configs live during the test run using this little hack, of putting `testInfo` as a second argument

Our second spec file has one test suite with 4 tests.

Our third file `serial.spec.ts` runs in SERIAL mode using `test.describe.configure({ mode: 'serial' });`

Now lets configure our playwright to use full parallelization

Inside our `playwright.config.ts` file:

```typescript
import { defineConfig, devices } from '@playwright/test';

export default defineConfig({
  testDir: './tests',
  fullyParallel: true,
  workers: process.env.CI ? 3 : 1,
  reporter: 'html',
  projects: [
    {
      name: 'x',
      use: { ...devices['Desktop Chrome'] },
    }
  ],
});
```

`workers: process.env.CI ? 3 : 1` if we are in CI it will setup to run using 3 workers, if not it will run with 1 worker.

`fullyParallel: true` means that this will spawn workers and have spec files assigned to them, but also the test suites (describe blocks) are mixed, and the tests inside are also mixed. This is what it means **full parallelization**, work is balanced not just per spec file, but also per describe block and even per tests.

## RESULTS:

See example results using parallelization with two shards (machines).

Besides **full parallelization,** when you are running in CI, you can balance the load on multiple machines using sharding.

We have to pay attention at worker id 1. Notice how the whole suite from serial file and all tests are in the same worker. Tests run in order 1 2 3 4.

From Playwright point of view, because serial.spec.ts file was in serial mode, it considered all of it as a whole and it did not try to split the tests, only the spec files got balanced.

You can see the explanation that even if you don't have `fullyParallel` set to true, it will still run in parallel but it will not be as they call it FULL parallel mode, because they perform load balancing at spec level.

Just for the fun of it. See below **RESULTS for a normal setup** without sharding or `fullyParallel` turned on. But still with 3 workers on just one machine.

It split the spec files only. It ran in parallel with 3 workers indeed. And it ran in order.

Its safe to say that this is not ideal, and most likely will cost you time and we all know that time is money.

**But what about the workers?** How would I know how many workers my setup can handle? Well, Butch Mayhew explains a way that you can test your environments and find out what are the optimal values to choose.

**What about sharding? How many machines should I choose?** Well the answer depends on your own setup. Factors:

- A new machine (use of sharding) means a new instance means more costs
- Sometimes on some projects when you spin up a machine the setup to get everything up and running for the tests to execute takes a long time
- A machine can have a certain capacity on its CPU so overloading one machine with multiple workers may be more heavier than just having more machines with less workers for each.
