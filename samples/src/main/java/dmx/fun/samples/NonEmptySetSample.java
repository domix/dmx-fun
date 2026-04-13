package dmx.fun.samples;

import dmx.fun.NonEmptyList;
import dmx.fun.NonEmptySet;
import dmx.fun.Option;
import java.util.Set;

/**
 * Demonstrates NonEmptySet<T>: a set guaranteed to have at least one element at compile time.
 * Use NonEmptySet when set semantics (no duplicates) are needed with a non-emptiness guarantee,
 * e.g. user roles, product tags, or required permission sets.
 */
public class NonEmptySetSample {

    // -------------------------------------------------------------------------
    // Access-control helpers backed by NonEmptySet.
    // A user's roles are always non-empty — the type system enforces this.
    // -------------------------------------------------------------------------

    record UserAccess(String username, NonEmptySet<String> roles) {}

    static boolean hasAnyRole(UserAccess user, NonEmptySet<String> required) {
        return user.roles().intersection(required.toSet()).isDefined();
    }

    static boolean hasAllRoles(UserAccess user, Set<String> required) {
        return user.roles().intersection(required)
            .map(common -> common.size() == required.size())
            .getOrElse(false);
    }

    static UserAccess addRole(UserAccess user, String newRole) {
        return new UserAccess(user.username(), user.roles().union(NonEmptySet.singleton(newRole)));
    }

    static void main(String[] args) {
        // ---- Construction ----

        // of(head, rest) — head element + additional elements
        NonEmptySet<String> roles = NonEmptySet.of("admin", Set.of("editor", "viewer"));
        System.out.println("Head: " + roles.head());  // admin
        System.out.println("Size: " + roles.size());  // 3

        // Duplicate head in rest is silently ignored
        NonEmptySet<String> dedup = NonEmptySet.of("admin", Set.of("admin", "editor"));
        System.out.println("Dedup size: " + dedup.size()); // 2

        // singleton
        NonEmptySet<String> single = NonEmptySet.singleton("admin");
        System.out.println("Single size: " + single.size()); // 1

        // fromSet — safe bridge from plain Set
        Option<NonEmptySet<String>> fromEmpty = NonEmptySet.fromSet(Set.of());
        System.out.println("From empty: " + fromEmpty.isEmpty());  // true

        Option<NonEmptySet<String>> fromFull = NonEmptySet.fromSet(Set.of("a", "b"));
        System.out.println("From full:  " + fromFull.isDefined()); // true

        // ---- Accessing elements ----

        System.out.println("contains admin:   " + roles.contains("admin"));   // true
        System.out.println("contains unknown: " + roles.contains("unknown")); // false

        // toSet — unmodifiable java.util.Set
        Set<String> javaSet = roles.toSet();
        System.out.println("Java set size: " + javaSet.size()); // 3

        // Iterable — direct for-each
        System.out.print("Roles: ");
        for (String role : roles) System.out.print(role + " ");
        System.out.println();

        // ---- Transformations ----

        NonEmptySet<String> upper = roles.map(String::toUpperCase);
        System.out.println("Upper contains ADMIN: " + upper.contains("ADMIN")); // true

        // map with deduplication
        NonEmptySet<String> collision = NonEmptySet.of("a", Set.of("A"));
        NonEmptySet<String> mapped = collision.map(String::toUpperCase);
        System.out.println("Collision size: " + mapped.size());  // 1
        System.out.println("Mapped head:    " + mapped.head()); // A

        // ---- Filter ----

        NonEmptySet<Integer> nums = NonEmptySet.of(1, Set.of(2, 3, 4, 5));

        Option<NonEmptySet<Integer>> evens = nums.filter(n -> n % 2 == 0);
        evens.peek(s -> System.out.println("Evens: " + s.toSet())); // [2, 4]

        Option<NonEmptySet<Integer>> none = nums.filter(n -> n > 100);
        System.out.println("No match: " + none.isEmpty()); // true

        // ---- Set operations ----

        NonEmptySet<String> a = NonEmptySet.of("admin", Set.of("editor"));
        NonEmptySet<String> b = NonEmptySet.of("editor", Set.of("viewer"));

        // union — always non-empty
        NonEmptySet<String> union = a.union(b);
        System.out.println("Union size: " + union.size());             // 3
        System.out.println("Union has viewer: " + union.contains("viewer")); // true

        // intersection — may be empty, returns Option
        Option<NonEmptySet<String>> common = a.intersection(b.toSet());
        common.peek(s -> System.out.println("Common: " + s.toSet())); // [editor]

        Option<NonEmptySet<String>> noCommon = a.intersection(Set.of("unknown"));
        System.out.println("No common: " + noCommon.isEmpty()); // true

        // ---- Interop ----

        // toNonEmptyList — ordered snapshot
        NonEmptyList<String> nel = roles.toNonEmptyList();
        System.out.println("NEL size: " + nel.size()); // 3

        // ---- Real-world: access control ----

        UserAccess alice = new UserAccess("alice", NonEmptySet.of("admin", Set.of("editor")));
        UserAccess bob   = new UserAccess("bob",   NonEmptySet.singleton("viewer"));

        System.out.println("alice can edit: " +
            hasAnyRole(alice, NonEmptySet.singleton("editor")));       // true
        System.out.println("bob can write:  " +
            hasAllRoles(bob, Set.of("viewer", "editor")));             // false
        System.out.println("bob has viewer: " +
            hasAnyRole(bob, NonEmptySet.singleton("viewer")));         // true

        UserAccess bobPromoted = addRole(bob, "editor");
        System.out.println("Bob promoted size: " + bobPromoted.roles().size()); // 2

        // Combined roles (e.g. for delegation)
        NonEmptySet<String> allRoles = alice.roles().union(bob.roles());
        System.out.println("Combined roles: " + allRoles.toSet()); // [admin, editor, viewer]
    }
}
