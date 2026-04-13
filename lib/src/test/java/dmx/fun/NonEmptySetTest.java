package dmx.fun;

import java.util.Set;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NonEmptySetTest {

    // -------------------------------------------------------------------------
    // of()
    // -------------------------------------------------------------------------

    @Test
    void of_shouldCreateSetWithHeadAndRest() {
        NonEmptySet<String> nes = NonEmptySet.of("admin", Set.of("user", "moderator"));
        assertThat(nes.head()).isEqualTo("admin");
        assertThat(nes.size()).isEqualTo(3);
    }

    @Test
    void of_shouldCreateSetWithEmptyRest() {
        NonEmptySet<String> nes = NonEmptySet.of("admin", Set.of());
        assertThat(nes.size()).isEqualTo(1);
    }

    @Test
    void of_shouldIgnoreDuplicateHeadInRest() {
        NonEmptySet<String> nes = NonEmptySet.of("admin", Set.of("admin", "user"));
        assertThat(nes.size()).isEqualTo(2);
        assertThat(nes.contains("admin")).isTrue();
        assertThat(nes.contains("user")).isTrue();
    }

    @Test
    void of_shouldThrowNPE_whenHeadIsNull() {
        assertThatThrownBy(() -> NonEmptySet.of(null, Set.of()))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("head");
    }

    @Test
    void of_shouldThrowNPE_whenRestIsNull() {
        assertThatThrownBy(() -> NonEmptySet.of("x", null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("rest");
    }

    @Test
    void of_shouldThrowNPE_whenRestContainsNull() {
        Set<String> restWithNull = new java.util.HashSet<>();
        restWithNull.add("user");
        restWithNull.add(null);
        assertThatThrownBy(() -> NonEmptySet.of("admin", restWithNull))
            .isInstanceOf(NullPointerException.class);
    }

    // -------------------------------------------------------------------------
    // singleton()
    // -------------------------------------------------------------------------

    @Test
    void singleton_shouldCreateSingleElementSet() {
        NonEmptySet<String> nes = NonEmptySet.singleton("admin");
        assertThat(nes.head()).isEqualTo("admin");
        assertThat(nes.size()).isEqualTo(1);
    }

    @Test
    void singleton_shouldThrowNPE_whenHeadIsNull() {
        assertThatThrownBy(() -> NonEmptySet.singleton(null))
            .isInstanceOf(NullPointerException.class);
    }

    // -------------------------------------------------------------------------
    // fromSet()
    // -------------------------------------------------------------------------

    @Test
    void fromSet_shouldReturnSome_whenSetIsNonEmpty() {
        Option<NonEmptySet<String>> result = NonEmptySet.fromSet(Set.of("a", "b"));
        assertThat(result.isDefined()).isTrue();
        assertThat(result.get().size()).isEqualTo(2);
    }

    @Test
    void fromSet_shouldReturnNone_whenSetIsEmpty() {
        Option<NonEmptySet<String>> result = NonEmptySet.fromSet(Set.of());
        assertThat(result.isEmpty()).isTrue();
    }

    @Test
    void fromSet_shouldThrowNPE_whenSetIsNull() {
        assertThatThrownBy(() -> NonEmptySet.fromSet(null))
            .isInstanceOf(NullPointerException.class);
    }

    // -------------------------------------------------------------------------
    // contains() / toSet()
    // -------------------------------------------------------------------------

    @Test
    void contains_shouldReturnTrue_forHead() {
        NonEmptySet<String> nes = NonEmptySet.singleton("admin");
        assertThat(nes.contains("admin")).isTrue();
    }

    @Test
    void contains_shouldReturnTrue_forTailElement() {
        NonEmptySet<String> nes = NonEmptySet.of("admin", Set.of("user"));
        assertThat(nes.contains("user")).isTrue();
    }

    @Test
    void contains_shouldReturnFalse_forAbsentElement() {
        NonEmptySet<String> nes = NonEmptySet.singleton("admin");
        assertThat(nes.contains("unknown")).isFalse();
    }

    @Test
    void toSet_shouldReturnAllElements() {
        NonEmptySet<String> nes = NonEmptySet.of("admin", Set.of("user"));
        assertThat(nes.toSet()).containsExactlyInAnyOrder("admin", "user");
    }

    @Test
    void toSet_shouldBeUnmodifiable() {
        NonEmptySet<String> nes = NonEmptySet.singleton("admin");
        assertThatThrownBy(() -> nes.toSet().add("x"))
            .isInstanceOf(UnsupportedOperationException.class);
    }

    // -------------------------------------------------------------------------
    // map()
    // -------------------------------------------------------------------------

    @Test
    void map_shouldTransformAllElements() {
        NonEmptySet<String> nes = NonEmptySet.of("admin", Set.of("user"));
        NonEmptySet<String> upper = nes.map(String::toUpperCase);
        assertThat(upper.contains("ADMIN")).isTrue();
        assertThat(upper.contains("USER")).isTrue();
    }

    @Test
    void map_shouldDeduplicateMappedElements() {
        NonEmptySet<String> nes = NonEmptySet.of("a", Set.of("A"));
        NonEmptySet<String> mapped = nes.map(String::toUpperCase);
        assertThat(mapped.size()).isEqualTo(1);
        assertThat(mapped.head()).isEqualTo("A");
    }

    @Test
    void map_shouldThrowNPE_whenMapperIsNull() {
        NonEmptySet<String> nes = NonEmptySet.singleton("x");
        assertThatThrownBy(() -> nes.map(null))
            .isInstanceOf(NullPointerException.class);
    }

    // -------------------------------------------------------------------------
    // filter()
    // -------------------------------------------------------------------------

    @Test
    void filter_shouldReturnSome_whenSomeElementsPass() {
        NonEmptySet<Integer> nes = NonEmptySet.of(1, Set.of(2, 3));
        Option<NonEmptySet<Integer>> result = nes.filter(n -> n > 1);
        assertThat(result.isDefined()).isTrue();
        assertThat(result.get().size()).isEqualTo(2);
    }

    @Test
    void filter_shouldReturnNone_whenNoElementsPass() {
        NonEmptySet<Integer> nes = NonEmptySet.singleton(1);
        Option<NonEmptySet<Integer>> result = nes.filter(n -> n > 100);
        assertThat(result.isEmpty()).isTrue();
    }

    @Test
    void filter_shouldReturnAll_whenAllElementsPass() {
        NonEmptySet<Integer> nes = NonEmptySet.of(1, Set.of(2, 3));
        Option<NonEmptySet<Integer>> result = nes.filter(n -> n > 0);
        assertThat(result.isDefined()).isTrue();
        assertThat(result.get().size()).isEqualTo(3);
    }

    // -------------------------------------------------------------------------
    // union()
    // -------------------------------------------------------------------------

    @Test
    void union_shouldCombineDisjointSets() {
        NonEmptySet<String> a = NonEmptySet.singleton("admin");
        NonEmptySet<String> b = NonEmptySet.singleton("user");
        NonEmptySet<String> union = a.union(b);
        assertThat(union.size()).isEqualTo(2);
        assertThat(union.contains("admin")).isTrue();
        assertThat(union.contains("user")).isTrue();
    }

    @Test
    void union_shouldDeduplicateOverlappingElements() {
        NonEmptySet<String> a = NonEmptySet.of("admin", Set.of("user"));
        NonEmptySet<String> b = NonEmptySet.singleton("user");
        NonEmptySet<String> union = a.union(b);
        assertThat(union.size()).isEqualTo(2);
    }

    @Test
    void union_shouldThrowNPE_whenOtherIsNull() {
        NonEmptySet<String> nes = NonEmptySet.singleton("x");
        assertThatThrownBy(() -> nes.union(null))
            .isInstanceOf(NullPointerException.class);
    }

    // -------------------------------------------------------------------------
    // intersection()
    // -------------------------------------------------------------------------

    @Test
    void intersection_shouldReturnSome_whenCommonElementsExist() {
        NonEmptySet<String> nes = NonEmptySet.of("admin", Set.of("user"));
        Option<NonEmptySet<String>> result = nes.intersection(Set.of("admin", "moderator"));
        assertThat(result.isDefined()).isTrue();
        assertThat(result.get().size()).isEqualTo(1);
        assertThat(result.get().contains("admin")).isTrue();
    }

    @Test
    void intersection_shouldReturnNone_whenNoCommonElements() {
        NonEmptySet<String> nes = NonEmptySet.singleton("admin");
        Option<NonEmptySet<String>> result = nes.intersection(Set.of("user"));
        assertThat(result.isEmpty()).isTrue();
    }

    @Test
    void intersection_shouldThrowNPE_whenOtherIsNull() {
        NonEmptySet<String> nes = NonEmptySet.singleton("x");
        assertThatThrownBy(() -> nes.intersection(null))
            .isInstanceOf(NullPointerException.class);
    }

    // -------------------------------------------------------------------------
    // toNonEmptyList()
    // -------------------------------------------------------------------------

    @Test
    void toNonEmptyList_shouldContainAllElements() {
        NonEmptySet<String> nes = NonEmptySet.of("admin", Set.of("user", "moderator"));
        NonEmptyList<String> nel = nes.toNonEmptyList();
        assertThat(nel.size()).isEqualTo(3);
        assertThat(nel.toList()).containsExactlyInAnyOrder("admin", "user", "moderator");
    }

    // -------------------------------------------------------------------------
    // Iterable
    // -------------------------------------------------------------------------

    @Test
    void iterable_shouldIterateAllElements() {
        NonEmptySet<String> nes = NonEmptySet.of("admin", Set.of("user"));
        int count = 0;
        for (String s : nes) count++;
        assertThat(count).isEqualTo(2);
    }

    // -------------------------------------------------------------------------
    // equals / hashCode / toString
    // -------------------------------------------------------------------------

    @Test
    void equals_shouldBeTrue_forEqualSets() {
        NonEmptySet<String> a = NonEmptySet.singleton("x");
        NonEmptySet<String> b = NonEmptySet.singleton("x");
        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }

    @Test
    void equals_shouldBeFalse_forDifferentSets() {
        NonEmptySet<String> a = NonEmptySet.singleton("x");
        NonEmptySet<String> b = NonEmptySet.singleton("y");
        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void toString_shouldIncludeElements() {
        NonEmptySet<String> nes = NonEmptySet.singleton("admin");
        assertThat(nes.toString()).contains("admin");
    }
}
