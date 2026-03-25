# Container Manager

Container Manager Service for SQL Judge Engine.

## Functionality

- Manage container pool (pre-started Docker containers)
- Provide container acquire/release API
- Container health check
- Container内 MySQL connection management

## Technology Stack

- Java 17
- Spring Boot 3.2
- Docker Java SDK

## Note

This is an **internal service** - it does not expose public APIs. All communication happens via HTTP.

## Running

```bash
mvn spring-boot:run
```
