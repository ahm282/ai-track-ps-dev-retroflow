# RetroFlow

Backend API for StackCraft's internal retrospective tool. Replaces the mix of sticky notes,
Miro boards, and shared Google Docs the 12 dev teams currently use to run retros.

## Status

The REST API under `/api` covers teams and members, retrospectives, feedback items and action
items. A small static web UI (plain HTML and JavaScript, no build step) is served by the same
application at `/`. The UI is a demo extra on top of the API.

The agreed assumptions, field limits, error messages and response fields are in
[`decisions.md`](decisions.md).

## API

All paths are under `/api`. Errors are returned as `{"message": "..."}`. Members are identified
by their UUID.

| Method and path | What it does |
| --- | --- |
| `POST /teams` | Create a team with its members (201) |
| `GET /teams` | List teams |
| `GET /teams/{id}` | Get a team with its active members |
| `POST /teams/{teamId}/members` | Add a member (201) |
| `PUT /teams/{teamId}/members/{memberId}` | Rename a member |
| `DELETE /teams/{teamId}/members/{memberId}` | Remove a member (204); the member is kept as inactive |
| `POST /teams/{teamId}/retrospectives` | Create a retrospective; body needs `title` and `memberId` (201). The date is set by the server |
| `GET /teams/{teamId}/retrospectives` | List a team's retrospectives |
| `GET /retrospectives/{id}` | Get one retrospective |
| `PUT /retrospectives/{id}/close` | Close a retrospective; body needs `memberId` |
| `POST /retrospectives/{id}/feedback` | Add feedback; body needs `content`, `type`, `submitterId`, plus `priority` for action items (201) |
| `GET /retrospectives/{id}/feedback` | List a retrospective's feedback |
| `PUT /feedback/{id}` | Replace `content` and, for action items, `priority` and `completed` |
| `DELETE /feedback/{id}` | Delete feedback (204) |
| `PUT /action-items/{id}/complete` | Mark an action item completed |
| `GET /action-items?priority=&completed=` | List action items, optionally filtered (case-insensitive) |

Feedback `type` is `WENT_WELL`, `NEEDS_IMPROVEMENT` or `ACTION_ITEM`; `priority` is `HIGH`,
`MEDIUM` or `LOW`. A team can have one open retrospective at a time, and a closed retrospective
accepts no feedback changes. Swagger UI lists the same endpoints with their schemas.

## Web UI

Open http://localhost:8080/ once the app runs.

- `/`: list teams and create one.
- `/team.html?id=`: manage members, choose who you are acting as, list and create retrospectives.
- `/retro.html?id=`: three columns (went well, needs improvement, action items); add, edit, delete
  and complete items; close the retrospective. Write controls are hidden once it is closed.
- `/action-items.html`: action items across all teams, filtered by priority and completed.

The "acting as" choice is stored per team in the browser's local storage.

## Tech stack

- Java 21, Spring Boot 3.3.4, Maven
- Spring Web, Spring Data JPA, Bean Validation
- H2 in-memory database (data is lost on every restart)
- Spring Boot Actuator and springdoc-openapi (Swagger UI)
- JaCoCo coverage gate, GitHub Actions CI, Docker

## Getting started

From a fresh clone to a running API in a few minutes. You need **JDK 21** and **Maven 3.9+**
(`java -version` and `mvn -v` to check). No database or other setup is needed.

```
git clone <repo-url>
cd ai-track-ps-dev-retroflow
mvn spring-boot:run
```

To use another port, add `-Dspring-boot.run.arguments=--server.port=8081`.

When the log shows `Started RetroflowApplication`, verify it works:

```
curl http://localhost:8080/actuator/health
```

Expected: `{"status":"UP"}`. Then open http://localhost:8080/ for the web UI or
http://localhost:8080/swagger-ui.html for Swagger UI.

