---
title: "Functional Design of Business Rules"
description: "Stop encoding business rules as scattered if/else chains and hidden exceptions. This post shows how to model rules as composable, testable, first-class values using Guard, Validated, and Result — with practical Java 25 examples."
pubDate: 2026-04-18
author: "domix"
authorImage: "https://gravatar.com/avatar/797a8fc41feef42d4bc41aff8cecb986d6f3fbbc157e49a65b2d5a5b6cd42640?s=200"
category: "Article"
tags: ["Business Rules", "Guard", "Validated", "Result", "Functional", "Patterns", "Java 25"]
image: "https://images.unsplash.com/photo-1453733190371-0a9bedd82893?q=80&w=1074&auto=format&fit=crop&ixlib=rb-4.1.0&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D"
imageCredit:
    author: "Roman Mager"
    authorUrl: "https://unsplash.com/@roman_lazygeek"
    source: "Unsplash"
    sourceUrl: "https://unsplash.com/es/fotos/ecuaciones-escritas-en-una-tabla-de-madera-marron-5mZ_M06Fc9g"
---

Business rules are the reason your software exists. They encode what your domain allows, what it forbids, and what it means to be in a valid state. Yet in most Java codebases, they are invisible — buried in `if` chains, duplicated across service methods, and tested only incidentally through integration paths.

This post makes them visible. We will look at what goes wrong with the typical imperative approach, then rebuild it using functional primitives: composable predicates, accumulating validation, and typed domain outcomes.

