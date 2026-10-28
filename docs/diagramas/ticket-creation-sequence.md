# Ticket Creation and Assignment Sequence

Ticket registration, assignment and audit history share a transaction. Dispatch is synchronous with bounded timeouts; failure allows the internal eligibility and ranking rules to continue.

```mermaid
sequenceDiagram
  actor Requester
  participant API as support-ticket-service
  participant UseCase as CreateTicketService
  participant SLA as SLA policy repository
  participant Dispatch as technician-dispatch-service
  participant Rules as TicketAssignmentPolicy
  participant DB as PostgreSQL
  Requester->>API: POST /tickets with JWT
  API->>UseCase: create command
  UseCase->>DB: begin transaction
  UseCase->>SLA: read priority policy
  SLA-->>UseCase: resolution hours
  UseCase->>UseCase: calculate deadline and create ABIERTO ticket
  UseCase->>Dispatch: POST /technician-dispatch (ticketId, categoria, prioridad)
  alt Dispatch responds
    Dispatch-->>UseCase: technician suggestion and estimate
  else Dispatch errors or times out
    Dispatch--xUseCase: unavailable
    Note over UseCase: Continue without suggestion
  end
  UseCase->>Rules: check active status, specialty, capacity, site and SLA fit
  Rules-->>UseCase: eligible choice or no candidate
  alt Candidate selected
    UseCase->>DB: lock technician, then persist assignment, status and history
  else No candidate
    UseCase->>DB: persist open ticket and ASSIGNMENT_PENDING history
  end
  UseCase->>DB: commit transaction
  UseCase-->>API: ticket view
  API-->>Requester: 201 Created
```
