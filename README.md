# Express & Possess

Wish lists for a group. Members write down what they want; other members take care of it.
Built for one family, published as a portfolio project.

Specification: [docs/spec-v3.html](docs/spec-v3.html). Build order: [docs/plan.md](docs/plan.md).

## Run locally

Docker Desktop must be running.

```bash
docker compose up -d postgres mailpit
mvn -pl api spring-boot:run
```

The API listens on http://localhost:8080. Emails the application sends are caught by
Mailpit at http://localhost:8025.

## Test

Tests run against a real PostgreSQL started by Testcontainers, so Docker must be running.

```bash
mvn verify
```
