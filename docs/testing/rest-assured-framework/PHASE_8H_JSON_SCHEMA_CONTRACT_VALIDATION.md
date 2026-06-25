# Phase 8H — JSON Schema / Response Contract Validation Foundation

## Goal

Activate the `json-schema-validator` dependency (previously deferred since Phase 4 because the
JAR was not in the local Maven cache), write JSON Schema files for the three most significant
response shapes, implement `SchemaAssertions` and activate `ProblemAssert.matchesProblemSchema()`,
and add 3 schema-backed live assertions in a focused spec class.

## Dependency status before this phase

The `io.rest-assured:json-schema-validator:6.0.0` dependency was commented out in `pom.xml`
since Phase 4 with the note "add when json-schema-validator is in local `~/.m2`".
The JAR and all its transitive dependencies were resolved from Maven Central during Phase 8H
discovery (2026-06-25):

| Artifact | Version | Source |
|---|---|---|
| `io.rest-assured:json-schema-validator` | 6.0.0 | Maven Central |
| `com.github.java-json-tools:json-schema-validator` | 2.2.14 | transitive |
| `com.github.java-json-tools:json-schema-core` | 1.2.14 | transitive |
| `com.github.java-json-tools:jackson-coreutils` | 2.0 | transitive |
| `com.github.java-json-tools:jackson-coreutils-equivalence` | 1.0 | transitive |
| `com.github.java-json-tools:uri-template` | 0.10 | transitive |
| `com.github.java-json-tools:msg-simple` | 1.2 | transitive |
| `com.github.java-json-tools:btf` | 1.3 | transitive |
| `com.google.guava:guava` | 33.2.1-jre | transitive (already cached) |
| `org.hamcrest:hamcrest` | 3.0 | transitive (already cached) |

All JARs are now in `~/.m2/repository` and the build is fully offline-safe.

## Files Added / Modified

| File | Change |
|------|--------|
| `apps/api-tests/pom.xml` | Uncommented `json-schema-validator` dep, uncommented version property |
| `apps/api-tests/src/test/resources/schema/payment-order.schema.json` | New — payment order response schema |
| `apps/api-tests/src/test/resources/schema/payment-summary.schema.json` | New — payment summary response schema |
| `apps/api-tests/src/test/java/lab/paymentquality/apitest/core/problem/SchemaAssertions.java` | New — static helper wrapping `matchesJsonSchemaInClasspath` |
| `apps/api-tests/src/test/java/lab/paymentquality/apitest/core/problem/ProblemAssert.java` | Activated `matchesProblemSchema()` (was `UnsupportedOperationException`) |
| `apps/api-tests/src/test/java/lab/paymentquality/apitest/scenarios/JsonSchemaContractSpec.java` | New spec: 3 schema-backed live tests |
| `docs/testing/rest-assured-framework/REST_ASSURED_BLACK_BOX_FRAMEWORK_PLAN.md` | Phase 8H row added |

Note: `schema/problem.schema.json` already existed as a draft from Phase 4. No changes needed.

## Schema Files

### `schema/problem.schema.json` (pre-existing)

Validates `application/problem+json` error responses. Uses `additionalProperties: true` because
two different exception handlers (`GlobalExceptionHandler` and `PaymentExceptionHandler`) produce
slightly different field sets. The schema validates the intersection — fields always present in
both handlers — without being brittle about handler-specific extras.

Required fields (validated): `status` (integer 400–599), `error` (string), `correlationId` (string).
Optional: `type`, `title`, `detail`, `message`, `code`, `details`.

### `schema/payment-order.schema.json` (new)

Validates `GET /api/merchants/{merchantId}/payment-orders/{paymentOrderId}` response.

**Key design decisions:**

- `additionalProperties: false` — catches undeclared fields and field renames. If the backend
  adds a new field, this schema fails intentionally; update the schema to accept the new field.
- Required fields: `paymentOrderId`, `merchantId`, `clientOrderReference`, `amountMinor`,
  `currency`, `status`, `createdAt`, `updatedAt` — always present regardless of lifecycle status.
- Optional nullable: all lifecycle timestamps and amounts (`authorizedAt`, `capturedAt`,
  `capturedAmountMinor`, etc.) are `"type": ["string", "null"]` or `["integer", "null"]` —
  null for orders that have not yet reached the corresponding state.
