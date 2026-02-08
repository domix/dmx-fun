---
title: "Error Handling with Try Type"
description: "Master error handling in functional style using the Try type for robust exception management"
pubDate: 2024-01-22
author: "dmx-fun Team"
category: "Tutorial"
tags: ["Try", "Error Handling", "Exceptions"]
---

Exception handling is a crucial part of writing robust applications. The `Try` type provides a functional approach to exception handling that's more composable and easier to reason about than traditional try-catch blocks.

## The Problem with Traditional Exception Handling

Traditional exception handling can be verbose and difficult to compose:

```java
public Integer parseAndDouble(String input) {
    try {
        int value = Integer.parseInt(input);
        return value * 2;
    } catch (NumberFormatException e) {
        // What to return here?
        return null; // Or throw?
    }
}
```

## Enter Try

The `Try` type represents a computation that may fail:

```java
Try<Integer> result = Try.of(() -> Integer.parseInt(input));
```

A Try can be either:
- **Success**: Contains the successful result
- **Failure**: Contains the exception that occurred

## Basic Operations

### Creating a Try

```java
// From a risky operation
Try<Integer> result = Try.of(() -> Integer.parseInt("123"));

// Explicitly creating Success
Try<Integer> success = Try.success(42);

// Explicitly creating Failure
Try<Integer> failure = Try.failure(new Exception("Error"));
```

### Checking Results

```java
Try<Integer> result = parseNumber(input);

if (result.isSuccess()) {
    System.out.println("Success: " + result.get());
} else {
    System.out.println("Failed: " + result.getCause());
}
```

## Transforming Try

### map

Transform successful values:

```java
Try<Integer> parsed = Try.of(() -> Integer.parseInt("42"));
Try<Integer> doubled = parsed.map(x -> x * 2); // Success(84)
```

### flatMap

Chain operations that can fail:

```java
Try<Integer> result = Try.of(() -> readFile("data.txt"))
    .flatMap(content -> Try.of(() -> Integer.parseInt(content)))
    .flatMap(number -> Try.of(() -> divide(100, number)));
```

### recover

Handle specific exceptions:

```java
Try<Integer> result = Try.of(() -> Integer.parseInt(input))
    .recover(NumberFormatException.class, e -> 0)
    .recover(Exception.class, e -> -1);
```

## Practical Examples

### API Calls

```java
public Try<UserData> fetchUser(String userId) {
    return Try.of(() -> {
        HttpResponse<String> response = httpClient.send(request);
        if (response.statusCode() != 200) {
            throw new ApiException("HTTP " + response.statusCode());
        }
        return parseUserData(response.body());
    });
}

// Usage
Try<UserData> user = fetchUser("123")
    .recover(ApiException.class, e -> UserData.guest())
    .recover(IOException.class, e -> UserData.offline());
```

### Database Operations

```java
public Try<List<Order>> getUserOrders(String userId) {
    return Try.of(() -> database.connect())
        .flatMap(conn -> Try.of(() -> conn.query("SELECT * FROM orders")))
        .flatMap(resultSet -> Try.of(() -> mapToOrders(resultSet)))
        .onFailure(e -> logger.error("Failed to fetch orders", e));
}
```

### File Processing

```java
public Try<ProcessedData> processFile(String filename) {
    return Try.of(() -> Files.readString(Path.of(filename)))
        .flatMap(content -> Try.of(() -> parseJson(content)))
        .flatMap(json -> Try.of(() -> validate(json)))
        .flatMap(data -> Try.of(() -> transform(data)))
        .recover(IOException.class, e -> ProcessedData.empty())
        .recover(JsonException.class, e -> ProcessedData.invalid());
}
```

## Combining Multiple Tries

When you have multiple operations that can fail:

```java
Try<Order> result = validateUser(userId)
    .flatMap(user -> validateProduct(productId)
        .flatMap(product -> validateInventory(product)
            .map(inventory -> createOrder(user, product, inventory))));
```

## Converting Between Types

### Try to Option

```java
Try<Integer> result = Try.of(() -> Integer.parseInt("42"));
Option<Integer> optional = result.toOption(); // Some(42)

Try<Integer> failed = Try.of(() -> Integer.parseInt("abc"));
Option<Integer> none = failed.toOption(); // None
```

### Try to Either

```java
Try<Integer> result = Try.of(() -> Integer.parseInt(input));
Either<Throwable, Integer> either = result.toEither();
```

## Best Practices

1. **Use Try for risky operations**: Wrap any code that might throw exceptions
2. **Chain operations**: Use `flatMap` for sequential operations
3. **Recover gracefully**: Provide sensible defaults for common failures
4. **Log failures**: Use `onFailure()` to log errors without breaking the chain
5. **Don't catch what you can't handle**: Let unexpected exceptions propagate

## Advanced Patterns

### Retry Logic

```java
public Try<Response> fetchWithRetry(String url, int maxRetries) {
    Try<Response> result = Try.of(() -> httpClient.get(url));

    for (int i = 0; i < maxRetries && result.isFailure(); i++) {
        result = Try.of(() -> httpClient.get(url));
    }

    return result;
}
```

### Circuit Breaker

```java
public class CircuitBreaker {
    private int failureCount = 0;
    private static final int THRESHOLD = 5;

    public <T> Try<T> execute(Supplier<T> operation) {
        if (failureCount >= THRESHOLD) {
            return Try.failure(new CircuitOpenException());
        }

        Try<T> result = Try.of(() -> operation.get());

        if (result.isFailure()) {
            failureCount++;
        } else {
            failureCount = 0;
        }

        return result;
    }
}
```

## Conclusion

The `Try` type brings functional error handling to Java, making your code more robust and easier to maintain. By treating failures as values, you can compose error-handling logic just like you compose successful operations.

Start using Try in your next project and experience the benefits of functional error handling!
