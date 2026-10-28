# HelpFlow Use Cases

Actors and system responsibilities shown below follow the implemented API and scheduler. `«include»` labels indicate required subflows; `«extend»` labels indicate conditional behavior.

```mermaid
flowchart LR
    requester[Requester: ROLE_USUARIO]
    tech[Technician: ROLE_TECNICO]
    admin[Administrator: ROLE_ADMIN]
    scheduler[Scheduler]
    dispatch[External dispatch service]
    subgraph system[HelpFlow system]
      register([Register and authenticate])
      create([Create ticket])
      inspect([View permitted tickets, history and assignments])
      manage([Change ticket status])
      assign([Assign or reassign ticket])
      adminUsers([Manage users and technicians])
      sla([Read or configure SLA policies])
      review([Review and escalate SLA tickets])
      suggest([Suggest technician])
    end
    requester --> register
    requester --> create
    requester --> inspect
    tech --> inspect
    tech --> manage
    admin --> inspect
    admin --> manage
    admin --> assign
    admin --> adminUsers
    admin --> sla
    admin --> review
    create -. "«include»" .-> suggest
    create -. "«include»" .-> assign
    review -. "«include»" .-> sla
    scheduler --> review
    suggest --> dispatch
    create -. "«extend» dispatch unavailable: rules fallback" .-> assign
```
