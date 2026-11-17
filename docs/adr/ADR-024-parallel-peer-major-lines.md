---
number: 24
title: "Parallel integration modules for incompatible peer major lines"
status: Proposed
date: 2026-10-18
---

## Context

[ADR-022](https://domix.github.io/dmx-fun/adr/adr-022-integration-modules/) packages
each framework integration as one subproject with the peer dependency declared
`compileOnly`, and assumes a single supported line per peer. Some peers break
binary compatibility between major versions and keep both lines alive for years.
Jackson is the first case: Jackson 2 (`com.fasterxml.jackson.*`) and Jackson 3
(`tools.jackson.*`) ship in parallel, Spring Framework 7 and Spring Boot 4 default
to Jackson 3, while Spring Boot 3 applications remain on Jackson 2. A single
`fun-jackson` module cannot serve both: the packages, the `Module` SPI and the
serializer base classes differ, and Spring Boot 4's auto-configuration only
registers `tools.jackson.databind.JacksonModule` beans. Before this decision a Boot 4
user had to exclude `spring-boot-jackson`, pin Jackson 2 and wire a deprecated HTTP
message converter by hand, as `samples/spring-boot-sample` did.

## Decision

When a peer dependency forks into binary-incompatible major lines that are both
in active use, ship one integration module per line and let them coexist.

**Trigger.** The trigger is binary incompatibility, not the existence of two
majors. A peer that keeps its packages and the APIs this library uses across
majors stays in a single module, verified by its compatibility matrix. Spring
Framework 6/7 and Spring Boot 3/4 are in that category today: `fun-spring`,
`fun-spring-webflux` and `fun-spring-boot` are one jar each, tested from
Spring 6.0 to 7.0 and from Boot 3.3 to 4.1. This ADR applies only once one jar
can no longer compile against both lines, as happened with Jackson 2/3.

**Naming.** Every line carries the peer's major version in the artifact, the
JPMS module and the base package; no unsuffixed name is left pointing at one
of the lines. If the integration already exists under an unsuffixed name, it is
renamed when the second line is introduced:

| | Artifact | JPMS module | Base package |
|---|---|---|---|
| Each line | `codes.domix:fun-{integration}{major}` | `dmx.fun.{integration}{major}` | `dmx.fun.{integration}{major}` |

First instance: `fun-jackson` becomes `fun-jackson2` (Jackson 2) and
`fun-jackson3` (Jackson 3) is added, both in release 0.3.0. Distinct packages
guarantee that both artifacts can sit on the same classpath and module path
without split packages or service-loader clashes. Auto-configuration
properties follow the same rule (`dmx.fun.jackson2.enabled`,
`dmx.fun.jackson3.enabled`).

**Primary line.** The primary line is the one the target ecosystem uses by
default; for Jackson that is the line Spring Boot's current major selects. New
features are designed for the primary line first and backported to the other
line when its API allows. The guide for each integration states which line is
primary and shows a decision table (framework version → artifact).

**Maintenance and retirement of the older line.** The older line receives bug
fixes and peer version bumps for as long as the framework major that depends on
it is within its open-source support window (for Jackson 2, Spring Boot 3.x).
Once that window closes, the module is deprecated in the next minor release of
dmx-fun and removed in the following major. Deprecation and removal are each
announced in the changelog and on the site at least one release in advance.

**Spring Boot auto-configuration.** `fun-spring-boot` provides one
auto-configuration per line, each conditional on the classes of its own line.
Neither disables the other; a project that has both lines on the classpath gets
both modules registered in their respective mappers.

**Build and CI.** Each line is its own Gradle subproject under the same category
directory (`serialization/jackson2`, `serialization/jackson3`), follows the
`dmx-fun.java-module` conventions of ADR-022, is listed in `fun-bom`, and has
its own compatibility matrix workflow driven by a `-P{framework}{major}Version`
property.

## Consequences

**Positive:**

- Users on either framework major get a zero-configuration path; the sample
  no longer needs exclusions, manual mappers or deprecated converters.
- Coexistence on one classpath means mixed deployments (a Boot 3 service and a
  Boot 4 service sharing a domain jar) work without classifier tricks.
- The retirement rule is tied to an external, public lifecycle, so dropping a
  line is a scheduled event rather than a judgment call.
- The naming rule extends to any future fork (`fun-spring7`, `fun-jackson4`)
  without a new decision, and the eventual retirement of a line leaves no
  orphaned unsuffixed name behind.

**Negative / tradeoffs:**

- Serializer and test code is duplicated per line. Shared logic cannot be
  factored out without a dependency on both peers, which ADR-022 forbids.
- Each extra line adds one subproject, one workflow, one guide section and one
  BOM entry to maintain.
- Users must pick the artifact that matches their framework; the core `lib`
  cannot detect the wrong choice at compile time.
- Renaming an existing module is a breaking change for its users (coordinates,
  package, property). Acceptable before 1.0 and announced in the changelog;
  the old coordinates remain on Maven Central and may carry a relocation POM.
- `javac` emits `[module] module name component jackson2 should avoid terminal
  digits` for `module dmx.fun.jackson2`. The lint exists because a trailing
  digit can be mistaken for a version; here the digit *is* the peer's major
  and that is the point. The warning is accepted, as `org.apache.commons.lang3`
  and similar modules do, and is not suppressed build-wide so that other
  modules keep the check.

## Alternatives considered

- **One module supporting both lines through reflection or a runtime adapter:**
  rejected. It hides a hard requirement behind runtime failures, defeats JPMS
  `requires` declarations, and doubles the test matrix inside one module.
- **Multi-release jar or Maven classifiers per line:** rejected. Neither is
  keyed on a third-party library version; classifiers resolve poorly in Gradle
  and are invisible to the BOM.
- **Drop the older line immediately and support only the primary one:**
  rejected while the older framework major is still inside its support window;
  it would break existing users for no functional gain.
- **Keep a single module on the older line and tell newer users to configure
  the bridge themselves:** rejected; this is the status quo and the sample
  shows how much ceremony it costs.
- **Keep the unsuffixed name for the original line and suffix only new lines:**
  rejected. The obvious name would point at the legacy line exactly when it
  enters maintenance, and retiring that line later would leave a confusing gap.
- **Rename only the artifact and keep the old package and module name:**
  rejected; it breaks the artifact/module/package alignment of ADR-022.
- **Module and package named `dmx.fun.jackson.v2` to avoid the terminal-digit
  lint:** rejected. It breaks the alignment with the artifact name
  (`fun-jackson2`), adds a segment that no other module has, and trades a
  harmless warning for a naming exception that every future line would have
  to repeat.
