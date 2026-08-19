# Module Boundary Checklist

- Which package is the module root?
- What is part of public API?
- What is internal?
- Which dependencies are allowed?
- Are there cyclic dependencies?
- Is a direct call justified?
- Would an event reduce coupling or only add complexity?
- What tests protect the boundary?
