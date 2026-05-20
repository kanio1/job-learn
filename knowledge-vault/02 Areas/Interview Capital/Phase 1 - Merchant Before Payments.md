# Phase 1 - Merchant Before Payments

Merchant registry came before payment orders because payment orders need a real ownership boundary. Building payments first would force a fake merchant assumption into APIs, data, tests, and security.

## Interview Story

I introduced merchant registry as the first business capability so later payment behavior can answer: who owns this payment, is that merchant active, and which actor may operate on the data?

## Quality Angle

This made the system easier to test because validation, duplicate constraints, lifecycle transitions, authorization, and persistence could be proven before adding PSP or payment-flow complexity.