All examples use **Java 25** with the [dmx-fun](https://github.com/domix/dmx-fun) library.

---

## The Problem: Rules Without Shape

Here is a typical "validate and process" method from a real codebase:

```java
public void submitLoan(LoanApplication app) {
    if (app.amount() <= 0) {
        throw new ValidationException("amount must be positive");
    }
    if (app.termMonths() < 6 || app.termMonths() > 360) {
        throw new ValidationException("term must be between 6 and 360 months");
    }
    if (app.applicant().creditScore() < 600) {
        throw new ValidationException("credit score too low");
    }
    if (app.applicant().monthlyIncome() * 0.4 < monthlyPayment(app)) {
        throw new ValidationException("debt-to-income ratio too high");
    }
    // ... ten more checks ...
    processLoan(app);
}
```

This code has four concrete problems:

1. **Rules are anonymous.** There is no named concept of "creditworthy" or "affordable payment". The business rule lives only as an expression inside an `if`.

2. **Each failure short-circuits.** The caller gets one error per submission, even if five rules were violated. Fixing them one at a time is painful.

3. **Rules cannot be reused.** Need the same credit score check in a pre-qualification endpoint? Copy-paste. Need to tighten the threshold? Find every copy.

4. **Testing is indirect.** To test the credit score rule in isolation, you have to construct a full `LoanApplication` and route it through `submitLoan`.

The functional approach solves all four by giving rules a first-class representation.

---

## Step 1 — Name Your Rules

A business rule is a predicate: it takes a value and decides whether it is acceptable. In Java 25, the cleanest representation is a functional interface that also carries a reason for rejection:

```java
@FunctionalInterface
public interface BusinessRule<T> {
    Validated<NonEmptyList<String>, T> check(T value);
}
```

Using the `Guard<T>` type from dmx-fun, this pattern is already built in. A `Guard` wraps a predicate and a rejection message, returning a `Validated` that either confirms the value or accumulates the error:

```java
import dmx.fun.Guard;
import dmx.fun.NonEmptyList;
import dmx.fun.Validated;

// Each rule is a value — named, stored, composed
Guard<LoanApplication> positiveAmount =
    Guard.of(app -> app.amount() > 0, "amount must be positive");

Guard<LoanApplication> validTerm =
    Guard.of(
        app -> app.termMonths() >= 6 && app.termMonths() <= 360,
        "term must be between 6 and 360 months"
    );

Guard<LoanApplication> minimumCreditScore =
    Guard.of(
        app -> app.applicant().creditScore() >= 600,
        "credit score must be at least 600"
    );

Guard<LoanApplication> debtToIncomeRatio =
    Guard.of(
        app -> app.applicant().monthlyIncome() * 0.4 >= monthlyPayment(app),
        "monthly payment exceeds 40% of income"
    );
```

Each rule is now a **value**: it has a name, it is declared once, it can be passed around, composed, tested independently, and referenced from documentation.

---

## Step 2 — Compose Rules

Individual rules are rarely applied in isolation. You want to express things like:

- "A loan is eligible if it passes *all* of the following…"
- "A premium rate applies if the applicant meets *any* of these criteria…"

`Guard` composes with `and`, which evaluates both sides and **accumulates errors from both**:

```java
Guard<LoanApplication> eligibility =
    positiveAmount
        .and(validTerm)
        .and(minimumCreditScore)
        .and(debtToIncomeRatio);
```

Now `eligibility` is itself a rule — a single object that represents the entire eligibility policy. When you call it, you get *all* violated rules at once, not just the first one:

```java
Validated<NonEmptyList<String>, LoanApplication> result = eligibility.check(app);

switch (result) {
    case Validated.Valid<?, LoanApplication>(var validApp) ->
        processLoan(validApp);
    case Validated.Invalid<NonEmptyList<String>, ?>(var errors) ->
        errors.toList().forEach(System.err::println);
    // Prints all violations: "credit score must be at least 600"
    //                         "monthly payment exceeds 40% of income"
}
```

The caller gets the complete picture in a single pass. No need for a second submission to discover the next rule.

---

## Step 3 — Encode Outcomes as Types

Validation tells you whether an input is acceptable. But business processes have richer outcomes — a loan might be **approved**, **referred** for manual review, or **declined** for specific typed reasons:

```java
sealed interface LoanDecision {
    record Approved(BigDecimal rate, int termMonths) implements LoanDecision {}
    record Referred(String reason)                  implements LoanDecision {}
    record Declined(NonEmptyList<String> violations) implements LoanDecision {}
}
```

The underwriting function returns `Result<LoanDecision, UnderwritingError>` — the success track carries a `LoanDecision`, the failure track carries a technical failure:

```java
public Result<LoanDecision, UnderwritingError> underwrite(LoanApplication app) {
    return eligibility.check(app).fold(
        // Invalid — return a typed Declined decision
        violations -> Result.ok(new LoanDecision.Declined(violations)),
        // Valid — run the underwriting logic
        validApp   -> scoreApplication(validApp)
    );
}

private Result<LoanDecision, UnderwritingError> scoreApplication(LoanApplication app) {
    return creditBureauClient.fetchScore(app.applicant())   // Result<CreditScore, UnderwritingError>
        .map(score  -> computeRate(score, app))             // Result<BigDecimal, UnderwritingError>
        .map(rate   -> decideTier(rate, app));              // Result<LoanDecision, UnderwritingError>
}
```

The result at every layer is explicit. A caller cannot accidentally ignore the possibility of a `Declined` decision — the type forces a branch:

```java
Result<LoanDecision, UnderwritingError> decision = service.underwrite(application);

decision.match(
    d -> switch (d) {
        case LoanDecision.Approved(var rate, var term) ->
            offerLetter(rate, term);
        case LoanDecision.Referred(var reason) ->
            routeToManualReview(reason);
        case LoanDecision.Declined(var violations) ->
            sendRejectionNotice(violations);
    },
    err -> log.error("Underwriting system error: {}", err)
);
```

---

## Step 4 — Validate Across Multiple Objects

Many business rules span more than one field. A `Guard` can take any type as input — you are not restricted to flat records:

```java
// A rule that spans both the application and the applicant
Guard<LoanApplication> existingCustomerBonus =
    Guard.of(
        app -> !app.applicant().isExistingCustomer()
            || app.amount() <= app.applicant().preApprovedLimit(),
        "existing customer pre-approved limit exceeded"
    );

// Compose the cross-field rule into the overall policy
Guard<LoanApplication> fullPolicy =
    eligibility.and(existingCustomerBonus);
```

Because `Guard<T>` is just a function `T → Validated<NonEmptyList<String>, T>`, cross-field logic, database-backed checks, and context-dependent rules are all expressed the same way.

---

## Step 5 — Validate Collections with `traverseNel`

When a loan application contains a list of collateral items, each must be validated individually. `Validated.traverseNel` applies a validator to every element and **accumulates all errors** into a single `NonEmptyList`:

```java
Guard<Collateral> validCollateral = Guard.of(
    c -> c.appraisedValue().compareTo(BigDecimal.ZERO) > 0,
    "collateral appraisal must be positive"
).and(
    Guard.of(c -> c.lienPosition() == 1, "only first-lien collateral accepted")
);

Validated<NonEmptyList<String>, NonEmptyList<Collateral>> collateralCheck =
    Validated.traverseNel(
        application.collateral(),
        item -> validCollateral.check(item)
    );
```

If any item fails, every failing item's error is reported. The caller receives the full rejection list — one submission, complete feedback.

---

## Step 6 — Testing Is Now Trivial

Because each rule is a pure function, testing it requires no mocking, no Spring context, and no database.

The `fun-assertj` module provides `GuardAssert` and `ValidatedAssert` — fluent assertion types that understand the semantics of `Guard` and `Validated` directly, so tests read at the domain level rather than inspecting raw boolean fields:

```java
import static dmx.fun.assertj.DmxFunAssertions.assertThat;

class LoanEligibilityTest {

    @Test
    void minimumCreditScore_rejectsLowScore() {
        LoanApplication app = aValidApplication().withCreditScore(550).build();

        // GuardAssert — rejectsWithMessage checks both rejection and the exact message
        assertThat(minimumCreditScore)
            .rejectsWithMessage(app, "credit score must be at least 600");
    }

    @Test
    void eligibility_accumulatesAllViolations() {
        LoanApplication app = aValidApplication()
            .withCreditScore(550)
            .withMonthlyIncome(1000)
            .withAmount(50_000)
            .build();

        // ValidatedAssert — isInvalid() verifies the outcome; extract and inspect the errors
        Validated<NonEmptyList<String>, LoanApplication> result = eligibility.check(app);

        assertThat(result).isInvalid();
        // Standard AssertJ for the accumulated error list
        org.assertj.core.api.Assertions.assertThat(result.getError().size())
            .isGreaterThanOrEqualTo(2);
    }

    @Test
    void eligibility_acceptsCompliantApplication() {
        LoanApplication app = aValidApplication().build();

        // GuardAssert — accepts() confirms the value passes
        assertThat(eligibility).accepts(app);
    }

    @Test
    void minimumCreditScore_acceptsBorderlineScore() {
        LoanApplication app = aValidApplication().withCreditScore(600).build();

        assertThat(minimumCreditScore).accepts(app);
    }

    @Test
    void eligibility_rejectsMultipleViolations() {
        LoanApplication app = aValidApplication()
            .withCreditScore(400)
            .withAmount(-1)
            .build();

        // GuardAssert — rejectsWithMessages checks all accumulated messages at once
        assertThat(eligibility).rejectsWithMessages(app,
            "amount must be positive",
            "credit score must be at least 600"
        );
    }
}
```

Each test isolates exactly one rule. No rule can hide behind another. Threshold changes require a single fix, not a hunt through if-chains.

---

## Putting It All Together

Here is the final shape of the system — readable enough to review with a business analyst:

```java
public final class LoanPolicy {

    // ── Individual rules ──────────────────────────────────────────────────────

    static final Guard<LoanApplication> POSITIVE_AMOUNT =
        Guard.of(app -> app.amount() > 0, "amount must be positive");

    static final Guard<LoanApplication> VALID_TERM =
        Guard.of(
            app -> app.termMonths() >= 6 && app.termMonths() <= 360,
            "term must be between 6 and 360 months"
        );

    static final Guard<LoanApplication> MIN_CREDIT_SCORE =
        Guard.of(
            app -> app.applicant().creditScore() >= 600,
            "credit score must be at least 600"
        );

    static final Guard<LoanApplication> DEBT_TO_INCOME =
        Guard.of(
            app -> app.applicant().monthlyIncome() * 0.4 >= monthlyPayment(app),
            "monthly payment exceeds 40% of income"
        );

    // ── Composed policy ───────────────────────────────────────────────────────

    static final Guard<LoanApplication> ELIGIBILITY =
        POSITIVE_AMOUNT
            .and(VALID_TERM)
            .and(MIN_CREDIT_SCORE)
            .and(DEBT_TO_INCOME);

    // ── Entry point ───────────────────────────────────────────────────────────

    public static Validated<NonEmptyList<String>, LoanApplication> check(LoanApplication app) {
        return ELIGIBILITY.check(app);
    }
}
```

The policy is a first-class object. It lives in one place, has a name, is tested directly, and maps one-to-one with the requirements document. When the product team changes the minimum credit score from 600 to 650, the change is one line.

---

## Summary

| Problem                          | Solution                                     |
|----------------------------------|----------------------------------------------|
| Rules are anonymous              | `Guard` — rules are named values             |
| First failure short-circuits     | `Validated` — accumulates all violations     |
| Rules cannot be reused           | `Guard.and(...)` — compose into policies     |
| Outcomes are not typed           | `sealed` `Result` variants per decision      |
| Collection validation loses items | `Validated.traverseNel` — validates all     |
| Testing requires full setup      | Pure functions — test each rule in isolation |

Functional design of business rules is not about writing Haskell in Java. It is about making the rules **legible** — giving them names, making them composable, and ensuring the type system enforces that callers handle every outcome. The result is code that reads like a policy, fails predictably, and changes safely.

---

**[dmx-fun](https://github.com/domix/dmx-fun)** provides `Guard`, `Validated`, `NonEmptyList`, `Result`, and `Option` as a composable, null-marked, zero-dependency toolkit designed for exactly this kind of domain logic in Java 25+ projects.
