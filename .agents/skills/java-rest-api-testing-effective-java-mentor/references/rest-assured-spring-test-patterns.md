# REST Assured and Spring Test Patterns

Use these patterns as starting points. Always inspect repository dependencies and conventions before generating code.

## REST Assured Client Pattern

```java
public final class PaymentApiClient {
    private final RequestSpecification requestSpecification;

    public PaymentApiClient(RequestSpecification requestSpecification) {
        this.requestSpecification = Objects.requireNonNull(requestSpecification);
    }

    public PaymentResponse createPayment(PaymentRequest request) {
        return postPayment(request)
                .statusCode(201)
                .extract()
                .as(PaymentResponse.class);
    }

    public ValidatableResponse createInvalidPayment(Map<String, Object> malformedPayload) {
        return postPayment(malformedPayload);
    }

    private ValidatableResponse postPayment(Object payload) {
        return given()
                .spec(requestSpecification)
                .contentType(ContentType.JSON)
                .body(payload)
                .when()
                .post("/payments")
                .then();
    }
}
```

Notes:

- Keep public methods business-readable.
- Hide technical execution details in private helpers.
- Inject `RequestSpecification` so authentication, base URI, logging policy, and filters are configured outside the client.
- Never log sensitive headers or payload fields.

## Negative JSON Payload Builder Pattern

```java
public final class PaymentJsonPayloadBuilder {
    private final Map<String, Object> values = new LinkedHashMap<>();

    private PaymentJsonPayloadBuilder() {
        values.put("amount", "10.00");
        values.put("currency", "EUR");
        values.put("merchantId", "merchant-123");
    }

    public static PaymentJsonPayloadBuilder validPayment() {
        return new PaymentJsonPayloadBuilder();
    }

    public PaymentJsonPayloadBuilder without(String field) {
        values.remove(field);
        return this;
    }

    public PaymentJsonPayloadBuilder with(String field, Object value) {
        values.put(field, value);
        return this;
    }

    public Map<String, Object> build() {
        return Map.copyOf(values);
    }
}
```

Use `Map<String, Object>` deliberately for malformed JSON, missing fields, wrong types, and contract-negative tests. Do not use it as the default model for valid DTOs.

## Typed Response Extraction Pattern

```java
List<PaymentResponse> payments = given()
        .spec(requestSpecification)
        .when()
        .get("/payments")
        .then()
        .statusCode(200)
        .extract()
        .as(new TypeRef<List<PaymentResponse>>() {});

assertThat(payments)
        .extracting(PaymentResponse::status)
        .containsExactlyInAnyOrder(PaymentStatus.AUTHORIZED, PaymentStatus.DECLINED);
```

Use `TypeRef<List<T>>` for REST Assured generic list extraction. Use Jackson `TypeReference<T>` when using `ObjectMapper` directly.

## MockMvc Controller Test Pattern

```java
@WebMvcTest(PaymentController.class)
class PaymentControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PaymentService paymentService;

    @Test
    void createsPaymentWhenRequestIsValid() throws Exception {
        PaymentRequest request = PaymentRequests.validCardPayment();
        PaymentResponse response = new PaymentResponse("pay_123", PaymentStatus.AUTHORIZED);

        given(paymentService.createPayment(request)).willReturn(response);

        mockMvc.perform(post("/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("pay_123"))
                .andExpect(jsonPath("$.status").value("AUTHORIZED"));
    }
}
```

Notes:

- Use `@WebMvcTest` for focused controller/web-layer behavior.
- Mock the service boundary, not DTOs.
- Validate status code and meaningful response body.
- Add validation error shape assertions for negative cases.

## SpringBootTest Integration Test Pattern

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PaymentApiIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void rejectsPaymentWhenAmountIsMissing() {
        Map<String, Object> payload = PaymentJsonPayloadBuilder.validPayment()
                .without("amount")
                .build();

        ResponseEntity<ProblemDetail> response = restTemplate.postForEntity(
                "/payments",
                payload,
                ProblemDetail.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody())
                .extracting(ProblemDetail::getTitle)
                .isEqualTo("Validation failed");
    }
}
```

Use `@SpringBootTest` only when a fuller context is justified: serialization, validation, security filters, database behavior, or cross-layer integration.

## Mockito Captor Pattern

```java
@Test
void sendsCommandWithNormalizedAmount() {
    PaymentRequest request = PaymentRequests.validCardPayment().withAmount("10.0");

    paymentFacade.createPayment(request);

    ArgumentCaptor<CreatePaymentCommand> captor = ArgumentCaptor.forClass(CreatePaymentCommand.class);
    then(paymentService).should().create(captor.capture());

    assertThat(captor.getValue())
            .extracting(CreatePaymentCommand::amount)
            .isEqualTo(new BigDecimal("10.00"));
}
```

Use captors when you need post-verification assertions on transformed immutable arguments. Avoid verifying every internal call.

## AssertJ Collection Assertion Pattern

```java
assertThat(payments)
        .filteredOn(payment -> payment.status() == PaymentStatus.AUTHORIZED)
        .extracting(PaymentResponse::id, PaymentResponse::currency)
        .containsExactly(tuple("pay_123", "EUR"));

assertThat(errors)
        .extracting(ValidationError::field, ValidationError::code)
        .containsExactlyInAnyOrder(
                tuple("amount", "NotNull"),
                tuple("currency", "Pattern")
        );
```

Prefer AssertJ extracting/filtering over manual loops when it makes the test oracle clearer.
