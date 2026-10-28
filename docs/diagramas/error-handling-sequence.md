# Error Handling Sequence

The global API advice converts domain, application, framework and unexpected failures to localized `application/problem+json` responses decorated with timestamp, request path and trace ID.

```mermaid
sequenceDiagram
  participant Caller
  participant Controller
  participant Core as Domain or application
  participant Handler as ApiExceptionHandler
  participant Locale as MessageSource
  Caller->>Controller: HTTP request
  Controller->>Core: invoke use case
  Core-->>Handler: throw typed exception
  Handler->>Locale: resolve problem title and detail (Accept-Language)
  Locale-->>Handler: localized text
  Handler->>Handler: add status, type, instance, timestamp, traceId
  Handler-->>Caller: application/problem+json
```
