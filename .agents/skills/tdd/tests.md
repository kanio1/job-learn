# Good and Bad Tests

## Good

Observable behavior through the public seam.

REST Assured — HTTP is the interface:

```java
@Test
void createsPaymentOrderAndMakesItRetrievable() {
    String reference = uniqueReference();

    String location = given()
        .header("Idempotency-Key", uniqueKey())
        .body(createBody(reference))
        .when().post("/api/merchants/{id}/payment-orders", merchantId)
        .then().statusCode(201)
        .extract().header("Location");

    given().when().get(location)
        .then().statusCode(200)
        .body("clientOrderReference", equalTo(reference))
        .body("status", equalTo("CREATED"));
}
```

Playwright E2E — the user-visible capability:

```ts
test('merchant manager can open a created payment order', async ({ page }) => {
  await page.getByRole('link', { name: reference }).click()
  await expect(page.getByRole('heading', { name: reference })).toBeVisible()
})
```

Playwright REST — BFF contract:

```ts
test('BFF create payment returns the order schema', async ({ request }) => {
  const response = await createPayment(request, uniqueLiveReference())
  expect(response.status()).toBe(201)
  paymentOrderSchema.parse(await response.json())
})
```

Characteristics: WHAT not HOW; public API only; survives internal refactors; independent expected values.

## Bad

Implementation-detail:

```java
@Test
void authorizeCallsPaymentServiceAuthorize() {
    verify(paymentService).authorize(eq(orderId), any());
}
```

Tautological:

```java
int expected = items.stream().mapToInt(Item::price).sum();
assertThat(total(items)).isEqualTo(expected);
```

Use a known literal from the spec (`1500` minor units) instead.

Wrong layer:

```ts
// E2E that re-implements the REST Assured 409 duplicate-reference matrix
test('API returns 409 for duplicate reference', async ({ page }) => { /* ... */ })
```

Put that case on the REST Assured or Playwright REST seam.
