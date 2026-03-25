# User Service

User Management Service for SQL Judge Engine.

## Functionality

- User registration (Teacher/Student)
- User login with JWT authentication
- User information query
- Role-based access control

## API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | /api/user/register | Register new user |
| POST | /api/user/login | User login |
| GET | /api/user/me | Get current user info |
| GET | /api/user/{userId} | Get user by ID |

## Technology Stack

- Java 17
- Spring Boot 3.2
- Spring Security
- JWT
- MySQL 8.0
- JPA/Hibernate

## Running

```bash
mvn spring-boot:run
```

## Testing

```bash
mvn test
```
