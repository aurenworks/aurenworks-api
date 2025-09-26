# AGENTS.md – Guidelines for AI Agents and Contributors

This document defines how AI assistants (like ChatGPT, Copilot, etc.) and human contributors
should interact with the **aurenworks-api** project.

---

## Mission

AurenWorks is a **citizen developer platform**.
`aurenworks-api` is the **control-plane backend** (Java 21 / Quarkus) providing REST & GraphQL endpoints
to manage workspaces, templates, outcomes and Git-backed resources.

---

## Core Responsibilities

- **Expose APIs** for studio and portal clients
- **Integrate with GitHub** for template and outcome repositories
- **Persist metadata** in PostgreSQL (templates refs, user/workspace info)
- **Manage secrets** via Kubernetes Secrets or HashiCorp Vault
- **Provide observability** (health, metrics, logs, tracing)

---

## Rules for AI Agents

### Security & Compliance
- **NEVER** commit raw secrets. Only store references (like `k8s:secret/<name>#<key>`).
- Do not print or log sensitive data (tokens, emails, etc.).
- Always sanitize user input to prevent injection attacks.
- Prefer **least privilege** for database and GitHub tokens.

### Development Guidelines
- Use Quarkus best practices (CDI, Panache, RESTEasy Reactive).
- Layered architecture: `api → service → repo → db`.
- PostgreSQL with Flyway migrations.
- Git-based YAML storage: outcomes/templates live in Git, database stores only references.

### Testing
- All changes must include tests (unit + integration).
- Use Quarkus JUnit 5 and Testcontainers for database tests.
- Run full test suite with: `./mvnw clean test`.
- **Format before testing**: Run `./mvnw spotless:apply` before running tests.

### Documentation
- Update `README.md`, `Architecture.md`, and relevant docs for each feature.
- For API changes, update OpenAPI spec.

### Code Style
- Follow `eclipse-formatter.xml`.
- Java files use **2 spaces** indentation.
- **Always format code**: Run `./mvnw spotless:apply` after editing Java files.
- **Check formatting**: Use `./mvnw spotless:check` to verify formatting before commits.

### Communication & Collaboration
- When proposing architectural changes, explain the trade-offs and security implications.
- Provide clear, concise PR descriptions with **Why** and **What**.

---

## Reference Architecture

- **API**: Quarkus REST/GraphQL
- **Persistence**: PostgreSQL + Flyway
- **Secrets**: Kubernetes Secrets or Vault
- **Git**: GitHub integration for templates and outcomes
- **Deployment**: Docker/Kubernetes

---

## Key Directories
- `src/main/java/.../api` — REST/GraphQL endpoints
- `src/main/java/.../service` — Business logic
- `src/main/java/.../repo` — Database repositories
- `src/main/java/.../model` — Entities and DTOs
- `src/test/java/...` — Tests
- `db/migration` — Flyway migrations

## Code Formatting with Spotless

### Quick Commands
```bash
# Format all code
./mvnw spotless:apply

# Check if code is properly formatted
./mvnw spotless:check
```

### For AI Agents
- **Always run `./mvnw spotless:apply`** after generating or editing Java files
- **Verify formatting** with `./mvnw spotless:check` before completing tasks
- **CI enforces formatting** - unformatted code will fail the build pipeline

---

## Final Notes
Security, testing, and Git-based storage are **non-negotiable**.
AI agents should always highlight potential security or compliance issues.
