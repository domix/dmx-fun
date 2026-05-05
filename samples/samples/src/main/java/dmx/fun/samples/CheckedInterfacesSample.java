package dmx.fun.samples;

import dmx.fun.CheckedConsumer;
import dmx.fun.CheckedFunction;
import dmx.fun.CheckedRunnable;
import dmx.fun.CheckedSupplier;
import dmx.fun.QuadFunction;
import dmx.fun.TriFunction;
import dmx.fun.Try;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Demonstrates Checked interfaces: pass lambdas that throw checked exceptions
 * to higher-order functions without wrapping in try/catch at the call site.
 */
public class CheckedInterfacesSample {

    static void main() throws Exception {
        // CheckedFunction<T, R> — wrap in Try to handle the exception as a value
        CheckedFunction<String, Integer> parseChecked = Integer::parseInt;
        Function<String, Try<Integer>> safeParse =
            s -> Try.of(() -> parseChecked.apply(s));

        var results = Stream.of("1", "bad", "3")
            .map(safeParse)
            .toList();
        results.forEach(t -> IO.println(t.isSuccess() ? t.getOrElse(-1) : "failed"));
        // 1 / failed / 3

        // CheckedSupplier<T> — read a file that may not exist
        CheckedSupplier<String> readFile =
            () -> Files.readString(Path.of("/tmp/dmx-fun-sample.txt"));
        var content = Try.of(readFile);
        IO.println("File read succeeded: " + content.isSuccess());

        // CheckedConsumer<T> — process items that may throw
        CheckedConsumer<String> writeLog =
            line -> IO.println("LOG: " + line);
        Try.run(() -> writeLog.accept("application started"));

        // CheckedRunnable — side-effectful action that may throw
        CheckedRunnable init = () -> IO.println("Initialising...");
        Try.run(init);

        // TriFunction — combine three values
        TriFunction<String, Integer, Boolean, String> describe =
            (name, age, active) -> name + " age=" + age + " active=" + active;
        IO.println(describe.apply("Alice", 30, true));
        // Alice age=30 active=true

        // QuadFunction — combine four values
        QuadFunction<String, Integer, Double, String, String> product =
            (name, qty, price, sku) -> sku + ": " + name + " x" + qty + " @ $" + price;
        IO.println(product.apply("Widget", 5, 9.99, "WGT-001"));
        // WGT-001: Widget x5 @ $9.99
    }
}
