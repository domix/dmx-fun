---
number: 13
title: "Try.withTimeout uses virtual threads (Thread.ofVirtual())"
status: Accepted
date: 2026-05-05
---

## Context

`Try.withTimeout` needs to run user-supplied code with a wall-clock deadline. Options were:
- Platform threads via `ExecutorService`
- Virtual threads via `Thread.ofVirtual()`
- `CompletableFuture` with a shared pool

## Decision

Use `Thread.ofVirtual().start(task)` to spawn a lightweight carrier for the timed operation. The calling thread blocks on `FutureTask.get(timeout, unit)` and interrupts the virtual thread on timeout.

## Consequences

- No `ExecutorService` lifecycle to manage; threads are fire-and-forget.
- Near-zero overhead for blocking I/O inside the timeout block (virtual thread parks, not blocks a carrier).
- The `CancellationException` branch (external cancellation of the internal `FutureTask`) is unreachable from outside the method — accepted as an untestable defensive guard.
- Requires Java 21+ (virtual threads GA); already met by the Java 25 baseline (ADR-001).

## Alternatives considered

- **Platform threads via `ExecutorService`:** requires lifecycle management and heavier overhead for blocking I/O.
- **`CompletableFuture` with a shared pool:** uses the common ForkJoin pool, which is inappropriate for blocking operations.
