package dmx.fun;

import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NonEmptyMapTest {

    // -------------------------------------------------------------------------
    // of()
    // -------------------------------------------------------------------------

    @Test
    void of_shouldCreateMapWithHeadAndRest() {
        NonEmptyMap<String, Integer> nem = NonEmptyMap.of("alice", 10, Map.of("bob", 20));
        assertThat(nem.headKey()).isEqualTo("alice");
        assertThat(nem.headValue()).isEqualTo(10);
        assertThat(nem.size()).isEqualTo(2);
    }

    @Test
    void of_shouldCreateMapWithEmptyRest() {
        NonEmptyMap<String, Integer> nem = NonEmptyMap.of("alice", 10, Map.of());
        assertThat(nem.size()).isEqualTo(1);
    }

    @Test
    void of_shouldIgnoreDuplicateKeyInRest() {
        NonEmptyMap<String, Integer> nem = NonEmptyMap.of("alice", 10, Map.of("alice", 99));
        assertThat(nem.size()).isEqualTo(1);
        assertThat(nem.headValue()).isEqualTo(10);
    }

    @Test
    void of_shouldThrowNPE_whenKeyIsNull() {
        assertThatThrownBy(() -> NonEmptyMap.of(null, 1, Map.of()))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("key");
    }

    @Test
    void of_shouldThrowNPE_whenValueIsNull() {
        assertThatThrownBy(() -> NonEmptyMap.of("k", null, Map.of()))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("value");
    }

    @Test
    void of_shouldThrowNPE_whenRestIsNull() {
        assertThatThrownBy(() -> NonEmptyMap.of("k", 1, null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("rest");
    }

    // -------------------------------------------------------------------------
    // singleton()
    // -------------------------------------------------------------------------

    @Test
    void singleton_shouldCreateSingleEntryMap() {
        NonEmptyMap<String, Integer> nem = NonEmptyMap.singleton("alice", 10);
        assertThat(nem.headKey()).isEqualTo("alice");
        assertThat(nem.headValue()).isEqualTo(10);
        assertThat(nem.size()).isEqualTo(1);
    }

    @Test
    void singleton_shouldThrowNPE_whenKeyIsNull() {
        assertThatThrownBy(() -> NonEmptyMap.singleton(null, 1))
            .isInstanceOf(NullPointerException.class);
    }

    // -------------------------------------------------------------------------
    // fromMap()
    // -------------------------------------------------------------------------

    @Test
    void fromMap_shouldReturnSome_whenMapIsNonEmpty() {
        Option<NonEmptyMap<String, Integer>> result = NonEmptyMap.fromMap(Map.of("a", 1));
        assertThat(result.isDefined()).isTrue();
        assertThat(result.get().size()).isEqualTo(1);
    }

    @Test
    void fromMap_shouldReturnNone_whenMapIsEmpty() {
        Option<NonEmptyMap<String, Integer>> result = NonEmptyMap.fromMap(Map.of());
        assertThat(result.isEmpty()).isTrue();
    }

    @Test
    void fromMap_shouldThrowNPE_whenMapIsNull() {
        assertThatThrownBy(() -> NonEmptyMap.fromMap(null))
            .isInstanceOf(NullPointerException.class);
    }

    // -------------------------------------------------------------------------
    // get() / containsKey()
    // -------------------------------------------------------------------------

    @Test
    void get_shouldReturnSome_forHeadKey() {
        NonEmptyMap<String, Integer> nem = NonEmptyMap.singleton("alice", 10);
        assertThat(nem.get("alice").isDefined()).isTrue();
        assertThat(nem.get("alice").get()).isEqualTo(10);
    }

    @Test
    void get_shouldReturnSome_forTailKey() {
        NonEmptyMap<String, Integer> nem = NonEmptyMap.of("alice", 10, Map.of("bob", 20));
        assertThat(nem.get("bob").isDefined()).isTrue();
        assertThat(nem.get("bob").get()).isEqualTo(20);
    }

    @Test
    void get_shouldReturnNone_forAbsentKey() {
        NonEmptyMap<String, Integer> nem = NonEmptyMap.singleton("alice", 10);
        assertThat(nem.get("unknown").isEmpty()).isTrue();
    }

    @Test
    void containsKey_shouldReturnTrue_forHeadKey() {
        NonEmptyMap<String, Integer> nem = NonEmptyMap.singleton("alice", 10);
        assertThat(nem.containsKey("alice")).isTrue();
    }

    @Test
    void containsKey_shouldReturnFalse_forAbsentKey() {
        NonEmptyMap<String, Integer> nem = NonEmptyMap.singleton("alice", 10);
        assertThat(nem.containsKey("bob")).isFalse();
    }

    // -------------------------------------------------------------------------
    // toMap()
    // -------------------------------------------------------------------------

    @Test
    void toMap_shouldReturnAllEntries() {
        NonEmptyMap<String, Integer> nem = NonEmptyMap.of("alice", 10, Map.of("bob", 20));
        Map<String, Integer> map = nem.toMap();
        assertThat(map).containsEntry("alice", 10).containsEntry("bob", 20);
    }

    @Test
    void toMap_shouldBeUnmodifiable() {
        NonEmptyMap<String, Integer> nem = NonEmptyMap.singleton("alice", 10);
        assertThatThrownBy(() -> nem.toMap().put("x", 1))
            .isInstanceOf(UnsupportedOperationException.class);
    }

    // -------------------------------------------------------------------------
    // mapValues()
    // -------------------------------------------------------------------------

    @Test
    void mapValues_shouldTransformAllValues() {
        NonEmptyMap<String, Integer> nem = NonEmptyMap.of("alice", 10, Map.of("bob", 20));
        NonEmptyMap<String, String> mapped = nem.mapValues(v -> v + " pts");
        assertThat(mapped.get("alice").get()).isEqualTo("10 pts");
        assertThat(mapped.get("bob").get()).isEqualTo("20 pts");
    }

    @Test
    void mapValues_shouldThrowNPE_whenMapperIsNull() {
        NonEmptyMap<String, Integer> nem = NonEmptyMap.singleton("a", 1);
        assertThatThrownBy(() -> nem.mapValues(null))
            .isInstanceOf(NullPointerException.class);
    }

    // -------------------------------------------------------------------------
    // mapKeys()
    // -------------------------------------------------------------------------

    @Test
    void mapKeys_shouldTransformAllKeys() {
        NonEmptyMap<String, Integer> nem = NonEmptyMap.of("alice", 10, Map.of("bob", 20));
        NonEmptyMap<String, Integer> mapped = nem.mapKeys(String::toUpperCase);
        assertThat(mapped.containsKey("ALICE")).isTrue();
        assertThat(mapped.containsKey("BOB")).isTrue();
    }

    @Test
    void mapKeys_shouldDropDuplicateKeysKeepingHead() {
        NonEmptyMap<String, Integer> nem = NonEmptyMap.of("a", 1, Map.of("A", 2));
        NonEmptyMap<String, Integer> mapped = nem.mapKeys(String::toUpperCase);
        assertThat(mapped.size()).isEqualTo(1);
        assertThat(mapped.headKey()).isEqualTo("A");
        assertThat(mapped.headValue()).isEqualTo(1);
    }

    // -------------------------------------------------------------------------
    // filter()
    // -------------------------------------------------------------------------

    @Test
    void filter_shouldReturnSome_whenSomeEntriesPass() {
        NonEmptyMap<String, Integer> nem = NonEmptyMap.of("alice", 10, Map.of("bob", 20));
        Option<NonEmptyMap<String, Integer>> result = nem.filter((k, v) -> v > 15);
        assertThat(result.isDefined()).isTrue();
        assertThat(result.get().size()).isEqualTo(1);
        assertThat(result.get().containsKey("bob")).isTrue();
    }

    @Test
    void filter_shouldReturnNone_whenNoEntriesPass() {
        NonEmptyMap<String, Integer> nem = NonEmptyMap.singleton("alice", 10);
        Option<NonEmptyMap<String, Integer>> result = nem.filter((k, v) -> v > 100);
        assertThat(result.isEmpty()).isTrue();
    }

    @Test
    void filter_shouldReturnAll_whenAllEntriesPass() {
        NonEmptyMap<String, Integer> nem = NonEmptyMap.of("a", 1, Map.of("b", 2));
        Option<NonEmptyMap<String, Integer>> result = nem.filter((k, v) -> v > 0);
        assertThat(result.isDefined()).isTrue();
        assertThat(result.get().size()).isEqualTo(2);
    }

    // -------------------------------------------------------------------------
    // merge()
    // -------------------------------------------------------------------------

    @Test
    void merge_shouldCombineDistinctEntries() {
        NonEmptyMap<String, Integer> a = NonEmptyMap.singleton("alice", 10);
        NonEmptyMap<String, Integer> b = NonEmptyMap.singleton("bob", 20);
        NonEmptyMap<String, Integer> merged = a.merge(b, Integer::sum);
        assertThat(merged.size()).isEqualTo(2);
        assertThat(merged.containsKey("alice")).isTrue();
        assertThat(merged.containsKey("bob")).isTrue();
    }

    @Test
    void merge_shouldApplyMergeFunctionOnConflict() {
        NonEmptyMap<String, Integer> a = NonEmptyMap.singleton("alice", 10);
        NonEmptyMap<String, Integer> b = NonEmptyMap.singleton("alice", 5);
        NonEmptyMap<String, Integer> merged = a.merge(b, Integer::sum);
        assertThat(merged.size()).isEqualTo(1);
        assertThat(merged.get("alice").get()).isEqualTo(15);
    }

    // -------------------------------------------------------------------------
    // keySet()
    // -------------------------------------------------------------------------

    @Test
    void keySet_shouldReturnAllKeys() {
        NonEmptyMap<String, Integer> nem = NonEmptyMap.of("alice", 10, Map.of("bob", 20));
        NonEmptySet<String> keys = nem.keySet();
        assertThat(keys.size()).isEqualTo(2);
        assertThat(keys.contains("alice")).isTrue();
        assertThat(keys.contains("bob")).isTrue();
    }

    @Test
    void keySet_headShouldMatchMapHeadKey() {
        NonEmptyMap<String, Integer> nem = NonEmptyMap.of("alice", 10, Map.of("bob", 20));
        assertThat(nem.keySet().head()).isEqualTo("alice");
    }

    @Test
    void keySet_singletonMapShouldYieldSingletonSet() {
        NonEmptyMap<String, Integer> nem = NonEmptyMap.singleton("alice", 10);
        assertThat(nem.keySet().size()).isEqualTo(1);
        assertThat(nem.keySet().head()).isEqualTo("alice");
    }

    // -------------------------------------------------------------------------
    // values()
    // -------------------------------------------------------------------------

    @Test
    void values_shouldReturnAllValues() {
        NonEmptyMap<String, Integer> nem = NonEmptyMap.of("alice", 10, Map.of("bob", 20));
        NonEmptyList<Integer> vals = nem.values();
        assertThat(vals.size()).isEqualTo(2);
        assertThat(vals.toList()).containsExactlyInAnyOrder(10, 20);
    }

    @Test
    void values_headShouldMatchMapHeadValue() {
        NonEmptyMap<String, Integer> nem = NonEmptyMap.singleton("alice", 42);
        assertThat(nem.values().head()).isEqualTo(42);
    }

    @Test
    void values_shouldPreserveDuplicateValues() {
        NonEmptyMap<String, Integer> nem = NonEmptyMap.of("alice", 10, Map.of("bob", 10));
        assertThat(nem.values().size()).isEqualTo(2);
    }

    // -------------------------------------------------------------------------
    // toNonEmptyList()
    // -------------------------------------------------------------------------

    @Test
    void toNonEmptyList_shouldContainAllEntries() {
        NonEmptyMap<String, Integer> nem = NonEmptyMap.of("alice", 10, Map.of("bob", 20));
        NonEmptyList<Map.Entry<String, Integer>> nel = nem.toNonEmptyList();
        assertThat(nel.size()).isEqualTo(2);
    }

    // -------------------------------------------------------------------------
    // equals / hashCode / toString
    // -------------------------------------------------------------------------

    @Test
    void equals_shouldBeTrue_forEqualMaps() {
        NonEmptyMap<String, Integer> a = NonEmptyMap.singleton("x", 1);
        NonEmptyMap<String, Integer> b = NonEmptyMap.singleton("x", 1);
        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }

    @Test
    void equals_shouldBeFalse_forDifferentMaps() {
        NonEmptyMap<String, Integer> a = NonEmptyMap.singleton("x", 1);
        NonEmptyMap<String, Integer> b = NonEmptyMap.singleton("y", 2);
        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void toString_shouldIncludeEntries() {
        NonEmptyMap<String, Integer> nem = NonEmptyMap.singleton("alice", 10);
        assertThat(nem.toString()).contains("alice").contains("10");
    }
}
