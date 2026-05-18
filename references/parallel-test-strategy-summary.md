# Parallel Test Strategy Summary

| Layer | Initial Approach | Isolation Focus |
|---|---|---|
| Unit | High parallelism | No shared state |
| Spring integration | Classes parallel first | DB/data/context discipline |
| REST Assured | Parallel-capable | Unique business keys |
| WireMock | Isolated instances/dynamic ports | No shared mutable stub state |
| Testcontainers | Deliberate lifecycle | Data isolation independent of container sharing |
| Playwright | Worker-aware | Worker-specific users/resources |
