# RetroFlow – instructions for AI assistants

- Stack: Java 21, Spring Boot 3.3.4 (Maven), Spring Data JPA, Bean Validation, H2 in-memory, springdoc 2.6.0, JUnit 5 + AssertJ. Package root `com.stackcraft.retroflow`.
- Jakarta (`jakarta.*`) only, never `javax.*`. No Lombok: write constructors, getters and setters explicitly.
- Layering: controller → service → repository. Business rules live in services, never in controllers or entities' callers.
- Entities are classes (no-args + all-args constructors, getters/setters). DTOs may be records. IDs are `UUID`.
- Inject via constructor into `private final` fields. No field `@Autowired` in production code.
- Naming: `XxxService`, `XxxRepository`, `XxxController`; enums in singular PascalCase (`Priority`); loggers named `LOGGER`.
- Errors: rule violations throw `RetroflowException(message)`, missing resources throw `ResourceNotFoundException`. `ApiExceptionHandler` maps them to `{"message": "..."}` with 400 / 404. Messages match `decisions.md`.
- `decisions.md` is the source of truth for business rules and validation limits. If code and it disagree, ask; don't guess.
- Rules to enforce: one OPEN retro per team; closed retro can't be reopened or closed again; feedback in a closed retro is read-only; completed action items can't be uncompleted.
- Tests: JUnit 5 + AssertJ, `// Arrange / Act / Assert` structure, names like `closingARetrospective_preventsAddingFeedback`. Every business rule gets a positive and a negative test. JaCoCo requires ≥80% line coverage.
- Before saying "done": run `mvn test` and report the actual result. If it fails or you couldn't run it, say so.
- Never: edit or weaken existing test assertions, `@Disabled`/skip tests, leave TODOs or placeholders, add dependencies without asking, or commit/push unless asked.
- Comments: minimal. No narrating comments or Javadoc unless something is genuinely non-obvious.
