
# λ dmx-fun: Functional Programming Constructions in Java

<img width="1536" height="1024" alt="image" src="https://github.com/user-attachments/assets/cab675c2-c8ca-4017-903f-6790309750e8" />

This repository contains a collection of implementations and experiments exploring **functional programming constructions in Java**. The goal is to demonstrate how functional paradigms—such as immutability, pure functions, higher-order functions, currying, and more—can be expressed in Java, a traditionally object-oriented language.

## 🔍 Purpose

While Java is not a purely functional language, modern features like lambda expressions, streams, and the `Optional` and `CompletableFuture` APIs allow for elegant and expressive functional-style programming. This project aims to:

- Explore functional programming principles in Java
- Showcase reusable constructions inspired by functional languages
- Experiment with Java libraries that support functional programming
- Serve as a reference and learning resource for developers

## 📦 What's Included

Examples and implementations may include:

- Function composition and higher-order functions  
- Currying and partial application  
- Immutable data structures  
- Functional error handling (e.g., using `Try`, `Either`, or `Option`)  
- Lazy evaluation  
- Monads and functors (in a Java-friendly context)  
- Functional streams and pipelines  

## 🛠 Technologies

- Java 24+  
- Gradle
- Spock for testing  
- [Vavr](https://www.vavr.io/) or similar libraries for functional data types (optional)  

## 🤝 Contributions

Contributions, discussions, and suggestions are welcome! Feel free to open issues or submit pull requests.

## 📄 License

This project is licensed under the Apache License. See the [LICENSE](./LICENSE) file for more details.

## 🛠️ Usage


### Installation

This library is available on Maven Central. To use it, include the following dependency to your project's configuration file:

#### Maven

```xml
<dependency>
    <groupId>codes.domix</groupId>
    <artifactId>fun</artifactId>
    <version>0.0.4</version>
</dependency>
```

#### Gradle

```groovy
implementation("codes.domix:fun:0.0.4")
```
### Usage

Assuming you have imported the library, you can start using it in your code.

Let's start with a simple example. You have a method that validates a user's email, in a method like this:

```java
    protected Result<CreateUserCommand, String> isValidEmail(
        CreateUserCommand command
    ) {
        var email = command.email();
        if (email == null || email.isBlank()) {
            return Result.err("Provided email is either null or blank");
        }
        boolean emailMatches = EMAIL_PATTERN
            .matcher(email)
            .matches();

        if (!emailMatches) {
            return Result.err("Invalid email");
        }

        return Result.ok(command);
    }
```

In the previous example, we are using the `Result` type from the `fun` library. This type is a simple wrapper around an `Optional` that provides additional methods for handling errors.

In a nutshell, the `Result` type allows you to express a computation that may fail, and handle the error case in a type-safe way.

If everything goes well, the `Result` will contain the original value, in case of error, it will contain an error message.

Now you want to validate the user's password, with similar logic. You can create a new method like this:

```java
    protected Result<CreateUserCommand, String> isValidPassword(
        CreateUserCommand command
    ) {

        var password = command.password();
        if (password == null) {
            return Result.err("Provided password is null");
        }
        boolean validPassword = PASSWORD_PATTERN
            .matcher(password)
            .matches();

        if (!validPassword) {
            return Result.err("Invalid password.");
        }

        return Result.ok(command);
    }
```

As you can see, the `Result` type is used to express the result of the validation. 

Now we can combine both validations in a single method. You can do it like this:

```java
    public Result<User, String> createUser(CreateUserCommand command) {
        return this.isValidEmail(command)
            .flatMap(this::isValidPassword)
            .flatMap(this.userRepository::createUser);
    }
```

In this case, the validation is performed in two steps, and the result of the first step is used as the input for the second step.

In the end, the result of the whole validation process is used to create the user in the database.

The main benefit of this approach is that the validation logic is encapsulated in a single method, and it can be reused in other places.
