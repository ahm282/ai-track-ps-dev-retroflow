# RetroFlow

Backend API for StackCraft's internal retrospective tool. Replaces the mix of sticky notes,
Miro boards, and shared Google Docs the 12 dev teams currently use to run retros.

## Status

Early stage. The domain model (Team, Member, Retrospective, FeedbackItem, ActionItem) and
three repositories exist. `RetroflowController` is an empty stub, so **no REST endpoints are
implemented yet**. The service layer and error handling are also still to be built.

**The build is currently broken:** `RetroflowBusinessRulesTest` uses `TeamService` and
`RetrospectiveService`, which do not exist yet, so the test sources do not compile. Until the
service layer is added, `mvn test`, `mvn verify`, a plain `mvn spring-boot:run` and
`docker build` all fail. See the workaround under [Getting started](#getting-started).

Planned operations: create and retrieve teams, create and close retrospectives, add feedback
items, mark action items complete, and filter action items by priority and completed status.
The agreed assumptions and field limits are in [`decisions.md`](decisions.md).

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
mvn -Dmaven.test.skip=true spring-boot:run
```

`-Dmaven.test.skip=true` skips compiling the tests, which is needed only while the build is
broken (see [Status](#status)). Once the service layer exists, plain `mvn spring-boot:run`
works.

When the log shows `Started RetroflowApplication`, verify it works:

```
curl http://localhost:8080/actuator/health
```

Expected: `{"status":"UP"}`. Then open http://localhost:8080/swagger-ui.html.

Local runs use the `dev` profile (H2 console enabled). No Java or Maven installed? Use the
[dev container](#vs-code-and-dev-container).

## Useful URLs

| URL | What it is |
| --- | --- |
| http://localhost:8080/swagger-ui.html | Swagger UI, lets you call endpoints from the browser |
| http://localhost:8080/v3/api-docs | OpenAPI spec (JSON), importable into Postman or Insomnia |
| http://localhost:8080/actuator/health | Health check |
| http://localhost:8080/h2-console | H2 console, `dev` profile only. JDBC URL `jdbc:h2:mem:retroflow`, user `sa`, empty password |

The H2 console is only enabled by the `dev` profile (`application-dev.properties`), which is the
default for local runs. Any other active profile leaves it off.

## Testing the API manually

Endpoints do not exist yet, so for now only the URLs above respond. Once they are added:

- **Swagger UI** is the fastest way to explore and call them, with no extra tooling.
- **curl** works from the terminal, for example `curl -i http://localhost:8080/<path>`.
- **Postman or Insomnia:** import http://localhost:8080/v3/api-docs to get every endpoint.
- **H2 console:** inspect the tables to confirm what was persisted.

## Running tests

```
mvn test      # unit and integration tests
mvn verify    # tests plus the JaCoCo coverage check
```

Both currently fail to compile until the service layer exists (see [Status](#status)).

`mvn verify` fails if line coverage drops below **80%**, the same check CI runs. The HTML
report is written to `target/site/jacoco/index.html`.

Run a single test class or method:

```
mvn test -Dtest=RetroflowBusinessRulesTest
mvn test -Dtest=RetroflowBusinessRulesTest#closingARetrospective_preventsAddingFeedback
```

## Running with Docker

Requires Docker only, no local Java or Maven. **Currently fails at the build step** because
`-DskipTests` still compiles the tests (see [Status](#status)); it works once they compile.

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
| `controller` | REST controllers (currently an empty stub) |
| `exception` | `RetroflowException` for business-rule violations |

Config lives in `src/main/resources/application.properties`.

## Continuous integration

GitHub Actions (`.github/workflows/ci.yml`) runs `mvn verify` on pull requests to `master`
and on pushes to `master` (it fails until the build is fixed). It enforces the 80% coverage gate, uploads the JaCoCo report as an
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
