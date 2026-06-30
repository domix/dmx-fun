---
number: 22
title: "Integration modules as optional peer dependencies"
status: Accepted
date: 2026-05-09
---

## Context

Many projects need dmx-fun types to interoperate with frameworks (Spring, Quarkus,
Jackson, Resilience4j, Micrometer, Jakarta). These integrations cannot live in the
core `lib` module because they would force transitive dependencies on all users,
even those who do not use the relevant framework. At the same time, users who adopt
several integrations must not have to track version numbers for each artifact
separately.

## Decision

Package each integration as a separate Gradle subproject organized by category,
with the peer dependency declared `compileOnly`. Users declare the peer dependency
themselves; the integration module adapts dmx-fun types to the framework's API.
A BOM (`fun-bom`) centralizes version alignment for users who adopt multiple modules.

**Directory layout** — subprojects are grouped by concern, not by framework name:

```
core/
  lib/               → codes.domix:fun              (always required)
  assertj/           → codes.domix:fun-assertj
serialization/
  jackson/           → codes.domix:fun-jackson
  jakarta-jaxb/      → codes.domix:fun-jakarta-jaxb
  jakarta-validation/→ codes.domix:fun-jakarta-validation
observability/
  micrometer/        → codes.domix:fun-micrometer
  observation/       → codes.domix:fun-observation
  tracing/           → codes.domix:fun-tracing
resilience/
  resilience4j/      → codes.domix:fun-resilience4j
protocols/
  http/              → codes.domix:fun-http
reactor/             → codes.domix:fun-reactor
frameworks/
  spring/            → codes.domix:fun-spring
  spring-boot/       → codes.domix:fun-spring-boot
  spring-webflux/    → codes.domix:fun-spring-webflux
  quarkus/runtime/   → codes.domix:fun-quarkus
bom/                 → codes.domix:fun-bom
```

**Module conventions** — every integration subproject follows a shared pattern
enforced by the `dmx-fun.java-module` convention plugin:

- The peer dependency is declared `compileOnly` so it is never pulled in
  transitively. Users bring their own version.
- `api project(':core:lib')` exposes the core library transitively, so users only
  need one dependency declaration for both the integration and the core types.
- Test dependencies use the concrete peer version resolved by a Gradle property
  (e.g., `-PjacksonVersion=X.Y.Z`), falling back to the version catalog entry.
  This enables CI compatibility matrix jobs without modifying build files.
- Every module ships a `module-info.java` (JPMS), consistent with
  [ADR-002 — JPMS from day one](https://domix.github.io/dmx-fun/adr/adr-002-jpms/).
  The `dmx-fun.java-module` plugin wires `--module-path`, `--patch-module`, and
  `--add-modules` into `compileTestJava` and `test` automatically via the `jpmsTest`
  DSL block.
- Maven coordinates follow `codes.domix:fun-{integration}` with JPMS module names
  `dmx.fun.{integration}` and base package `dmx.fun.{integration}`.
- The `dmx-fun.java-module` plugin also wires Maven publishing, artifact signing,
  and the JSpecify null-safety dependency — no per-module boilerplate.

**BOM** — `codes.domix:fun-bom` is a standard Maven Bill of Materials that
constrains all dmx-fun artifact versions in one import. Users who adopt multiple
modules import the BOM once and omit the version from every individual declaration:

```groovy
// Gradle
implementation(platform("codes.domix:fun-bom:${version}"))
```

```xml
<!-- Maven -->
<dependencyManagement>
  <dependencies>
    <dependency>
      <groupId>codes.domix</groupId>
      <artifactId>fun-bom</artifactId>
      <version>${version}</version>
      <type>pom</type>
      <scope>import</scope>
    </dependency>
  </dependencies>
</dependencyManagement>
```

**CI compatibility matrices** — each integration module with a peer dependency
provides a dedicated workflow (e.g., `jackson-compatibility.yaml`) that runs the
module's test suite against every supported version of the peer library using the
`-P{framework}Version=X.Y.Z` override property. The core `lib` and `assertj` modules
do not need a compatibility matrix because they have no peer dependency.

## Consequences

**Positive:**

- Core `lib` has zero framework dependencies — no transitive noise for users who
  do not need integrations.
- Each integration module is versioned and published independently to Maven Central
  under the same group ID (`codes.domix`).
- The BOM allows users to import all modules at a consistent version without
  repeating version numbers.
- CI matrix workflows per module verify compatibility across the supported version
  range of each peer dependency without modifying build files.
- The `dmx-fun.java-module` convention plugin eliminates per-module boilerplate:
  publishing, signing, JPMS wiring, and JSpecify are configured once.
- `api project(':core:lib')` means a single user dependency declaration is enough
  for both the integration and the core types.

**Negative / tradeoffs:**

- 15 integration subprojects (as of this writing) to maintain; each new module
  needs its own directory, `build.gradle`, `module-info.java`, guide page, and
  CI compatibility workflow.
- Users must discover and add the correct integration artifact for their framework;
  the core `lib` does not advertise optional integrations.
- `compileOnly` peer dependencies are absent from the published POM — tools that
  inspect the POM (e.g., Dependabot) will not surface peer dependency updates.

## Alternatives considered

- **Optional Maven dependencies in a single artifact:** pollutes the compile
  classpath even for unused integrations; the `optional` flag has poor and
  inconsistent support in Gradle and IDEs.
- **One multi-classifier artifact:** non-standard; breaks standard Maven/Gradle
  dependency resolution and confuses IDEs.
- **Fat jar with shading:** completely inappropriate for a library — forces version
  conflicts on consumers by relocating shared classes.
- **Flat module list (no category directories):** all subprojects at the root level;
  rejected because the 13+ modules would be difficult to navigate and the category
  grouping (`frameworks/`, `serialization/`, `observability/`) makes the project
  structure self-documenting.
