# RetroFlow

Backend API for StackCraft's internal retrospective tool. Replaces the mix of sticky notes,
Miro boards, and shared Google Docs the 12 dev teams currently use to run retros.

## Tech stack

- Java 21
- Spring Boot 3
- Maven
- H2 (in-memory)
- Spring Boot Actuator and springdoc-openapi

## Getting started

1. Install Java 21 and Maven.
2. Run the app:

   ```
   mvn spring-boot:run
   ```

3. The API will be available at http://localhost:8080.

No database setup is needed. The app uses an in-memory H2 database configured in
`src/main/resources/application.properties`, and Hibernate creates the schema at startup.
**All data is lost when the app stops.**

## Useful URLs

| URL | What it is |
| --- | --- |
| http://localhost:8080/h2-console | H2 web console. JDBC URL `jdbc:h2:mem:retroflow`, user `sa`, empty password |
| http://localhost:8080/swagger-ui.html | Swagger UI for the API |
| http://localhost:8080/v3/api-docs | OpenAPI spec (JSON) |
| http://localhost:8080/actuator/health | Health check |

## Running tests

```
mvn test
```

## Project structure

Standard Spring Boot layered structure under `com.stackcraft.retroflow`:

- `entity`: JPA entities (Team, Retrospective, FeedbackItem, ActionItem)
- `repository`: Spring Data repositories
- `controller`: REST controllers
- `exception`: `RetroflowException` for business-rule violations

## Current status

The domain model and repositories exist. The service layer, error handling and REST
endpoints are not built yet: `RetroflowController` is an empty stub.

## Contact

The previous team lead has left the company, so there is no forwarding contact. Ping #retroflow-support on
Slack if you get stuck, though response times have been slow lately.
