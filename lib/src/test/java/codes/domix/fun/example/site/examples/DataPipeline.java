package codes.domix.fun.example.site.examples;

import codes.domix.fun.Try;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public class DataPipeline {
    public record ProcessedData(String input) {

    }

    public record RawData(String input) {
        public boolean isValid() {
            return input.length() > 5;
        }

        public TransformedData transform() {
            return new TransformedData(input.toUpperCase());
        }

    }

    public record TransformedData(String input) {
        public ProcessedData enrich() {
            return new ProcessedData(input);
        }
    }

    public static <T, R> Try<List<R>> process(
        List<T> items,
        Function<T, Try<R>> processor
    ) {
        return Try.of(() ->
            items.stream()
                .map(processor)
                .map(Try::get)
                .collect(Collectors.toList())
        );
    }

    public Try<ProcessedData> processUserData(String input) {
        return parseInput(input)
            .flatMap(this::validate)
            .flatMap(this::transform)
            .flatMap(this::enrich);
    }

    private Try<RawData> parseInput(String input) {
        return Try.of(() -> new RawData(input));
    }

    private Try<RawData> validate(RawData data) {
        return data.isValid()
            ? Try.success(data)
            : Try.failure(new RuntimeException("Invalid data"));
    }

    private Try<TransformedData> transform(RawData data) {
        return Try.of(data::transform);
    }

    private Try<ProcessedData> enrich(TransformedData data) {
        return Try.of(data::enrich);
    }

    void main() {
        // Usage
        DataPipeline pipeline = new DataPipeline();

        List<String> inputs = List.of("data1", "data2", "data3");

        Try<List<ProcessedData>> results = DataPipeline.process(
            inputs,
            pipeline::processUserData
        );

        final var result = results.fold(
            data -> "Processed " + data.size() + " items",
            error -> "Pipeline failed: " + error
        );
    }
}
