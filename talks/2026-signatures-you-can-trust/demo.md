# Demo script (minutes 27–32)

Goal: a Spring Boot 4 controller returns dmx-fun types; the client gets the right status and JSON with no hand-written mapping.

Before going on stage:

```bash
# from the repository root; Docker must be running (Testcontainers / docker-compose for PostgreSQL)
./gradlew :samples:spring-boot-sample:test     # proves the environment works
./gradlew :samples:spring-boot-sample:bootRun   # leave it running on :8080
```

On stage, in a terminal with a large font:

```bash
curl -s -X POST localhost:8080/items -H 'Content-Type: application/json' \
  -d '{"name":"Widget","description":"A fine widget"}' | jq .          # 200, unwrapped Item
curl -s -X POST localhost:8080/items -H 'Content-Type: application/json' \
  -d '{"name":"","description":"x"}' -w '\n%{http_code}\n'            # 500, "name must not be blank"
curl -s -o /dev/null -w '%{http_code}\n' localhost:8080/items/-1       # 404 from Option.none()
curl -s 'localhost:8080/items/search?name=Widget' | jq .               # NonEmptyList as a plain array (DmxFunModule)
```

Fallback: record the four commands with asciinema before the talk and keep the cast in this directory.
