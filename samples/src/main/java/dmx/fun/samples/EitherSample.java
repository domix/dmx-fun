package dmx.fun.samples;

import dmx.fun.Either;

/**
 * Demonstrates Either<L, R>: a value that is one of two types with no success/failure semantics.
 * Use Either when neither branch is inherently an error — both are valid outcomes.
 */
public class EitherSample {

    // A classifier that routes input to one of two processing paths
    static Either<Integer, String> classify(String input) {
        try {
            return Either.left(Integer.parseInt(input));
        } catch (NumberFormatException e) {
            return Either.right(input.toUpperCase());
        }
    }

    static void main(String[] args) {
        // Numeric input → Left path
        Either<Integer, String> number = classify("42");
        System.out.println("Is left: " + number.isLeft()); // true
        System.out.println("Value:   " + number.getLeft()); // 42

        // Text input → Right path
        Either<Integer, String> text = classify("hello");
        System.out.println("Is right: " + text.isRight()); // true
        System.out.println("Value:    " + text.getRight()); // HELLO

        // Fold: apply different functions to each branch
        String description = classify("100").fold(
            n -> "number with " + n.toString().length() + " digits",
            s -> "text: " + s
        );
        System.out.println(description); // number with 3 digits

        // Map over the right side only
        Either<Integer, String> mapped = classify("world").map(s -> s + "!");
        System.out.println("Mapped right: " + mapped.getRight()); // WORLD!

        // Pattern match
        switch (classify("7")) {
            case Either.Left<Integer, String>  left  -> System.out.println("Left: "  + left.value());
            case Either.Right<Integer, String> right -> System.out.println("Right: " + right.value());
        }
    }
}
