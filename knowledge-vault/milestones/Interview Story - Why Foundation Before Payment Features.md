# Interview Story - Why Foundation Before Payment Features

I built the foundation before payment features because payment systems need trustworthy structure before they need feature volume.

Phase 0 proves the backend, frontend, infrastructure, tests, docs, and learning notes can run together. It also keeps scope clean: no payment workflow, no PSP integration, no Kafka, and no fake dashboards.

From a QA perspective, this creates leverage. I can review module boundaries, setup reproducibility, test data isolation, and automation layers before business complexity arrives. That reduces future flakiness and makes later payment behavior easier to test with confidence.
