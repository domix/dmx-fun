---
number: 14
title: "Facade collectors (Results, Options, Tries) as a single entry point"
status: Accepted
date: 2026-05-05
---

## Context

Each ADT (`Result`, `Try`, `Option`) exposes static collector factories (`toList()`, `partitioningBy()`, `groupingBy()`). Users working with streams need a discoverable, consistent entry point.

## Decision

Provide companion facade classes — `Results`, `Tries`, `Options` — that re-export all collector factories and common static helpers as top-level static methods. The ADT classes retain the canonical implementations; facades are pure delegation.

## Consequences

- Single import (`import static dmx.fun.Results.*`) gives access to all stream collectors.
- Facade classes are final with a private constructor (no instantiation).
- No logic duplication — all methods delegate to the ADT static methods.
- Discoverability: IDE auto-complete on `Results.` surfaces all stream operations without knowing the ADT class names.
- Slight API surface increase; mitigated by `@NullMarked` and consistent Javadoc.

## Alternatives considered

- **No facades — access via ADT class directly:** users must remember which static method lives on which class; less discoverable.
- **Single `Dmx` utility class:** mixes collectors for all types in one namespace, reducing clarity.
