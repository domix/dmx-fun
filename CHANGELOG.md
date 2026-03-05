# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

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

[Unreleased]: https://github.com/domix/dmx-fun/compare/v0.0.9...HEAD
[0.0.9]: https://github.com/domix/dmx-fun/compare/v0.0.8...v0.0.9
[0.0.8]: https://github.com/domix/dmx-fun/compare/v0.0.7...v0.0.8
[0.0.7]: https://github.com/domix/dmx-fun/compare/v0.0.6...v0.0.7
