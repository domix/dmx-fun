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

    static void main() {
        // Numeric input → Left path
        var number = classify("42");
        IO.println("Is left: " + number.isLeft()); // true
        IO.println("Value:   " + number.getLeft()); // 42

        // Text input → Right path
        var text = classify("hello");
        IO.println("Is right: " + text.isRight()); // true
        IO.println("Value:    " + text.getRight()); // HELLO

        // Fold: apply different functions to each branch
        var description = classify("100").fold(
            n -> "number with " + n.toString().length() + " digits",
            s -> "text: " + s
        );
        IO.println(description); // number with 3 digits

        // Map over the right side only
        var mapped = classify("world").map(s -> s + "!");
        IO.println("Mapped right: " + mapped.getRight()); // WORLD!

        // Pattern match
        switch (classify("7")) {
            case Either.Left<Integer, String>  left  -> IO.println("Left: "  + left.value());
            case Either.Right<Integer, String> right -> IO.println("Right: " + right.value());
        }
    }
}
