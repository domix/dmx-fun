package dmx.fun.samples;

import dmx.fun.NonEmptyList;
import dmx.fun.NonEmptyMap;
import dmx.fun.NonEmptySet;
import dmx.fun.Option;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Demonstrates NonEmptyMap<K, V>: a map guaranteed to have at least one entry at compile time.
 * Use NonEmptyMap when a registry, configuration, or lookup table must always have at least one entry.
 */
public class NonEmptyMapSample {

    // -------------------------------------------------------------------------
    // A role-to-permissions registry backed by NonEmptyMap.
    // The compiler prevents creating the registry with an empty map.
    // -------------------------------------------------------------------------

    static Option<NonEmptyMap<String, Set<String>>> rolesWithPermission(
            NonEmptyMap<String, Set<String>> registry, String permission) {
        return registry.filter((role, perms) -> perms.contains(permission));
    }

    static NonEmptyMap<String, Set<String>> addRole(
            NonEmptyMap<String, Set<String>> registry, String role, Set<String> perms) {
        return registry.merge(NonEmptyMap.singleton(role, perms), (existing, incoming) -> {
            Set<String> merged = new HashSet<>(existing);
            merged.addAll(incoming);
            return Set.copyOf(merged);
        });
    }

    static void main(String[] args) {
        // ---- Construction ----

        // of(key, value, rest) — head entry + additional entries
        NonEmptyMap<String, Integer> scores =
            NonEmptyMap.of("alice", 10, Map.of("bob", 20, "carol", 30));
        System.out.println("Head key:   " + scores.headKey());   // alice
        System.out.println("Head value: " + scores.headValue()); // 10
        System.out.println("Size:       " + scores.size());      // 3

        // Duplicate head key in rest — head value wins
        NonEmptyMap<String, Integer> dedup =
            NonEmptyMap.of("alice", 10, Map.of("alice", 99, "bob", 20));
        System.out.println("Dedup size: " + dedup.size());           // 2
        System.out.println("Alice:      " + dedup.headValue());      // 10 (not 99)

        // singleton
        NonEmptyMap<String, Integer> single = NonEmptyMap.singleton("alice", 10);
        System.out.println("Single size: " + single.size());         // 1

        // fromMap — safe bridge from plain Map
        Option<NonEmptyMap<String, Integer>> fromEmpty = NonEmptyMap.fromMap(Map.of());
        System.out.println("From empty: " + fromEmpty.isEmpty());    // true

        Option<NonEmptyMap<String, Integer>> fromFull = NonEmptyMap.fromMap(Map.of("x", 1));
        System.out.println("From full:  " + fromFull.isDefined());   // true

        // ---- Accessing elements ----

        Option<Integer> found   = scores.get("bob");     // Some(20)
        Option<Integer> missing = scores.get("unknown"); // None
        System.out.println("bob:     " + found.getOrElse(-1));   // 20
        System.out.println("unknown: " + missing.getOrElse(-1)); // -1

        System.out.println("containsKey alice: " + scores.containsKey("alice")); // true
        System.out.println("containsKey dave:  " + scores.containsKey("dave"));  // false

        // ---- Transformations ----

        // mapValues
        NonEmptyMap<String, String> labeled = scores.mapValues(v -> v + " pts");
        System.out.println("alice labeled: " + labeled.get("alice").getOrElse("?")); // 10 pts

        // mapKeys
        NonEmptyMap<String, Integer> upper = scores.mapKeys(String::toUpperCase);
        System.out.println("ALICE: " + upper.containsKey("ALICE")); // true
        System.out.println("BOB:   " + upper.containsKey("BOB"));   // true

        // mapKeys — collision: head key takes priority
        NonEmptyMap<String, Integer> collision = NonEmptyMap.of("a", 1, Map.of("A", 2));
        NonEmptyMap<String, Integer> mapped = collision.mapKeys(String::toUpperCase);
        System.out.println("Collision size: " + mapped.size());      // 1
        System.out.println("A value:        " + mapped.headValue()); // 1 (not 2)

        // ---- Filter ----

        Option<NonEmptyMap<String, Integer>> highScorers = scores.filter((k, v) -> v >= 20);
        highScorers.peek(m -> System.out.println("High scorers: " + m.toMap()));
        // {bob=20, carol=30}

        Option<NonEmptyMap<String, Integer>> noMatch = scores.filter((k, v) -> v > 100);
        System.out.println("No match: " + noMatch.isEmpty()); // true

        // ---- Merge ----

        NonEmptyMap<String, Integer> round1 = NonEmptyMap.of("alice", 10, Map.of("bob", 5));
        NonEmptyMap<String, Integer> round2 = NonEmptyMap.of("carol", 20, Map.of("bob", 15));

        NonEmptyMap<String, Integer> total = round1.merge(round2, Integer::sum);
        System.out.println("Merged bob: " + total.get("bob").getOrElse(0)); // 20

        // ---- Interop ----

        // keySet() — all keys as a NonEmptySet; head key becomes set head
        NonEmptySet<String> keys = scores.keySet();
        System.out.println("Keys size: " + keys.size());       // 3
        System.out.println("Keys head: " + keys.head());       // alice
        System.out.println("Keys has bob: " + keys.contains("bob")); // true

        // values() — all values as a NonEmptyList in insertion order
        NonEmptyList<Integer> vals = scores.values();
        System.out.println("Values size: " + vals.size());     // 3
        System.out.println("Values head: " + vals.head());     // 10

        // toMap — standard java.util.Map
        Map<String, Integer> javaMap = scores.toMap();
        System.out.println("Java map size: " + javaMap.size()); // 3

        // toNonEmptyList
        NonEmptyList<Map.Entry<String, Integer>> entries = scores.toNonEmptyList();
        System.out.println("Entries size: " + entries.size()); // 3

        // ---- Real-world: role permissions registry ----

        NonEmptyMap<String, Set<String>> registry = NonEmptyMap.of(
            "admin",  Set.of("read", "write", "delete"),
            Map.of(
                "editor", Set.of("read", "write"),
                "viewer", Set.of("read")
            )
        );

        Option<NonEmptyMap<String, Set<String>>> canWrite = rolesWithPermission(registry, "write");
        canWrite.peek(m -> System.out.println("Can write: " + m.toMap().keySet()));
        // [admin, editor]

        NonEmptyMap<String, Set<String>> withModerator =
            addRole(registry, "moderator", Set.of("read", "flag"));
        System.out.println("Roles after add: " + withModerator.size()); // 4
    }
}
