# Gateway

API Gateway for SQL Judge Engine using Nginx.

## Functionality

- Unified entry point for all requests
- JWT authentication
- Rate limiting
- Route to microservices

## Routing Rules

| Path Prefix | Target Service | Description |
|-------------|----------------|-------------|
| /api/user | user-service:8081 | User authentication |
| /api/problem | problem-service:8082 | Problem management |
| /api/submission | submission-service:8083 | Submission handling |
| /api/result | result-service:8086 | Result queries |

## Internal Services (403 Forbidden)

- /api/judge - judge-service (internal)
- /api/container - container-manager (internal)
- /api/internal - internal services

## Running

```bash
docker build -t sql-judge-gateway .
docker run -p 8080:8080 sql-judge-gateway
```
