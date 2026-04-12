# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [0.0.13] - 2026-04-12

### Breaking Changes

- **Package renamed from `codes.domix.fun` to `dmx.fun`:** all import statements must be
  updated. Module descriptor renamed to `dmx.fun`; artifact group/coordinates unchanged.

### Added

- **`Either<L, R>` — disjoint union type:**
  - Sealed interface with `Left<L, R>` and `Right<L, R>` implementations.
  - `map(Function)` / `mapLeft(Function)` / `flatMap(Function)` — standard functor / monad
    operations on the right channel.
  - `fold(Function, Function)` — collapses either branch into a single value.
  - `peek(Consumer)` / `peekLeft(Consumer)` — side-effect hooks.
  - `swap()` — exchanges left and right channels.
  - `toOption()` / `toResult()` / `toValidated()` — interop with other dmx-fun types.
  - Full `@NullMarked` coverage.
- **`NonEmptyList<T>` — non-empty list guaranteed at compile time (#122):**
  - `of(T head, List<? extends T> tail)` and `singleton(T)` factory methods.
  - `fromList(List<T>)` — returns `Option<NonEmptyList<T>>`; empty list produces `None`.
  - `head()`, `tail()`, `size()`, `toList()` accessors.
  - `map(Function)` — transforms all elements, preserving non-emptiness.
  - `concat(NonEmptyList)` — concatenates two non-empty lists.
  - Interop with `Validated` for error accumulation via `Validated.invalidNel`.
- **`Try.recover` and `Try.recoverWith` typed overloads (#118):**
  - `recover(Class<E>, Function<E, Value>)` — recovers only from a specific exception type.
  - `recoverWith(Class<E>, Function<E, Try<Value>>)` — same but returns a new `Try`.
  - Both overloads ignore failures whose exception does not match the given type.
- **`Option` / `Result` — eager and lazy `orElse` overloads (#123):**
  - `Option.orElse(Option<Value>)` — returns `this` if `Some`, otherwise the given alternative.
  - `Option.orElse(Supplier<Option<Value>>)` — lazy variant; supplier not called when `Some`.
  - `Result.orElse` / `Result.orElse(Supplier)` equivalents on the error channel.
- **`Validated` collectors (#117):**
  - `Validated.collector()` — `Collector` that accumulates a `Stream<Validated<E, A>>` into a
    single `Validated<NonEmptyList<E>, List<A>>`.
  - `Validated.traverseCollector(Function)` — map-then-collect in one pass.
- **`fun-jackson` module — Jackson serialization support:**
  - `DmxFunModule` — Jackson `Module` that registers serializers and deserializers for all
    dmx-fun types; register once with `ObjectMapper.registerModule(new DmxFunModule())`.
  - `Option<T>`: `Some` serializes as `{"value": ...}`, `None` as `{}`.
  - `Result<V, E>`: `Ok` as `{"ok": ...}`, `Err` as `{"err": ...}`.
  - `Try<V>`: `Success` as `{"value": ...}`, `Failure` as `{"error": "..."}`.
  - `Either<L, R>`: `Left` as `{"left": ...}`, `Right` as `{"right": ...}`.
  - `Validated<E, A>`, `Tuple2/3/4`, `NonEmptyList<T>`: full round-trip support.
  - Tested against Jackson 2.17.x through 2.21.x.
- **`fun-assertj` module — fluent AssertJ custom assertions (#116):**
  - `DmxFunAssertions.assertThat(Option<T>)` — `isSome()`, `isNone()`, `hasSomeValue(T)`.
  - `DmxFunAssertions.assertThat(Result<V, E>)` — `isOk()`, `isError()`, `hasOkValue(V)`.
  - `DmxFunAssertions.assertThat(Try<V>)` — `isSuccess()`, `isFailure()`, `hasSuccessValue(V)`.
  - `DmxFunAssertions.assertThat(Validated<E, A>)` — `isValid()`, `isInvalid()`.
  - `DmxFunAssertions.assertThat(Tuple2/3/4)` — per-slot value assertions.
  - Tested against AssertJ 3.21.x through 3.27.x.
- **`samples/` subproject — runnable examples (#52):**
  - One executable `*Sample.java` class per type: `OptionSample`, `ResultSample`, `TrySample`,
    `EitherSample`, `ValidatedSample`, `LazySample`, `TupleSample`, `NonEmptyListSample`,
    `CheckedInterfacesSample`, `JacksonSample`.
  - `AssertJSampleTest` — JUnit 5 test class demonstrating all `fun-assertj` assertions.

### Changed

- **Build — convention plugins extracted (#156):**
  - `dmx-fun.java-base` — applies `java-library`, JaCoCo, JUnit 5, AssertJ test dependencies.
  - `dmx-fun.java-module` — extends base with `mavenPublishing`, signing, JSpecify dependency,
    `issueManagement`, and `ciManagement` metadata.
- **POM metadata improved for all modules (#209):**
  - `name` and `description` updated to reflect each module's identity.
  - `issueManagement` pointing to the GitHub issue tracker added to all published modules.
  - `ciManagement` pointing to GitHub Actions added to all published modules.
- **CI — multi-module Maven Central publication (#157, #172):**
  - Root-level `publishToMavenCentral` task now publishes `lib`, `jackson`, and `assertj`
    modules in a single pipeline run.
  - Separate `publish-snapshot.yml` workflow publishes `*-SNAPSHOT` builds on push to `main`.
  - `publish.yml` skips automatically when the version ends with `-SNAPSHOT` (#201).
  - `publish-snapshot.yml` uses `publishToMavenCentral` (not the generic `publish` task) (#203).
- **CI — GitHub Actions upgraded to v6 and path filters expanded (#196).**
- **CI — compatibility matrices added:** AssertJ 3.21–3.27 (#194); Jackson 2.17–2.21 (#174).
- **Java version centralized** in `libs.versions.toml`; Jackson updated to 2.21.2.

### Fixed

- `Try.toEither` now guards against a `null` value inside `Success`.
- `Either.flatMap` now validates that the mapper does not return `null`.

### Documentation

- **Full Developer Guide published** — 9 core type pages (Option, Result, Try, Either,
  Validated, Lazy, Tuples, NonEmptyList, CheckedInterfaces) plus Jackson and AssertJ
  integration pages, each with a link to its runnable sample.
- **Contributing & Maintainer guide** — 4 new pages: contributing, pipelines, release process,
  module conventions (#212).
- **README rewritten** (#206) — Maven Central badges, type overview table, quick example,
  documentation link.
- **Homepage redesigned** (#211) — type grid, modules section, install snippets.

## [0.0.12] - 2026-04-06

### Added

- **`CompletableFuture` adapters for `Try` and `Result` (#49):**
  - `Try.fromFuture(CompletableFuture<V>)` — wraps a future outcome as `Try<V>`;
    `CompletionException` is unwrapped to its cause where possible; `CancellationException`
    is captured as a failure as-is.
  - `Result.fromFuture(CompletableFuture<V>)` — wraps as `Result<V, Throwable>`.
  - `toFuture()` on both `Try` and `Result` — converts to an already-completed
    `CompletableFuture`; failures become exceptionally-completed futures.
  - Duplicated future-unwrapping logic consolidated via shared helper.
- **`Lazy<T>` — lazily evaluated, memoized value (#51):**
  - `Lazy.of(Supplier<T>)` — defers evaluation until first access.
  - `get()` — evaluates the supplier exactly once and caches the result; if the supplier
    throws, the exception is captured and rethrown on every subsequent call.
  - `map(Function<T, R>)` — transforms the deferred value without forcing evaluation.
  - `fromFuture(CompletableFuture<T>)` — wraps a future as a `Lazy<Try<T>>`.
  - Fully `@NullMarked`; memoization is exception-safe using an internal `Try` state.
- **`zip3` / `zipWith3` / `map3` on `Option`, `Result`, and `Try` (#69):**
  - `zip3(a, b, c)` — combines three containers into a `Tuple3` if all are present/successful.
  - `zipWith3(a, b, c, TriFunction)` — combines three containers using a custom combiner.
  - `Option.map3` / `Result.map3` / `Try.map3` static variants.
  - `TriFunction<A, B, C, R>` top-level `@FunctionalInterface` added as a prerequisite.
- **`zip4` / `zipWith4` / `map4` on `Option`, `Result`, and `Try` (#70):**
  - `zip4(a, b, c, d)` — combines four containers into a `Tuple4`.
  - `zipWith4(a, b, c, d, QuadFunction)` — combines four containers using a custom combiner.
  - `QuadFunction<A, B, C, D, R>` top-level `@FunctionalInterface` added as a prerequisite.
- **`Try.flatMapError` (#114):**
  - `flatMapError(Function<? super Throwable, ? extends Try<? extends Value>>)` — dual of
    `flatMap`, operating on the failure channel; allows recovery from a `Failure` by running
    another fallible computation.
  - If the mapper itself throws or returns `null`, the exception is captured as a new
    `Failure` (mirrors `recoverWith` behaviour).

### Changed

- **`Bicontainer<Value, Error>` shared interface (#72):**
  - Common combinators (`fold`, `getOrElse`, `getOrElseGet`, `getOrThrow`, `peek`,
    `peekError`, `toOption`, `toResult`) extracted from `Result` and `Validated` into
    `Bicontainer`, eliminating duplicated implementations.
  - `Containers.requireNonNullResult` utility wired internally to guard callback return
    values consistently across both types.
- **`sequence` and `traverse` migrated to Stream Gatherers (#81):**
  - `Gatherer.ofSequential()` replaces `Collector.of()` and manual iterator loops in
    `Option`, `Result`, and `Try`.
  - Short-circuit semantics on first `None` / `Err` / `Failure` are preserved via the
    integrator returning `false`.
  - `Iterable` overloads delegate to their `Stream` counterparts via
    `StreamSupport.stream(iterable.spliterator(), false)`.
  - `Try.sequence` retains `Collections.unmodifiableList` (not `List.copyOf`) to preserve
    `null` elements produced by `Try.run()`.

### Refactored

- **`Validated.product()` — record-pattern switch (#82):**
  - Double-nested `switch` replaced with a local `record Pair<X, Y>` and a single
    exhaustive pattern-matching `switch`; improves readability and eliminates one level of
    indentation.
- **`Validated.traverse(Iterable)` — stream pipeline (#83):**
  - Anonymous `Iterator` inner class replaced with a
    `StreamSupport.stream(...).map(...).iterator()` pipeline; removes the manual
    `Iterator` import.

### Fixed

- Orphaned Javadoc block for the removed `isError()` method deleted from `Result`.

## [0.0.11] - 2026-03-20

### Added

- **`Validated<E, A>` — applicative error-accumulating validation type (#47):**
  - Sealed interface with two implementations: `Valid<E, A>` and `Invalid<E, A>`.
  - `Validated.valid(A)` / `Validated.invalid(E)` factory methods.
  - `map(Function)`, `mapError(Function)`, `flatMap(Function)` — standard functor / monad
    operations on the success channel.
  - `combine(Validated, BiFunction, BinaryOperator)` — merges two `Validated` values
    applicatively, accumulating errors from both sides when both are `Invalid`.
  - `fold(Function, Function)` — collapses either branch to a single value.
  - `getOrElse(A)` / `getOrElseGet(Supplier)` / `getOrThrow(Function)` — terminal extractors.
  - `peek(Consumer)` / `peekError(Consumer)` — side-effect hooks without altering the value.
  - `toOption()` / `toResult(Supplier)` — interop with `Option` and `Result`.
  - `sequence(Iterable)` / `traverse(Iterable, Function)` — collect a set of `Validated`
    values into a single `Validated<E, List<A>>`, accumulating all errors.
  - `fromOption(Option, Supplier)` — lifts an `Option` into `Validated`.
  - Full `@NullMarked` coverage; null guards on all callbacks and their return values.
- **Checked functional interfaces promoted to top-level types (#48):**
  - `CheckedFunction<T, R>` — `Function`-like interface whose `apply` method declares
    `throws Exception`; enables wrapping legacy checked-exception APIs.
  - `CheckedSupplier<T>` — `Supplier`-like with `throws Exception`.
  - `CheckedConsumer<T>` — `Consumer`-like with `throws Exception`.
  - `CheckedRunnable` — `Runnable`-like with `throws Exception`.
  - All four interfaces are `@NullMarked` and `@FunctionalInterface`.
- **`Tuple3<A, B, C>` and `Tuple4<A, B, C, D>` (#50):**
  - Immutable `@NullMarked` records with null-guarded compact constructors.
  - `of(...)` static factory methods.
  - `mapFirst` / `mapSecond` / `mapThird` (/ `mapFourth` for `Tuple4`) — apply a function
    to a single slot and return a new tuple with the remaining slots unchanged.
  - `map(TriFunction)` / `map(QuadFunction)` — collapse all elements into a single value.
- **`TriFunction<A, B, C, R>` and `QuadFunction<A, B, C, D, R>` (#50):**
  - `@NullMarked @FunctionalInterface` types required by `Tuple3.map` and `Tuple4.map`.
  - Also serve as building blocks for the planned `zip3` / `zip4` combinators (#69, #70).

### Changed

- **Java toolchain upgraded from 24 to 25 (#78):** `lib/build.gradle` toolchain and all
  three CI workflow files (`gradle.yml`, `publish.yml`, `pages.yml`) updated to
  `java-version: '25'`.
- **CI workflow triggers optimized (#56):**
  - `gradle.yml`: top-level `paths` filter removed from the `pull_request` trigger; a
    preliminary `changes` job using `dorny/paths-filter@v3` was added so that the `build`
    and `dependency-submission` jobs skip via `if:` conditions instead of the workflow not
    running at all — this keeps required status checks satisfied on PRs that touch
    unrelated files.
  - `publish.yml` and `pages.yml`: `paths` filters added / completed; `gradlew` and
    `gradlew.bat` included as trigger paths in both `gradle.yml` and `publish.yml`.

## [0.0.10] - 2026-03-08

### Added
- **`Try` — sequence & traverse combinators:**
  - `sequence(Iterable<Try<V>>)` and `sequence(Stream<Try<V>>)` — collect a homogeneous
    set of `Try` values into a single `Try<List<V>>`, failing fast on the first `Failure`.
  - `traverse(Iterable<A>, Function<A, Try<B>>)` and `traverse(Stream<A>, Function<A, Try<B>>)` —
    map-then-collect in one pass, failing fast on the first `Failure` produced by the mapper.
- **`Try` — null-safety hardening:** `@NullMarked` added to the class; null guards added
  throughout; `recoverWith` now validates that the callback does not return `null`.
- **`Result` — Stream collectors:**
  - `stream()` instance method — returns a single-element `Stream<Value>` for `Ok`, empty
    for `Err`; enables composing `Result` values with the Stream API.
  - `toList()` static `Collector` — accumulates `Stream<Result<V,E>>` into
    `Result<List<V>,E>`; returns the first `Err` found after consuming all elements (not
    fail-fast; use `sequence(Stream)` for short-circuit behaviour).
  - `partitioningBy()` static `Collector` → `Result.Partition<V,E>` — splits a
    `Stream<Result<V,E>>` into two unmodifiable lists: `oks` and `errors`, in encounter order.
  - `Result.Partition<V,E>` nested record — typed container produced by `partitioningBy()`;
    compact constructor defensively copies both lists via `List.copyOf` and null-checks inputs.
- **`Result.fromOptional(Optional<V>)`** — converts a present `Optional` to `Ok` and an
  empty `Optional` to `Err(NoSuchElementException)`.
- **`Try.fromOptional(Optional<V>, Supplier<Throwable>)`** — converts a present `Optional`
  to `Success` and an empty `Optional` to `Failure` using the provided exception supplier.

### Changed
- `Try.sequence` / `Try.traverse` use `Collections.unmodifiableList` instead of
  `List.copyOf` so that `Success(null)` values (produced by `Try.run()`) are preserved
  in the collected list.
- `Result.partitioningBy()` refactored to a single-pass accumulator: each element is
  routed directly to `oks` or `errors` during accumulation instead of buffering all
  `Result` elements and partitioning in a second pass at finisher time.

### Fixed
- `Result.toList()` and `Result.partitioningBy()` now explicitly reject `null` stream
  elements with `NullPointerException`, matching the contract of `sequence()` and
  `traverse()`.
- `Try.filter` Javadocs corrected: null-handling notes are now scoped to the `Success`
  path; existing `Failure` instances are documented as returned unchanged; the behaviour
  when `errorFn` returns `null` (wrapped as `Failure(NPE)`) is distinguished from passing
  a `null` predicate (throws NPE directly).
- `Result.toList()` Javadoc updated to explicitly state that all stream elements are
  consumed before the finisher scans for the first `Err`.

## [0.0.9] - 2026-03-01

### Added
- JPMS module descriptor (`module-info.java`): module `codes.domix.fun` requires
  `org.jspecify` and exports `codes.domix.fun`.
- `ModuleTest`: verifies module name, exported packages, declared dependencies,
  and absence of unintended exports via `ModuleFinder`.

## [0.0.8] - 2026-03-01

### Changed
- Version bump; no functional changes.

## [0.0.7] - 2026-03-01

### Added
- **`Result` — six new combinators:**
  - `recover(Function<Error, Value>)` — converts `Err` to `Ok` via a rescue function.
  - `recoverWith(Function<Error, Result<Value, Error>>)` — like `recover` but the
    rescue function returns a full `Result`.
  - `or(Supplier<Result<Value, Error>>)` — lazy fallback to an alternative `Result`
    on `Err`.
  - `flatMapError(Function<Error, Result<Value, NewError>>)` — dual of `flatMap`,
    operating on the error channel.
  - `swap()` — exchanges the `Ok` and `Err` channels.
  - `getOrElseGetWithError(Function<Error, Value>)` — derives a fallback value from
    the error itself (named to avoid overload ambiguity with `getOrElseGet(Supplier)`).
- **JSpecify null-safety annotations** on `Result`: the type is `@NullMarked`; only
  `getOrNull()` is annotated `@Nullable`.
- **GitHub Actions publish workflow** (`.github/workflows/publish.yml`): publishes
  to Maven Central on push to `main` when the version in `gradle.properties` does
  not yet have a corresponding git tag. Creates an annotated tag and a GitHub Release
  with auto-generated release notes.
- `Option.some(null)` now throws `NullPointerException` (Javadoc updated to match).

### Changed
- `Ok.value` is now **non-null**: the compact constructor enforces
  `Objects.requireNonNull` and `Result.ok(null)` throws `NullPointerException`.
- `Err.error` is now **non-null**: same enforcement; `Result.err(null)` throws
  `NullPointerException`.
- All callbacks that return a value (`recover`, `recoverWith`, `or`, `flatMapError`,
  `getOrElseGet`, `getOrElseGetWithError`) now validate that the return value is
  non-null and throw `NullPointerException` if the callback returns `null`.
- `toOption()` on `Ok` uses `Option.some()` instead of `Option.ofNullable()`,
  consistent with the non-null contract.
- `getOrElseGet(Function<Error, Value>)` renamed to `getOrElseGetWithError` to
  eliminate overload ambiguity with `getOrElseGet(Supplier<Value>)`.

### Fixed
- Javadoc build errors in `Ok`, `Err`, and `Try.Failure` compact constructors:
  `@throws` tags moved from record-level Javadoc (invalid) to compact-constructor
  Javadoc (valid).
- `Option.exceptionSupplier` returning `null` no longer causes a `NullPointerException`
  with a misleading stack trace; the case is now handled explicitly.

## [0.0.6] and earlier

Initial development: `Result`, `Try`, `Option`, and `Tuple2` types; interoperability
between all four types; monadic laws test suite (Spock); Java 24 toolchain; Maven
Central publication setup.

[Unreleased]: https://github.com/domix/dmx-fun/compare/v0.0.13...HEAD
[0.0.13]: https://github.com/domix/dmx-fun/compare/v0.0.12...v0.0.13
[0.0.12]: https://github.com/domix/dmx-fun/compare/v0.0.11...v0.0.12
[0.0.11]: https://github.com/domix/dmx-fun/compare/v0.0.10...v0.0.11
[0.0.10]: https://github.com/domix/dmx-fun/compare/v0.0.9...v0.0.10
[0.0.9]: https://github.com/domix/dmx-fun/compare/v0.0.8...v0.0.9
[0.0.8]: https://github.com/domix/dmx-fun/compare/v0.0.7...v0.0.8
[0.0.7]: https://github.com/domix/dmx-fun/compare/v0.0.6...v0.0.7
