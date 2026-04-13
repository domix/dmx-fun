package dmx.fun.samples;

import dmx.fun.NonEmptyList;
import dmx.fun.NonEmptySet;
import dmx.fun.Option;
import dmx.fun.Validated;
import java.util.List;
import java.util.stream.Stream;

/**
 * Demonstrates NonEmptyList<T>: a list guaranteed to have at least one element at compile time.
 * Use NonEmptyList when an API contract requires at least one element.
 */
public class NonEmptyListSample {

    static Validated<NonEmptyList<String>, Integer> parsePositive(String raw) {
        try {
            int value = Integer.parseInt(raw);
            return value > 0
                ? Validated.valid(value)
                : Validated.invalidNel("Must be positive, got: " + value);
        } catch (NumberFormatException e) {
            return Validated.invalidNel("Not a number: " + raw);
        }
    }

    public static void main(String[] args) {
        // Construction — head + tail list
        NonEmptyList<String> tags = NonEmptyList.of("java", List.of("fp", "dmx-fun"));
        System.out.println("Head: " + tags.head());      // java
        System.out.println("Tail: " + tags.tail());      // [fp, dmx-fun]
        System.out.println("Size: " + tags.size());      // 3

        // Singleton
        NonEmptyList<String> single = NonEmptyList.singleton("only");
        System.out.println("Single size: " + single.size()); // 1

        // Map over all elements
        NonEmptyList<String> upper = tags.map(String::toUpperCase);
        System.out.println("Upper: " + upper.toList()); // [JAVA, FP, DMX-FUN]

        // Convert to a plain List when needed
        List<String> list = tags.toList();
        System.out.println("As List: " + list);

        // fromList returns Option — empty list produces None
        Option<NonEmptyList<String>> fromEmpty = NonEmptyList.fromList(List.of());
        System.out.println("From empty: " + fromEmpty.isEmpty()); // true

        Option<NonEmptyList<String>> fromFull = NonEmptyList.fromList(List.of("a", "b"));
        System.out.println("From full: " + fromFull.isDefined()); // true

        // toNonEmptySet — deduplicates while preserving head and insertion order
        NonEmptyList<String> withDups = NonEmptyList.of("java", List.of("fp", "java", "fp", "dmx-fun"));
        NonEmptySet<String> dedupSet  = withDups.toNonEmptySet();
        System.out.println("Set head: " + dedupSet.head());  // java
        System.out.println("Set size: " + dedupSet.size());  // 3 (not 5)

        // Concat — instance method
        NonEmptyList<String> more = NonEmptyList.of("quarkus", List.of("spring"));
        NonEmptyList<String> all  = tags.concat(more);
        System.out.println("Concat size: " + all.size()); // 5

        // Common use: accumulate validation errors with Validated
        Validated<NonEmptyList<String>, Integer> v1 = parsePositive("-1");
        Validated<NonEmptyList<String>, Integer> v2 = parsePositive("abc");
        v1.combine(v2, NonEmptyList::concat, Integer::sum)
          .peekError(errors -> errors.toList().forEach(e -> System.out.println("Error: " + e)));
        // Error: Must be positive, got: -1
        // Error: Not a number: abc

        // ---- toNonEmptyList() collector ----

        System.out.println("\n=== toNonEmptyList collector ===");

        // Non-empty stream → Some(NonEmptyList)
        Option<NonEmptyList<String>> collected = Stream.of("java", "fp", "dmx-fun")
            .filter(t -> t.length() > 2)
            .collect(NonEmptyList.toNonEmptyList());
        collected.peek(nel -> System.out.println("Collected: " + nel.toList()));
        // Collected: [java, fp, dmx-fun]

        // Empty stream → None
        Option<NonEmptyList<String>> noResults = Stream.<String>empty()
            .collect(NonEmptyList.toNonEmptyList());
        System.out.println("No results: " + noResults.isEmpty()); // true

        // Useful when a filter may eliminate all elements
        Option<NonEmptyList<String>> longTags = Stream.of("java", "fp", "dmx-fun")
            .filter(t -> t.length() > 10)
            .collect(NonEmptyList.toNonEmptyList());
        System.out.println("Long tags: " + longTags.isEmpty()); // true
    }
}