- `metadata`: typed as `anyOf: [null, object{additionalProperties: string}]` because the
  field is null for unpatched orders and a JSON object after PATCH.
- Status enum: not constrained — avoids schema churn if a new lifecycle status is added.
- Timestamp format: not constrained (type: string only) — ISO-8601 format is stable but
  exact format assertions belong in field-level checks, not structural schema.

### `schema/payment-summary.schema.json` (new)

Validates `GET /api/merchants/{merchantId}/payment-orders/summary` response.

**Key design decisions:**

- `additionalProperties: false` at root **and** within `byCurrency`/`byStatus` item schemas.
  This catches field renames in the aggregate projection (e.g. `orderCount` → `count`,
  `totalAmountMinor` → `totalAmount`).
- Both `byCurrency` and `byStatus` items have `required` arrays — if the backend drops a field
  from the projection, the schema fails even if the array is non-empty.
- `minimum: 0` on all integer fields — catches negative values that would indicate a DB bug.

## SchemaAssertions helper

```java
// src/.../core/problem/SchemaAssertions.java

public static void matchesProblemSchema(Response response)         // schema/problem.schema.json
public static void matchesPaymentOrderSchema(Response response)    // schema/payment-order.schema.json
public static void matchesPaymentSummarySchema(Response response)  // schema/payment-summary.schema.json
```

Internally uses `matchesJsonSchemaInClasspath(path)` from `io.restassured.module.jsv.JsonSchemaValidator`
wrapped in `MatcherAssert.assertThat(response.asString(), matcher)` (Hamcrest 3.0).

On failure: throws `AssertionError` with the full schema validation report — all violations,
not just the first. JUnit 5 displays this as a test failure with a detailed message.

## ProblemAssert.matchesProblemSchema()

The previously stubbed-out method now delegates to `SchemaAssertions.matchesProblemSchema(actual)`:

```java
// Before (Phase 4 stub):
public ProblemAssert matchesProblemSchema() {
    throw new UnsupportedOperationException("requires json-schema-validator...");
}

// After (Phase 8H activation):
public ProblemAssert matchesProblemSchema() {
    isNotNull();
    SchemaAssertions.matchesProblemSchema(actual);
    return this;  // fluent chain — can follow hasError(), hasStatus(), etc.
}
```

**Usage** (can now be added to any ProblemAssert chain):
```java
ProblemAssert.assertThat(response)
    .hasStatus(404)
    .hasError(ProblemCodes.NOT_FOUND)
    .hasCorrelationId()
    .matchesProblemSchema();
```

## Tests Added (JsonSchemaContractSpec)

### Test 1: `get_payment_order_response_conforms_to_payment_order_schema`

**Scenario:** GET seeded `PAYMENT_ORDER_ALPHA_001_CREATED_ID` → 200 body.

**Why CREATED status:** all lifecycle timestamps are null, verifying the schema correctly
accepts null for optional lifecycle fields. If the backend drops null handling and starts
omitting nullable fields entirely, the schema's `required` array would catch it.

**What schema catches that AssertJ misses:** if `clientOrderReference` is renamed to
`orderReference`, all tests that assert `status` or `amountMinor` pass.
This schema test fails because `clientOrderReference` is in `required` and `orderReference`
is blocked by `additionalProperties: false`.

### Test 2: `get_404_error_response_conforms_to_problem_schema`

**Scenario:** GET non-existent UUID → 404 `not_found` → problem body.

**Pattern:** field checks first (`ProblemAssert.hasStatus(404).hasError(NOT_FOUND)`),
schema check second. The field checks confirm it's the right error code; the schema check
confirms the body structure is well-formed.

**What schema catches that AssertJ misses:** if `status` body field changes from integer `404`
to string `"404"`, `ProblemAssert.hasStatus(404)` still passes (it checks the HTTP status code,
not the JSON body field). The schema fails because `"status": {"type": "integer"}`.

### Test 3: `get_payment_summary_response_conforms_to_payment_summary_schema`

**Scenario:** GET summary for seeded merchant → 200 body with non-empty `byCurrency`/`byStatus`.

**Why non-empty arrays matter:** JSON Schema only validates item schemas when items exist.
Testing with a non-empty dataset confirms the nested `byCurrency` / `byStatus` item schemas
are exercised, not just the root-level shape.

