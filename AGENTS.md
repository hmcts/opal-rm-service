# Repository Guidelines

This file covers repo-specific guidance for `opal-rm-service`.

## IMPORTANT: Required Shared Opal Skills
- Do not rely on this repo's `AGENTS.md` alone for normal Opal work.
- This repo expects the shared `opal-dev-agent-skills` repository to be installed either into this project or made available to Codex globally before substantive coding or review work starts.
- Required Codex skills for this repo are the shared `opal-java` skill and the shared review guidance when reviewing code.
- Repo-local install paths are `.codex/skills/opal-java` and `.codex/skills/review`.
- For any request to write, change, review, or explain Java code, use the shared `opal-java` skill.
- If the required shared skills are missing or broken in both repo-local and global form, warn the user immediately and tell them to install the shared skills.

Use this format exactly:

```text
WARNING: Shared Opal agent skills are not installed correctly.
Clone the `opal-dev-agent-skills` repository and follow its README to install the shared skills before relying on Java code generation or review in this repo.
```

## Project Structure
- Application code: `src/main/java`
- Resources and Flyway migrations: `src/main/resources`
- Unit tests: `src/test/java`
- Smoke tests: `src/smokeTest/java`
- Integration tests: `src/integrationTest/java`
- Functional tests: `src/functionalTest/java`
- Ops assets: `charts/`, `config/`, `infrastructure/`

## Commands
- `./gradlew build` compiles, runs unit tests, and builds the artifact.
- `./gradlew smoke` runs smoke tests against `TEST_URL` when provided.
- `./gradlew integration` runs integration tests.
- `./gradlew functional` runs functional tests.
- `./gradlew jacocoTestReport` refreshes coverage output for Sonar.
- `./gradlew run` starts the application against provisioned infrastructure.
- `docker compose up --build` starts the application and local Postgres in containers.

## Local Conventions
- Target Java 21 and Lombok.
- Keep the standard layer flow: controller -> service -> repository -> domain/DTO.
- Put transaction boundaries on service methods and keep read flows `@Transactional(readOnly = true)` where appropriate.
- Default JPA associations to `FetchType.LAZY`.

## Commit and Config Notes
- Follow the existing commit style with a Jira key or concise imperative prefix.
- Do not commit secrets or environment-specific credentials.
