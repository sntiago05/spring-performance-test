# Hexagonal Architecture by Service

The ticket service owns business rules and persistence; dispatch implements a small stateless selector. Input ports expose use cases, while output ports isolate external dependencies.

```mermaid
flowchart LR
  subgraph ticket["support-ticket-service"]
    web["REST controllers and DTOs"] --> inports["Input ports / use cases"]
    job["SLA scheduler"] --> inports
    inports --> domain["Domain model and policies"]
    inports --> outports["Output ports"]
    outports --> jpa["JPA adapters and MapStruct"]
    outports --> client["Dispatch REST adapter"]
    outports --> security["JWT and password adapters"]
    outports --> metrics["Micrometer adapter"]
    jpa --> db[(PostgreSQL)]
  end
  subgraph dispatch["technician-dispatch-service"]
    rest["REST controller"] --> suggestPort["SuggestTechnicianUseCase"]
    suggestPort --> selector["CRC32 deterministic selector"]
  end
  client --> rest
```
