# Ticket Lifecycle

Transitions mirror `TicketStatus` and the web API. Technicians may manage only their assigned tickets; cancellation is administrator-only. Administrative assignment/reassignment moves an open or in-progress ticket to `ASIGNADO`.

```mermaid
stateDiagram-v2
  [*] --> ABIERTO: Requester creates ticket
  ABIERTO --> ASIGNADO: System or admin assigns
  ABIERTO --> CANCELADO: Admin
  ASIGNADO --> EN_PROGRESO: Assigned technician or admin
  ASIGNADO --> CANCELADO: Admin
  EN_PROGRESO --> ASIGNADO: Admin reassigns
  EN_PROGRESO --> RESUELTO: Assigned technician or admin
  EN_PROGRESO --> CANCELADO: Admin
  RESUELTO --> EN_PROGRESO: Admin reopens
  RESUELTO --> CERRADO: Technician or admin closes
  CERRADO --> [*]
  CANCELADO --> [*]
```
