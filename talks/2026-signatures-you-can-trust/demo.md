# Demo script (minutes 27–32)

Goal: a Spring Boot 4 controller returns dmx-fun types; the client gets the right status and JSON with no hand-written mapping.

## Before going on stage

```bash
# from the repository root; Docker must be running (PostgreSQL via Docker Compose on port 5432)
./gradlew :samples:spring-boot-sample:test     # proves the environment works
./gradlew :samples:spring-boot-sample:bootRun   # leave it running on :8080
```

If port 5432 is taken on the machine, start a throwaway PostgreSQL elsewhere and point the sample at it:

```bash
docker run -d --rm --name dmx-fun-demo-postgres -e POSTGRES_DB=dmxfun -e POSTGRES_USER=dmxfun \
  -e POSTGRES_PASSWORD=dmxfun -p 5434:5432 postgres:16-alpine
./gradlew :samples:spring-boot-sample:bootRun \
  --args='--spring.docker.compose.enabled=false --spring.datasource.url=jdbc:postgresql://localhost:5434/dmxfun'
```

## On stage

In a terminal with a large font, either type the four requests or run the scripted version,
which types them for you with a pause after each response (`DEMO_PAUSE`, `DEMO_TYPE_DELAY` tune the pacing):

```bash
bash scripts/demo.sh
```

| Request | Expected | Why |
|---|---|---|
| `POST /items` `{"name":"Widget",…}` | `200`, the item as JSON | `Result.ok(item)`, unwrapped by the MVC return value handler |
| `POST /items` `{"name":"",…}` | `500`, `name must not be blank` | `Result.err(message)`, no `@ExceptionHandler` |
| `GET /items/-1` | `404`, empty body | `Option.none()` |
| `GET /items/search?name=Widget` | `200`, a JSON array | `Result<NonEmptyList<Item>, String>`: the list is serialized by `DmxFunModule` |

## Fallback

`demo.cast` is a recording of `scripts/demo.sh` against the sample (asciicast v2). It plays on the
slide right after the demo table (`<Cast />`, space to play, arrows to seek) and in a terminal with
`pnpm demo:play`. Re-record after changing the sample or the script, with the sample running:

```bash
pnpm demo:record      # scripts/record-demo.sh → demo.cast
```