Local runs use the `dev` profile (H2 console enabled). No Java or Maven installed? Use the
[dev container](#vs-code-and-dev-container).

## Useful URLs

| URL | What it is |
| --- | --- |
| http://localhost:8080/ | Web UI |
| http://localhost:8080/swagger-ui.html | Swagger UI, lets you call endpoints from the browser |
| http://localhost:8080/v3/api-docs | OpenAPI spec (JSON), importable into Postman or Insomnia |
| http://localhost:8080/actuator/health | Health check |
| http://localhost:8080/h2-console | H2 console, `dev` profile only. JDBC URL `jdbc:h2:mem:retroflow`, user `sa`, empty password |

The H2 console is only enabled by the `dev` profile (`application-dev.properties`), which is the
default for local runs. Any other active profile leaves it off.

## Testing the API manually

- **Web UI** at http://localhost:8080/ runs the whole retro flow in the browser.
- **Swagger UI** is the fastest way to explore and call the endpoints, with no extra tooling.
- **curl** works from the terminal, for example `curl -i http://localhost:8080/api/teams`.
- **Postman or Insomnia:** import http://localhost:8080/v3/api-docs to get every endpoint.
- **H2 console:** inspect the tables to confirm what was persisted.

## Running tests

```
mvn test      # unit and integration tests
mvn verify    # tests plus the JaCoCo coverage check
```

`mvn verify` fails if line coverage drops below **80%**, the same check CI runs. The HTML
report is written to `target/site/jacoco/index.html`.

Run a single test class or method:

```
mvn test -Dtest=RetroflowBusinessRulesTest
mvn test -Dtest=RetroflowBusinessRulesTest#closingARetrospective_preventsAddingFeedback
```

## Running with Docker

Requires Docker only, no local Java or Maven.

```
docker build -t retroflow .
docker run --rm -p 8080:8080 --name retroflow retroflow
```

The API is then at http://localhost:8080. `docker ps` shows the container as healthy or
unhealthy, based on `/actuator/health`. Stop it with `docker stop retroflow`.

The image is built in two stages (Maven and JDK to build, JRE only to run), runs as a
non-root user, and starts with the `prod` profile, so the H2 console is off.

## Development setup

### IntelliJ IDEA

1. **File > Open** and select the `pom.xml` (open as project). Maven imports automatically.
2. Set the project SDK to JDK 21 under **File > Project Structure**.
3. Run `RetroflowApplication` from the gutter icon or the Run menu.
4. Enable annotation processing only if you add Lombok or MapStruct later. Nothing needs it today.

### VS Code and dev container

- Install the Extension Pack for Java.
- The repo ships a `.devcontainer`. Choose **Reopen in Container** and you get JDK 21 and
  Maven preinstalled, dependencies pre-compiled, and port 8080 forwarded. This is the quickest
  route if your machine has no JDK 21, and it also works in GitHub Codespaces.

### Faster feedback loop

- `spring-boot-devtools` is included: while the app runs, recompiling (Build Project in
  IntelliJ, or save in VS Code) restarts it automatically. It is not packaged in the jar.
- Run only the test you are working on with `-Dtest=`.
- Add `-q` to Maven for less noise, and `-o` to work offline once dependencies are cached.
- Use the H2 console to check data without writing throwaway code.

## Project structure

Layered structure under `com.stackcraft.retroflow`:

| Package | Contents |
| --- | --- |
| `entity` | JPA entities and enums (Team, Member, Retrospective, FeedbackItem, ActionItem, Priority, FeedbackType, RetrospectiveStatus) |
| `repository` | Spring Data repositories |
| `service` | Business rules (`TeamService`, `RetrospectiveService`, `FeedbackService`) |
| `controller` | REST controllers, mappers and the single `ApiExceptionHandler` |
| `dto` | Request and response records |
| `exception` | `RetroflowException` for business-rule violations |

Config lives in `src/main/resources/application.properties`. The web UI is in
`src/main/resources/static`.

## Continuous integration

GitHub Actions (`.github/workflows/ci.yml`) runs `mvn verify` on pull requests to `master`
and on pushes to `master`. It enforces the 80% coverage gate, uploads the JaCoCo report as an
artifact, and comments the coverage on the PR. Actions are pinned to commit SHAs and kept
current by Dependabot.

Run `mvn verify` locally before pushing so CI does not fail on coverage.

## Known limitations

- No persistence: H2 is in memory and the schema uses `ddl-auto=update`.
- No authentication or authorization.
- Only a `dev` profile exists. The Docker image sets `prod`, which has no config of its own yet.
- No Maven wrapper (`mvnw`) is committed, so Maven must be installed.

## Contact

Ask in #retroflow-support on Slack. Response times have been slow lately, and the previous
team lead has left the company.
