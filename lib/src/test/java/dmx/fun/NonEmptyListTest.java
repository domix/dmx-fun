package dmx.fun;

import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NonEmptyListTest {

    // -------------------------------------------------------------------------
    // of()
    // -------------------------------------------------------------------------

    @Test
    void of_shouldCreateListWithHeadAndTail() {
        NonEmptyList<Integer> nel = NonEmptyList.of(1, List.of(2, 3));
        assertThat(nel.head()).isEqualTo(1);
        assertThat(nel.tail()).containsExactly(2, 3);
        assertThat(nel.size()).isEqualTo(3);
    }

    @Test
    void of_shouldCreateListWithEmptyTail() {
        NonEmptyList<String> nel = NonEmptyList.of("a", List.of());
        assertThat(nel.head()).isEqualTo("a");
        assertThat(nel.tail()).isEmpty();
        assertThat(nel.size()).isEqualTo(1);
    }

    @Test
    void of_shouldThrowNPE_whenHeadIsNull() {
        assertThatThrownBy(() -> NonEmptyList.of(null, List.of(1)))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("head");
    }

    @Test
    void of_shouldThrowNPE_whenTailIsNull() {
        assertThatThrownBy(() -> NonEmptyList.of(1, null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("tail");
    }

    @Test
    void of_shouldThrowNPE_whenTailContainsNull() {
        List<Integer> tailWithNull = new java.util.ArrayList<>();
        tailWithNull.add(2);
        tailWithNull.add(null);
        assertThatThrownBy(() -> NonEmptyList.of(1, tailWithNull))
            .isInstanceOf(NullPointerException.class);
    }

    // -------------------------------------------------------------------------
    // singleton()
    // -------------------------------------------------------------------------

    @Test
    void singleton_shouldCreateSingleElementList() {
        NonEmptyList<String> nel = NonEmptyList.singleton("only");
        assertThat(nel.head()).isEqualTo("only");
        assertThat(nel.tail()).isEmpty();
        assertThat(nel.size()).isEqualTo(1);
    }

    @Test
    void singleton_shouldThrowNPE_whenElementIsNull() {
        assertThatThrownBy(() -> NonEmptyList.singleton(null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("head");
    }

    // -------------------------------------------------------------------------
    // fromList()
    // -------------------------------------------------------------------------

    @Test
    void fromList_shouldReturnSome_forNonEmptyList() {
        Option<NonEmptyList<Integer>> result = NonEmptyList.fromList(List.of(1, 2, 3));
        assertThat(result.isDefined()).isTrue();
        NonEmptyList<Integer> nel = result.get();
        assertThat(nel.head()).isEqualTo(1);
        assertThat(nel.tail()).containsExactly(2, 3);
    }

    @Test
    void fromList_shouldReturnSome_forSingletonList() {
        Option<NonEmptyList<String>> result = NonEmptyList.fromList(List.of("x"));
        assertThat(result.isDefined()).isTrue();
        assertThat(result.get().size()).isEqualTo(1);
    }

    @Test
    void fromList_shouldReturnNone_forEmptyList() {
        Option<NonEmptyList<String>> result = NonEmptyList.fromList(List.of());
        assertThat(result.isEmpty()).isTrue();
    }

    @Test
    void fromList_shouldThrowNPE_whenListIsNull() {
        assertThatThrownBy(() -> NonEmptyList.fromList(null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("list");
    }

    @Test
    void fromList_shouldThrowNPE_whenListContainsNull() {
        List<String> listWithNull = new java.util.ArrayList<>();
        listWithNull.add("a");
        listWithNull.add(null);
        assertThatThrownBy(() -> NonEmptyList.fromList(listWithNull))
            .isInstanceOf(NullPointerException.class);
    }

    // -------------------------------------------------------------------------
    // toList()
    // -------------------------------------------------------------------------

    @Test
    void toList_shouldReturnAllElements_inOrder() {
        NonEmptyList<Integer> nel = NonEmptyList.of(10, List.of(20, 30));
        assertThat(nel.toList()).containsExactly(10, 20, 30);
    }

    @Test
    void toList_shouldReturnUnmodifiableList() {
        NonEmptyList<Integer> nel = NonEmptyList.of(1, List.of(2, 3));
        assertThatThrownBy(() -> nel.toList().add(99))
            .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void toList_shouldReturnSingleElement_forSingleton() {
        assertThat(NonEmptyList.singleton("x").toList()).containsExactly("x");
    }

    // -------------------------------------------------------------------------
    // map()
    // -------------------------------------------------------------------------

    @Test
    void map_shouldTransformAllElements() {
        NonEmptyList<Integer> nel = NonEmptyList.of(1, List.of(2, 3));
        NonEmptyList<String> mapped = nel.map(Object::toString);
        assertThat(mapped.toList()).containsExactly("1", "2", "3");
    }

    @Test
    void map_shouldPreserveHeadTailStructure() {
        NonEmptyList<Integer> nel = NonEmptyList.of(5, List.of(10, 15));
        NonEmptyList<Integer> doubled = nel.map(n -> n * 2);
        assertThat(doubled.head()).isEqualTo(10);
        assertThat(doubled.tail()).containsExactly(20, 30);
    }

    @Test
    void map_shouldWorkOnSingleton() {
        NonEmptyList<Integer> nel = NonEmptyList.singleton(7);
        NonEmptyList<Integer> mapped = nel.map(n -> n + 1);
        assertThat(mapped.head()).isEqualTo(8);
        assertThat(mapped.tail()).isEmpty();
    }

    @Test
    void map_shouldThrowNPE_whenMapperIsNull() {
        NonEmptyList<Integer> nel = NonEmptyList.singleton(1);
        assertThatThrownBy(() -> nel.map(null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("mapper");
    }

    @Test
    void map_shouldThrowNPE_whenMapperReturnsNull() {
        NonEmptyList<String> nel = NonEmptyList.of("a", List.of("b"));
        assertThatThrownBy(() -> nel.map(s -> null))
            .isInstanceOf(NullPointerException.class);
    }

    // -------------------------------------------------------------------------
    // append()
    // -------------------------------------------------------------------------

    @Test
    void append_shouldAddElementAtEnd() {
        NonEmptyList<Integer> nel = NonEmptyList.of(1, List.of(2));
        NonEmptyList<Integer> result = nel.append(3);
        assertThat(result.toList()).containsExactly(1, 2, 3);
    }

    @Test
    void append_shouldNotMutateOriginal() {
        NonEmptyList<Integer> original = NonEmptyList.of(1, List.of(2));
        original.append(3);
        assertThat(original.size()).isEqualTo(2);
    }

    @Test
    void append_shouldThrowNPE_whenElementIsNull() {
        NonEmptyList<Integer> nel = NonEmptyList.singleton(1);
        assertThatThrownBy(() -> nel.append(null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("element");
    }

    // -------------------------------------------------------------------------
    // prepend()
    // -------------------------------------------------------------------------

    @Test
    void prepend_shouldAddElementAtFront() {
        NonEmptyList<Integer> nel = NonEmptyList.of(2, List.of(3));
        NonEmptyList<Integer> result = nel.prepend(1);
        assertThat(result.toList()).containsExactly(1, 2, 3);
    }

    @Test
    void prepend_shouldNotMutateOriginal() {
        NonEmptyList<Integer> original = NonEmptyList.of(2, List.of(3));
        original.prepend(1);
        assertThat(original.size()).isEqualTo(2);
    }

    @Test
    void prepend_shouldThrowNPE_whenElementIsNull() {
        NonEmptyList<Integer> nel = NonEmptyList.singleton(1);
        assertThatThrownBy(() -> nel.prepend(null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("element");
    }

    @Test
    void prepend_shouldBecomeNewHead() {
        NonEmptyList<String> nel = NonEmptyList.of("b", List.of("c"));
        NonEmptyList<String> result = nel.prepend("a");
        assertThat(result.head()).isEqualTo("a");
        assertThat(result.tail()).containsExactly("b", "c");
    }

    // -------------------------------------------------------------------------
    // concat()
    // -------------------------------------------------------------------------

    @Test
    void concat_shouldCombineBothLists() {
        NonEmptyList<Integer> a = NonEmptyList.of(1, List.of(2));
        NonEmptyList<Integer> b = NonEmptyList.of(3, List.of(4, 5));
        NonEmptyList<Integer> result = a.concat(b);
        assertThat(result.toList()).containsExactly(1, 2, 3, 4, 5);
    }

    @Test
    void concat_shouldPreserveHeadOfFirst() {
        NonEmptyList<String> a = NonEmptyList.singleton("x");
        NonEmptyList<String> b = NonEmptyList.of("y", List.of("z"));
        assertThat(a.concat(b).head()).isEqualTo("x");
    }

    @Test
    void concat_shouldNotMutateEither() {
        NonEmptyList<Integer> a = NonEmptyList.of(1, List.of(2));
        NonEmptyList<Integer> b = NonEmptyList.of(3, List.of(4));
        a.concat(b);
        assertThat(a.size()).isEqualTo(2);
        assertThat(b.size()).isEqualTo(2);
    }

    @Test
    void concat_shouldThrowNPE_whenOtherIsNull() {
        NonEmptyList<Integer> nel = NonEmptyList.singleton(1);
        assertThatThrownBy(() -> nel.concat(null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("other");
    }

    // -------------------------------------------------------------------------
    // Iterable
    // -------------------------------------------------------------------------

    @Test
    void iterator_shouldYieldAllElementsInOrder() {
        NonEmptyList<Integer> nel = NonEmptyList.of(10, List.of(20, 30));
        java.util.List<Integer> collected = new java.util.ArrayList<>();
        for (Integer i : nel) {
            collected.add(i);
        }
        assertThat(collected).containsExactly(10, 20, 30);
    }

    // -------------------------------------------------------------------------
    // equals / hashCode / toString
    // -------------------------------------------------------------------------

    @Test
    void equals_shouldBeTrue_forSameContent() {
        NonEmptyList<Integer> a = NonEmptyList.of(1, List.of(2, 3));
        NonEmptyList<Integer> b = NonEmptyList.of(1, List.of(2, 3));
        assertThat(a).isEqualTo(b);
    }

    @Test
    void equals_shouldBeFalse_forDifferentContent() {
        NonEmptyList<Integer> a = NonEmptyList.of(1, List.of(2));
        NonEmptyList<Integer> b = NonEmptyList.of(1, List.of(3));
        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void equals_shouldBeFalse_forDifferentSize() {
        NonEmptyList<Integer> a = NonEmptyList.of(1, List.of(2));
        NonEmptyList<Integer> b = NonEmptyList.of(1, List.of(2, 3));
        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void equals_shouldBeTrue_forSameInstance() {
        NonEmptyList<Integer> nel = NonEmptyList.singleton(42);
        assertThat(nel).isEqualTo(nel);
    }

    @Test
    void equals_shouldBeFalse_forNull() {
        assertThat(NonEmptyList.singleton(1)).isNotEqualTo(null);
    }

    @Test
    void equals_shouldBeFalse_forDifferentType() {
        assertThat(NonEmptyList.singleton(1)).isNotEqualTo(List.of(1));
    }

    @Test
    void hashCode_shouldBeConsistent_forEqualLists() {
        NonEmptyList<Integer> a = NonEmptyList.of(1, List.of(2, 3));
        NonEmptyList<Integer> b = NonEmptyList.of(1, List.of(2, 3));
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }

    @Test
    void toString_shouldContainAllElements() {
        NonEmptyList<Integer> nel = NonEmptyList.of(1, List.of(2, 3));
        assertThat(nel.toString()).contains("1", "2", "3");
        assertThat(nel.toString()).startsWith("NonEmptyList");
    }
}
