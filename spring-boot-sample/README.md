# spring-boot-sample

End-to-end Spring Boot demo showing how `fun-spring-boot`, `fun-jackson`, and `fun-assertj`
fit together in a realistic, production-style Spring Boot project.

This module is **not published** to Maven Central — it lives in the repository as
documentation-by-code.

## What it shows

| Scenario                                                 | Component                                         |
|----------------------------------------------------------|---------------------------------------------------|
| Create item — success path                               | `TxResult` (programmatic)                         |
| Create item — validation failure, transaction rolls back | `TxResult`                                        |
| Update item — success / not-found                        | `@TransactionalResult` (declarative)              |
| Fetch item — wraps `Optional` in `Option`                | `ItemService.findById`                            |
| REST response serializes `Result<Item,String>` to JSON   | `fun-jackson` + `DmxFunModule`                    |
| Integration tests with fluent assertions                 | `fun-assertj` + Testcontainers                    |
| Local dev starts PostgreSQL automatically                | Spring Boot Docker Compose + `docker-compose.yml` |
| CI runs full suite against real PostgreSQL               | Testcontainers                                    |

## Running locally

**Prerequisites:** Docker (for PostgreSQL via Docker Compose).

```bash
./gradlew :spring-boot-sample:bootRun
```

Spring Boot's Docker Compose support starts the `docker-compose.yml` automatically.
The schema is created on startup via `spring.sql.init` (idempotent `CREATE TABLE IF NOT EXISTS`).

Once running, try the API:

```bash
# Create an item
curl -s -X POST http://localhost:8080/items \
  -H 'Content-Type: application/json' \
  -d '{"name":"Widget","description":"A fine widget"}' | jq .
# → {"ok":{"id":1,"name":"Widget","description":"A fine widget"}}

# Validation failure — Result.err serialized as {"error":"..."}
curl -s -X POST http://localhost:8080/items \
  -H 'Content-Type: application/json' \
  -d '{"name":"","description":"desc"}' | jq .
# → {"err":"name must not be blank"}

# Fetch an item (Option → 200 or 404)
curl -s http://localhost:8080/items/1 | jq .
curl -s -o /dev/null -w '%{http_code}' http://localhost:8080/items/-1
# → 404
```

## Running the tests

Tests use Testcontainers (PostgreSQL 16) — no manual Docker Compose needed.

```bash
# Default version (from version catalog)
./gradlew :spring-boot-sample:test

# Test against a specific Spring Boot version
./gradlew :spring-boot-sample:test -PspringBootVersion=3.4.13
```

## Key design choices

### `TxResult` vs `@TransactionalResult`

`ItemService.create` uses the **programmatic** style (`TxResult.execute`): the transaction
boundary is explicit at the call site, which is easier to follow when the validation logic
and the persistence logic both live in the same method.

`ItemService.update` uses the **declarative** style (`@TransactionalResult`): the AOP aspect
handles the transaction transparently, keeping the method free of transaction boilerplate.

Both styles roll back automatically when the returned `Result` is `Err`.

### `Result<Item, String>` in the REST layer

The controller returns `Result<Item, String>` directly from `@PostMapping` / `@PutMapping`
methods. Spring's Jackson integration serializes it via `DmxFunModule`:

```json
// Success
{"ok": {"id": 1, "name": "Widget", "description": "..."}}

// Failure
{"err": "name must not be blank"}
```

HTTP status is always `200 OK`. For production APIs, map the result to `ResponseEntity`
to return appropriate status codes (e.g., `400` for validation errors, `404` for not-found).

### Schema management

The sample uses Spring Boot's built-in SQL initialization (`spring.sql.init`) for
simplicity. For production applications, use [Flyway](https://flywaydb.org) or
[Liquibase](https://www.liquibase.org).

## Module structure

```
spring-boot-sample/
├── build.gradle
├── docker-compose.yml              # PostgreSQL for local dev
├── README.md
└── src/
    ├── main/
    │   ├── java/dmx/fun/sample/
    │   │   ├── SampleApplication.java        # @SpringBootApplication + DmxFunModule bean
    │   │   └── item/
    │   │       ├── Item.java                 # domain record (@Table, @Id)
    │   │       ├── ItemRepository.java       # Spring Data JDBC CrudRepository
    │   │       ├── ItemService.java          # TxResult + @TransactionalResult
    │   │       └── ItemController.java       # REST: Result<Item,String> → JSON
    │   └── resources/
    │       ├── application.yml
    │       └── db/schema.sql
    └── test/
        └── java/dmx/fun/sample/item/
            ├── ItemServiceTest.java          # @SpringBootTest + Testcontainers
            └── ItemControllerTest.java       # @SpringBootTest + MockMvc + Testcontainers
```
