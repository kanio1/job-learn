# Parallel Test Data Isolation — Database Lens

Patterns:
- per-test unique references
- per-worker merchant/user data
- cleanup by owner/tag
- rollback only where technically valid
- schema-per-worker only when suite complexity justifies it
