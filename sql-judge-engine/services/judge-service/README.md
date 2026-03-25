# Judge Service

Judge Service for SQL Judge Engine.

## Functionality

- Consume judging tasks from RabbitMQ
- Acquire containers from container-manager
- Execute SQL in isolated containers
- Compare results and score submissions
- Report results to result-service

## Technology Stack

- Java 17
- Spring Boot 3.2
- RabbitMQ
- HTTP Client

## Note

This is an **internal service** - it does not expose public APIs. All communication happens via:
- RabbitMQ: Consuming judge-tasks queue
- HTTP: Calling container-manager and result-service

## Running

```bash
mvn spring-boot:run
```