**What schema catches that SoftAssertions misses:** if the backend renames `orderCount` to
`count` in the aggregate JPQL projection, the explicit assertions on `byCurrency[0].orderCount`
would fail — but only if those specific indices are tested. The schema test fails for any item
in either array, regardless of position.

## REST Assured API: matchesJsonSchemaInClasspath

```java
// Standard REST Assured schema validation with ValidatableResponse:
given().when().get(url).then()
    .body(matchesJsonSchemaInClasspath("schema/payment-order.schema.json"));

// With raw Response objects (used in this framework — all facades return Response):
String body = response.asString();
MatcherAssert.assertThat(body, matchesJsonSchemaInClasspath("schema/payment-order.schema.json"));
```

This framework wraps the second form in `SchemaAssertions` to keep scenario code clean.

**Why `matchesJsonSchemaInClasspath` and not a file path?**
`matchesJsonSchemaInClasspath` looks up the schema relative to the test classpath root.
`src/test/resources/schema/` is on the test classpath via Maven's standard layout.
This is portable — schema files are packaged into the test JAR and found regardless of
working directory.

## Design principles applied

**Schema strategy (1 schema test per response type, not 1 per endpoint call):**
If every test called `matchesPaymentOrderSchema()`, any legitimate contract expansion
(adding a new field) would break 25+ tests simultaneously. One dedicated schema test
contains the blast radius to one test per response type.

**Why `additionalProperties: false` in some schemas:**
Strict schemas catch field renames and additions that `additionalProperties: true` would
silently accept. The cost is that any intentional new field requires a schema update — which
is the desired friction. For error responses (`problem.schema.json`), `additionalProperties: true`
is used because two different exception handlers produce different field supersets and
locking down to one shape would require testing two schemas.

**Why timestamps are not format-validated:**
JSON Schema `"format": "date-time"` is an annotation in draft-07, not an assertion unless
the validator is configured with `checkFormats: true`. The java-json-tools validator does
not assert format by default. Even if it did, asserting `"2026-06-25T10:07:58Z"` as valid
ISO-8601 in a schema test adds little value over a field-level regex check in a dedicated
timestamp-format test.

## SDET Interview Topics

**Q: What is the difference between a JSON Schema test and a field-level assertion test?**

Field-level assertions are specific ("assert `error` equals `not_found`") and fail only for
what you checked. Schema tests are structural ("assert the body conforms to this shape contract")
and catch any field that was added, removed, renamed, or changed type — including ones no
individual assertion explicitly mentions. Both are needed: schema tests protect the contract;
field assertions verify the specific business rule.

**Q: Why use `additionalProperties: false` in a test schema?**

To catch silent drift. If a backend field is renamed (`clientOrderReference` → `orderReference`),
clients that serialized from the old field name break silently. Without `additionalProperties: false`,
the schema accepts both `clientOrderReference` and `orderReference` as valid unknowns.
With it, the schema fails on `orderReference` (undeclared) and fails on the missing required
`clientOrderReference`. The test tells you exactly what changed.

**Q: Why did you not add schema validation to every existing test?**

1. Schema validation adds test overhead and should be reserved for the "is this response shape
   correct?" question, which only needs to be asked once per response type on a stable resource.
2. Existing tests ask "does this specific business operation produce the correct result?" —
   that's a different question with different oracles.
3. Blast radius control: if a legitimate new field is added to the payment order response,
   only one test (`JsonSchemaContractSpec.get_payment_order_response_conforms_to_payment_order_schema`)
   needs updating, not every test that calls `getById`.

**Q: What does `matchesJsonSchemaInClasspath` do under the hood?**

It returns a Hamcrest `Matcher<String>`. When evaluated (via `MatcherAssert.assertThat`),
it:
1. Parses the JSON string.
2. Loads the schema file from the classpath.
3. Uses `com.github.java-json-tools:json-schema-validator:2.2.14` (JSON Schema draft-07) to
   validate the parsed JSON against the loaded schema.
4. Throws `AssertionError` with a detailed violation report if validation fails.

The `io.rest-assured:json-schema-validator:6.0.0` is a thin wrapper that integrates
java-json-tools into REST Assured's fluent API.

## Test Results

- **79 offline tests**: all pass (unchanged)
- **67 live tests**: all pass (3 new in Phase 8H)
