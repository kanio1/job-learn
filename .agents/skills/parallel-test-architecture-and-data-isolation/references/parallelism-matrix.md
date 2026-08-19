# Parallelism Matrix

| Layer | Default stance | Main risk |
|---|---|---|
| Unit | High parallelism | hidden shared state |
| Spring integration | Controlled | context/data coupling |
| REST Assured | High if data isolated | shared business keys |
| WireMock | Dynamic isolation | shared stubs/ports |
| Testcontainers | Deliberate lifecycle | container/data confusion |
| Playwright | Worker-aware | shared users/resources |
