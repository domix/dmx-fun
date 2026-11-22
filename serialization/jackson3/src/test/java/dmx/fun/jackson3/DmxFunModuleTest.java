package dmx.fun.jackson3;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.DatabindException;
import tools.jackson.databind.DefaultTyping;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.cfg.EnumFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import tools.jackson.databind.jsontype.NamedType;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.exc.InvalidFormatException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.exc.MismatchedInputException;
import dmx.fun.Either;
import dmx.fun.NonEmptyList;
import dmx.fun.NonEmptyMap;
import dmx.fun.NonEmptySet;
import dmx.fun.Option;
import dmx.fun.Result;
import dmx.fun.Try;
import dmx.fun.Tuple2;
import dmx.fun.Tuple3;
import dmx.fun.Tuple4;
import dmx.fun.Validated;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;

import static dmx.fun.assertj.DmxFunAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DmxFunModuleTest {

    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = JsonMapper.builder().addModule(new DmxFunModule()).build();
    }

    // -------------------------------------------------------------------------
    // Option
    // -------------------------------------------------------------------------

    @Nested
    class OptionTests {

        @Test
        void serializeSome() throws Exception {
            var some = Option.some("hello");
            var json = mapper.writeValueAsString(some);
            assertThat(json).isEqualTo("\"hello\"");
        }

        @Test
        void serializeNone() throws Exception {
            var none = Option.none();
            var json = mapper.writeValueAsString(none);
            assertThat(json).isEqualTo("null");
        }

        @Test
        void deserializeSome() throws Exception {
            var result = mapper.readValue("\"world\"", new TypeReference<Option<String>>() {});
            assertThat(result).isSome().containsValue("world");
        }

        @Test
        void deserializeNone() throws Exception {
            var result = mapper.readValue("null", new TypeReference<Option<String>>() {});
            assertThat(result).isNone();
        }

        @Test
        void roundTripSome() throws Exception {
            var original = Option.some(42);
            var json = mapper.writeValueAsString(original);
            var deserialized = mapper.readValue(json, new TypeReference<Option<Integer>>() {});
            assertThat(deserialized).isSome().containsValue(42);
        }

        @Test
        void roundTripNone() throws Exception {
            var original = Option.none();
            var json = mapper.writeValueAsString(original);
            var deserialized = mapper.readValue(json, new TypeReference<Option<Integer>>() {});
            assertThat(deserialized).isNone();
        }

        @Test
        @SuppressWarnings("unchecked")
        void deserializeRawType_coversNullValueTypePath() throws Exception {
            // No TypeReference → createContextual returns `this` (no type params),
            // deserialize falls back to p.readValueAs(Object.class)
            var result = mapper.readValue("\"raw\"", Option.class);
            assertThat(result).isSome();
            assertThat(result.get()).isEqualTo("raw");
        }
    }

    // -------------------------------------------------------------------------
    // Result
    // -------------------------------------------------------------------------

    @Nested
    class ResultTests {

        @Test
        void serializeOk() throws Exception {
            var ok = Result.ok("success");
            var json = mapper.writeValueAsString(ok);
            assertThat(json).isEqualTo("{\"ok\":\"success\"}");
        }

        @Test
        void serializeErr() throws Exception {
            var err = Result.err(404);
            var json = mapper.writeValueAsString(err);
            assertThat(json).isEqualTo("{\"err\":404}");
        }

        @Test
        void roundTripOk() throws Exception {
            var original = Result.ok("hello");
            var json = mapper.writeValueAsString(original);
            var deserialized = mapper.readValue(json, new TypeReference<Result<String, Integer>>() {});
            assertThat(deserialized).isOk().containsValue("hello");
        }

        @Test
        void roundTripErr() throws Exception {
            var original = Result.err(500);
            var json = mapper.writeValueAsString(original);
            var deserialized = mapper.readValue(json, new TypeReference<Result<String, Integer>>() {});
            assertThat(deserialized).isErr().containsError(500);
        }

        @Test
        @SuppressWarnings("unchecked")
        void deserializeOk_rawType_coversNullValueTypePath() throws Exception {
            var result = mapper.readValue("{\"ok\":99}", Result.class);
            assertThat(result).isOk();
            assertThat(result.get()).isEqualTo(99);
        }

        @Test
        @SuppressWarnings("unchecked")
        void deserializeErr_rawType_coversNullErrorTypePath() throws Exception {
            var result = mapper.readValue("{\"err\":\"oops\"}", Result.class);
            assertThat(result).isErr();
            assertThat(result.getError()).isEqualTo("oops");
        }

        @Test
        void deserializeUnknownField_throws() {
            var ex = assertThrows(DatabindException.class, () ->
                mapper.readValue("{\"unknown\":42}", new TypeReference<Result<Integer, String>>() {}));
            assertThat(ex.getMessage()).contains("unknown");
        }

        @Test
        void deserializeFromNonObject_throws() {
            assertThrows(DatabindException.class, () ->
                mapper.readValue("42", new TypeReference<Result<Integer, String>>() {}));
        }

        @Test
        void deserializeEmptyObject_throws() {
            assertThrows(DatabindException.class, () ->
                mapper.readValue("{}", new TypeReference<Result<Integer, String>>() {}));
        }

        @Test
        void deserializeExtraField_throws() {
            var ex = assertThrows(DatabindException.class, () ->
                mapper.readValue("{\"ok\":1,\"extra\":2}", new TypeReference<Result<Integer, String>>() {}));
            assertThat(ex.getMessage()).contains("extra");
        }
    }

    // -------------------------------------------------------------------------
    // Try
    // -------------------------------------------------------------------------

    @Nested
    class TryTests {

        @Test
        void serializeSuccess() throws Exception {
            var success = Try.success("ok");
            var json = mapper.writeValueAsString(success);
            assertThat(json).isEqualTo("\"ok\"");
        }

        @Test
        void serializeFailure() throws Exception {
            var failure = Try.failure(new RuntimeException("boom"));
            var json = mapper.writeValueAsString(failure);
            assertThat(json).isEqualTo("{\"error\":\"boom\"}");
        }

        @Test
        void roundTripSuccess() throws Exception {
            var original = Try.success(99);
            var json = mapper.writeValueAsString(original);
            var deserialized = mapper.readValue(json, new TypeReference<Try<Integer>>() {});
            assertThat(deserialized).isSuccess().containsValue(99);
        }

        @Test
        void roundTripFailure() throws Exception {
            var original = Try.failure(new RuntimeException("error message"));
            var json = mapper.writeValueAsString(original);
            var deserialized = mapper.readValue(json, new TypeReference<Try<Integer>>() {});
            assertThat(deserialized).isFailure();
            assertThat(deserialized.getCause().getMessage()).isEqualTo("error message");
        }

        @Test
        void roundTripFailure_nullMessage() throws Exception {
            var original = Try.failure(new RuntimeException());
            var json = mapper.writeValueAsString(original);
            assertThat(json).isEqualTo("{\"error\":null}");
            var deserialized = mapper.readValue(json, new TypeReference<Try<Integer>>() {});
            assertThat(deserialized).isFailure();
            assertThat(deserialized.getCause().getMessage()).isNull();
        }

        @Test
        void deserializeSuccessObject_coversReadTreeAsValuePath() throws Exception {
            // When the JSON is a START_OBJECT with no "error" key and valueType is known,
            // TryDeserializer calls ctxt.readTreeAsValue(node, valueType)
            var json = "{\"name\":\"alice\"}";
            var result = mapper.readValue(json,
                new TypeReference<Try<Map<String, Object>>>() {});
            assertThat(result).isSuccess();
            assertThat(result.get().get("name")).isEqualTo("alice");
        }

        @Test
        @SuppressWarnings("unchecked")
        void deserializeSuccessObject_rawType_coversNodeReturnPath() throws Exception {
            // START_OBJECT, no "error" key, valueType == null → returns Try.success(node)
            var result = mapper.readValue("{\"key\":\"v\"}", Try.class);
            assertThat(result).isSuccess();
            assertInstanceOf(JsonNode.class, result.get());
            assertThat(((JsonNode) result.get()).get("key").asString()).isEqualTo("v");
        }

        @Test
        @SuppressWarnings("unchecked")
        void deserializeSuccess_rawType_coversPrimitiveNullTypePath() throws Exception {
            // Non-object token, valueType == null → p.readValueAs(Object.class)
            var result = mapper.readValue("42", Try.class);
            assertThat(result).isSuccess();
            assertThat(result.get()).isEqualTo(42);
        }
    }

    // -------------------------------------------------------------------------
    // Validated
    // -------------------------------------------------------------------------

    @Nested
    class ValidatedTests {

        @Test
        void serializeValid() throws Exception {
            var valid = Validated.valid(42);
            var json = mapper.writeValueAsString(valid);
            assertThat(json).isEqualTo("{\"valid\":42}");
        }

        @Test
        void serializeInvalid() throws Exception {
            var invalid = Validated.invalid("error");
            var json = mapper.writeValueAsString(invalid);
            assertThat(json).isEqualTo("{\"invalid\":\"error\"}");
        }

        @Test
        void roundTripValid() throws Exception {
            var original = Validated.valid(7);
            var json = mapper.writeValueAsString(original);
            var deserialized = mapper.readValue(json, new TypeReference<Validated<String, Integer>>() {});
            assertThat(deserialized).isValid().containsValue(7);
        }

        @Test
        void roundTripInvalid() throws Exception {
            var original = Validated.invalid("bad input");
            var json = mapper.writeValueAsString(original);
            var deserialized = mapper.readValue(json, new TypeReference<Validated<String, Integer>>() {});
            assertThat(deserialized).isInvalid().hasError("bad input");
        }

        @Test
        @SuppressWarnings("unchecked")
        void deserializeValid_rawType_coversNullValueTypePath() throws Exception {
            var result = mapper.readValue("{\"valid\":7}", Validated.class);
            assertThat(result).isValid();
            assertThat(result.get()).isEqualTo(7);
        }

        @Test
        @SuppressWarnings("unchecked")
        void deserializeInvalid_rawType_coversNullErrorTypePath() throws Exception {
            var result = mapper.readValue("{\"invalid\":\"e\"}", Validated.class);
            assertThat(result).isInvalid();
            assertThat(result.getError()).isEqualTo("e");
        }

        @Test
        void deserializeUnknownField_throws() {
            var ex = assertThrows(DatabindException.class, () ->
                mapper.readValue("{\"unknown\":42}", new TypeReference<Validated<String, Integer>>() {}));
            assertThat(ex.getMessage()).contains("unknown");
        }

        @Test
        void deserializeFromNonObject_throws() {
            assertThrows(DatabindException.class, () ->
                mapper.readValue("42", new TypeReference<Validated<String, Integer>>() {}));
        }

        @Test
        void deserializeEmptyObject_throws() {
            assertThrows(DatabindException.class, () ->
                mapper.readValue("{}", new TypeReference<Validated<String, Integer>>() {}));
        }

        @Test
        void deserializeExtraField_throws() {
            var ex = assertThrows(DatabindException.class, () ->
                mapper.readValue("{\"valid\":1,\"extra\":2}", new TypeReference<Validated<String, Integer>>() {}));
            assertThat(ex.getMessage()).contains("extra");
        }
    }

    // -------------------------------------------------------------------------
    // Either
    // -------------------------------------------------------------------------

    @Nested
    class EitherTests {

        @Test
        void serializeRight() throws Exception {
            var right = Either.right(1);
            var json = mapper.writeValueAsString(right);
            assertThat(json).isEqualTo("{\"right\":1}");
        }

        @Test
        void serializeLeft() throws Exception {
            var left = Either.left("oops");
            var json = mapper.writeValueAsString(left);
            assertThat(json).isEqualTo("{\"left\":\"oops\"}");
        }

        @Test
        void roundTripRight() throws Exception {
            var original = Either.right(5);
            var json = mapper.writeValueAsString(original);
            var deserialized = mapper.readValue(json, new TypeReference<Either<String, Integer>>() {});
            assertThat(deserialized).isRight().containsRight(5);
        }

        @Test
        void roundTripLeft() throws Exception {
            var original = Either.left("left value");
            var json = mapper.writeValueAsString(original);
            var deserialized = mapper.readValue(json, new TypeReference<Either<String, Integer>>() {});
            assertThat(deserialized).isLeft().containsLeft("left value");
        }

        @Test
        @SuppressWarnings("unchecked")
        void deserializeRight_rawType_coversNullRightTypePath() throws Exception {
            var result = (Either<Object, Object>) mapper.readValue("{\"right\":5}", Either.class);
            assertThat(result).isRight().containsRight(5);
        }

        @Test
        @SuppressWarnings("unchecked")
        void deserializeLeft_rawType_coversNullLeftTypePath() throws Exception {
            var result = (Either<Object, Object>) mapper.readValue("{\"left\":\"e\"}", Either.class);
            assertThat(result).isLeft().containsLeft("e");
        }

        @Test
        void deserializeUnknownField_throws() {
            var ex = assertThrows(DatabindException.class, () ->
                mapper.readValue("{\"unknown\":42}", new TypeReference<Either<String, Integer>>() {}));
            assertThat(ex.getMessage()).contains("unknown");
        }

        @Test
        void deserializeFromNonObject_throws() {
            assertThrows(DatabindException.class, () ->
                mapper.readValue("42", new TypeReference<Either<String, Integer>>() {}));
        }

        @Test
        void deserializeEmptyObject_throws() {
            assertThrows(DatabindException.class, () ->
                mapper.readValue("{}", new TypeReference<Either<String, Integer>>() {}));
        }

        @Test
        void deserializeExtraField_throws() {
            var ex = assertThrows(DatabindException.class, () ->
                mapper.readValue("{\"right\":1,\"extra\":2}", new TypeReference<Either<String, Integer>>() {}));
            assertThat(ex.getMessage()).contains("extra");
        }
    }

    // -------------------------------------------------------------------------
    // Tuple2
    // -------------------------------------------------------------------------

    @Nested
    class Tuple2Tests {

        @Test
        void serialize() throws Exception {
            var t = Tuple2.of("a", 1);
            var json = mapper.writeValueAsString(t);
            assertThat(json).isEqualTo("[\"a\",1]");
        }

        @Test
        void roundTrip() throws Exception {
            var original = Tuple2.of("hello", 42);
            var json = mapper.writeValueAsString(original);
            var deserialized = mapper.readValue(json, new TypeReference<Tuple2<String, Integer>>() {});
            assertThat(deserialized._1()).isEqualTo("hello");
            assertThat(deserialized._2()).isEqualTo(42);
        }

        @Test
        @SuppressWarnings("unchecked")
        void deserializeRawType_coversNullTypePaths() throws Exception {
            var result = mapper.readValue("[\"a\",1]", Tuple2.class);
            assertThat(result._1()).isEqualTo("a");
            assertThat(result._2()).isEqualTo(1);
        }

        @Test
        void deserializeFromNonArray_throws() {
            assertThrows(DatabindException.class, () ->
                mapper.readValue("{}", new TypeReference<Tuple2<String, Integer>>() {}));
        }

        @Test
        void deserializeTooFewElements_zero_throws() {
            assertThrows(InvalidFormatException.class, () ->
                mapper.readValue("[]", new TypeReference<Tuple2<String, Integer>>() {}));
        }

        @Test
        void deserializeTooFewElements_one_throws() {
            assertThrows(InvalidFormatException.class, () ->
                mapper.readValue("[1]", new TypeReference<Tuple2<String, Integer>>() {}));
        }

        @Test
        void deserializeTooManyElements_throws() {
            assertThrows(InvalidFormatException.class, () ->
                mapper.readValue("[1,2,3]", new TypeReference<Tuple2<String, Integer>>() {}));
        }

        @Test
        void deserializeNullElement_throws() {
            assertThrows(MismatchedInputException.class, () ->
                mapper.readValue("[null,1]", new TypeReference<Tuple2<String, Integer>>() {}));
        }
    }

    // -------------------------------------------------------------------------
    // Tuple3
    // -------------------------------------------------------------------------

    @Nested
    class Tuple3Tests {

        @Test
        void serialize() throws Exception {
            var t = Tuple3.of("a", 1, true);
            var json = mapper.writeValueAsString(t);
            assertThat(json).isEqualTo("[\"a\",1,true]");
        }

        @Test
        void roundTrip() throws Exception {
            var original = Tuple3.of("x", 10, false);
            var json = mapper.writeValueAsString(original);
            var deserialized = mapper.readValue(json, new TypeReference<Tuple3<String, Integer, Boolean>>() {});
            assertThat(deserialized._1()).isEqualTo("x");
            assertThat(deserialized._2()).isEqualTo(10);
            assertThat(deserialized._3()).isEqualTo(false);
        }

        @Test
        @SuppressWarnings("unchecked")
        void deserializeRawType_coversNullTypePaths() throws Exception {
            var result = mapper.readValue("[\"a\",1,true]", Tuple3.class);
            assertThat(result._1()).isEqualTo("a");
            assertThat(result._2()).isEqualTo(1);
            assertThat(result._3()).isEqualTo(true);
        }

        @Test
        void deserializeFromNonArray_throws() {
            assertThrows(DatabindException.class, () ->
                mapper.readValue("{}", new TypeReference<Tuple3<String, Integer, Boolean>>() {}));
        }

        @Test
        void deserializeTooFewElements_zero_throws() {
            assertThrows(InvalidFormatException.class, () ->
                mapper.readValue("[]", new TypeReference<Tuple3<String, Integer, Boolean>>() {}));
        }

        @Test
        void deserializeTooFewElements_one_throws() {
            assertThrows(InvalidFormatException.class, () ->
                mapper.readValue("[1]", new TypeReference<Tuple3<String, Integer, Boolean>>() {}));
        }

        @Test
        void deserializeTooFewElements_two_throws() {
            assertThrows(InvalidFormatException.class, () ->
                mapper.readValue("[1,2]", new TypeReference<Tuple3<String, Integer, Boolean>>() {}));
        }

        @Test
        void deserializeTooManyElements_throws() {
            assertThrows(InvalidFormatException.class, () ->
                mapper.readValue("[1,2,3,4]", new TypeReference<Tuple3<String, Integer, Boolean>>() {}));
        }
    }

    // -------------------------------------------------------------------------
    // Tuple4
    // -------------------------------------------------------------------------

    @Nested
    class Tuple4Tests {

        @Test
        void serialize() throws Exception {
            var t = Tuple4.of("a", 1, true, 3.14);
            var json = mapper.writeValueAsString(t);
            assertThat(json).isEqualTo("[\"a\",1,true,3.14]");
        }

        @Test
        void roundTrip() throws Exception {
            var original = Tuple4.of("z", 7, true, 2.71);
            var json = mapper.writeValueAsString(original);
            var deserialized = mapper.readValue(json, new TypeReference<Tuple4<String, Integer, Boolean, Double>>() {});
            assertThat(deserialized._1()).isEqualTo("z");
            assertThat(deserialized._2()).isEqualTo(7);
            assertThat(deserialized._3()).isEqualTo(true);
            assertThat(deserialized._4()).isEqualTo(2.71);
        }

        @Test
        @SuppressWarnings("unchecked")
        void deserializeRawType_coversNullTypePaths() throws Exception {
            var result = mapper.readValue("[\"a\",1,true,3.14]", Tuple4.class);
            assertThat(result._1()).isEqualTo("a");
            assertThat(result._2()).isEqualTo(1);
            assertThat(result._3()).isEqualTo(true);
            assertThat((double) result._4()).isEqualTo(3.14);
        }

        @Test
        void deserializeFromNonArray_throws() {
            assertThrows(DatabindException.class, () ->
                mapper.readValue("{}", new TypeReference<Tuple4<String, Integer, Boolean, Double>>() {}));
        }

        @Test
        void deserializeTooFewElements_zero_throws() {
            assertThrows(InvalidFormatException.class, () ->
                mapper.readValue("[]", new TypeReference<Tuple4<String, Integer, Boolean, Double>>() {}));
        }

        @Test
        void deserializeTooFewElements_one_throws() {
            assertThrows(InvalidFormatException.class, () ->
                mapper.readValue("[1]", new TypeReference<Tuple4<String, Integer, Boolean, Double>>() {}));
        }

        @Test
        void deserializeTooFewElements_two_throws() {
            assertThrows(InvalidFormatException.class, () ->
                mapper.readValue("[1,2]", new TypeReference<Tuple4<String, Integer, Boolean, Double>>() {}));
        }

        @Test
        void deserializeTooFewElements_three_throws() {
            assertThrows(InvalidFormatException.class, () ->
                mapper.readValue("[1,2,3]", new TypeReference<Tuple4<String, Integer, Boolean, Double>>() {}));
        }

        @Test
        void deserializeTooManyElements_throws() {
            assertThrows(InvalidFormatException.class, () ->
                mapper.readValue("[1,2,3,4,5]", new TypeReference<Tuple4<String, Integer, Boolean, Double>>() {}));
        }
    }

    // -------------------------------------------------------------------------
    // NonEmptyList
    // -------------------------------------------------------------------------

    @Nested
    class NonEmptyListTests {

        @Test
        void serializeSingleElement() throws Exception {
            var nel = NonEmptyList.of(1, List.of());
            var json = mapper.writeValueAsString(nel);
            assertThat(json).isEqualTo("[1]");
        }

        @Test
        void serializeMultipleElements() throws Exception {
            var nel = NonEmptyList.of(1, List.of(2, 3));
            var json = mapper.writeValueAsString(nel);
            assertThat(json).isEqualTo("[1,2,3]");
        }

        @Test
        void roundTrip() throws Exception {
            var original = NonEmptyList.of(1, List.of(2, 3));
            var json = mapper.writeValueAsString(original);
            var deserialized = mapper.readValue(json, new TypeReference<NonEmptyList<Integer>>() {});
            assertThat(deserialized.head()).isEqualTo(1);
            assertThat(deserialized.tail()).isEqualTo(List.of(2, 3));
        }

        @Test
        void emptyArrayThrows() {
            assertThrows(InvalidFormatException.class, () ->
                mapper.readValue("[]", new TypeReference<NonEmptyList<Integer>>() {}));
        }

        @Test
        @SuppressWarnings("unchecked")
        void deserializeRawType_coversNullElementTypePath() throws Exception {
            var result = mapper.readValue("[1,2,3]", NonEmptyList.class);
            assertThat(result.head()).isEqualTo(1);
            assertThat(result.tail()).isEqualTo(List.of(2, 3));
        }

        @Test
        void deserializeFromNonArray_throws() {
            assertThrows(DatabindException.class, () ->
                mapper.readValue("42", new TypeReference<NonEmptyList<Integer>>() {}));
        }

        @Test
        void nullElementThrows() {
            var ex = assertThrows(MismatchedInputException.class, () ->
                mapper.readValue("[null]", new TypeReference<NonEmptyList<Integer>>() {}));
            assertThat(ex.getMessage()).contains("null element");
        }

        @Test
        void declaredObjectElements_carryTypeIdsUnderDefaultTyping() throws Exception {
            var typed = nonFinalTypingMapper();
            var json = typed.writeValueAsString(new ObjectList(NonEmptyList.of(new Item("x"), List.of())));
            assertThat(json).isEqualTo("{\"items\":[[\"" + Item.class.getName() + "\",{\"name\":\"x\"}]]}");
            var back = typed.readValue(json, ObjectList.class);
            assertThat(back.items().head()).isEqualTo(new Item("x"));
        }

        @Test
        void propertyFormat_appliesToElements() throws Exception {
            var json = mapper.writeValueAsString(new DatedList(NonEmptyList.of(JAN_2, List.of())));
            assertThat(json).isEqualTo("{\"dates\":[\"2024-01-02\"]}");
            assertThat(mapper.readValue(json, DatedList.class).dates().head()).isEqualTo(JAN_2);
        }

        @Test
        void inObjectSlot_carriesItsOwnTypeIdAndRoundTrips() throws Exception {
            var typed = nonFinalTypingMapper();
            var holder = new ObjectValues(NonEmptyMap.singleton("l", NonEmptyList.of(1, List.of(2))), NonEmptySet.of(1, Set.of()));
            var json = typed.writeValueAsString(holder);
            assertThat(json).contains("\"values\":{\"l\":[\"dmx.fun.NonEmptyList\",[1,2]]}");
            var back = typed.readValue(json, ObjectValues.class);
            assertThat(((NonEmptyList<?>) back.values().headValue()).toList()).isEqualTo(List.of(1, 2));
        }
    }


    // -------------------------------------------------------------------------
    // Default-typing fixtures shared by NonEmptySet and NonEmptyMap
    // -------------------------------------------------------------------------

    record Item(String name) {}

    record ObjectValues(NonEmptyMap<String, Object> values, NonEmptySet<Object> elements) {}

    record ItemValues(NonEmptyMap<String, Item> values, NonEmptySet<Item> elements) {}

    record ListValues(NonEmptyMap<String, List<Item>> values, NonEmptySet<List<Item>> elements) {}

    record SetsByKey(Map<String, NonEmptySet<Object>> byKey) {}

    interface Key {}

    record Id(String value) implements Key {}

    record KeyedValues(NonEmptyMap<Key, Integer> keyed, Map<Key, Integer> plain) {}

    /** Serializes any {@link Key} as {@code key:<class simple name>}, to tell declared-type lookup apart. */
    static final class KeySerializer extends tools.jackson.databind.ser.std.StdSerializer<Key> {
        KeySerializer() { super(Key.class); }
        @Override
        public void serialize(Key value, tools.jackson.core.JsonGenerator gen,
                              tools.jackson.databind.SerializationContext ctxt) {
            gen.writeName("key:" + value.getClass().getSimpleName());
        }
    }

    enum Color { RED }

    record Dated(
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd", timezone = "UTC") NonEmptySet<Date> dates,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd", timezone = "UTC") NonEmptyMap<String, Date> byName) {}

    record Typed(@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS) Object value) {}

    static class Animal { public String name; }

    static class Dog extends Animal { public String breed; }

    record Zoo(Set<Animal> plain, NonEmptySet<Animal> set, NonEmptyMap<String, Animal> map) {}

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
    @JsonSubTypes(@JsonSubTypes.Type(value = Pet.class, name = "pet"))
    static class TypedAnimal { public String name; }

    static class Pet extends TypedAnimal { public String breed; }

    record TypedZoo(Set<TypedAnimal> plain, NonEmptySet<TypedAnimal> set, NonEmptyMap<String, TypedAnimal> map) {}

    private static Pet pet() {
        var pet = new Pet();
        pet.name = "rex";
        pet.breed = "collie";
        return pet;
    }

    private static Dog dog() {
        var dog = new Dog();
        dog.name = "rex";
        dog.breed = "collie";
        return dog;
    }

    record ObjectList(NonEmptyList<Object> items) {}

    record DatedList(
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd", timezone = "UTC") NonEmptyList<Date> dates) {}

    record DynamicZoo(
        @JsonSerialize(typing = JsonSerialize.Typing.DYNAMIC) NonEmptySet<Animal> set,
        @JsonSerialize(typing = JsonSerialize.Typing.DYNAMIC) NonEmptyMap<String, Animal> map,
        @JsonSerialize(typing = JsonSerialize.Typing.DYNAMIC) NonEmptyList<Animal> list) {}

    record Named(@JsonTypeInfo(use = JsonTypeInfo.Id.NAME) Object value) {}

    record Deduced(@JsonTypeInfo(use = JsonTypeInfo.Id.DEDUCTION) Object value) {}

    record ObjectPayloads(Option<Object> option, Try<Object> attempt, Result<Object, String> result,
                          Validated<String, Object> validated, Either<String, Object> either,
                          Tuple2<Object, Integer> t2, Tuple3<Object, Integer, Integer> t3,
                          Tuple4<Object, Integer, Integer, Integer> t4) {}

    record SameTypeNesting(Option<Option<Item>> option, Tuple2<Tuple2<String, Integer>, String> tuple,
                  Result<Result<String, Integer>, String> result, Either<Either<String, Integer>, String> either,
                  NonEmptyList<NonEmptyList<Integer>> list) {}

    record ItemPayloads(Option<Item> option, Try<Item> attempt, Result<Item, String> result,
                        Validated<String, Item> validated, Either<String, Item> either,
                        Tuple2<Item, Integer> t2, Tuple3<Item, Integer, Integer> t3,
                        Tuple4<Item, Integer, Integer, Integer> t4) {}

    record AnnotatedZoo(@JsonSerialize NonEmptySet<Animal> set) {}

    record StaticZoo(
        @JsonSerialize(typing = JsonSerialize.Typing.STATIC) NonEmptySet<Animal> set,
        @JsonSerialize(typing = JsonSerialize.Typing.STATIC) NonEmptyMap<String, Animal> map,
        @JsonSerialize(typing = JsonSerialize.Typing.STATIC) NonEmptyList<Animal> list) {}

    private static final Date JAN_2 = Date.from(LocalDate.of(2024, 1, 2).atStartOfDay(ZoneOffset.UTC).toInstant());

    /** Jackson 3's default validator refuses {@code @JsonTypeInfo} on an {@code Object} base type. */
    private static ObjectMapper permissiveTypeIdMapper() {
        return JsonMapper.builder().addModule(new DmxFunModule())
            .polymorphicTypeValidator(BasicPolymorphicTypeValidator.builder().allowIfBaseType(Object.class).build())
            .build();
    }

    private static ObjectMapper nonFinalTypingMapper() {
        return JsonMapper.builder().addModule(new DmxFunModule())
            .activateDefaultTyping(
                BasicPolymorphicTypeValidator.builder().allowIfBaseType(Object.class).build(),
                DefaultTyping.NON_FINAL)
            .build();
    }

    // -------------------------------------------------------------------------
    // NonEmptySet
    // -------------------------------------------------------------------------

    @Nested
    class NonEmptySetTests {

        @Test
        void serializeHeadFirst() throws Exception {
            var nes = NonEmptySet.of(1, Set.of(2));
            var json = mapper.writeValueAsString(nes);
            assertThat(json).isEqualTo("[1,2]");
        }

        @Test
        void roundTrip() throws Exception {
            var original = NonEmptySet.of("a", Set.of("b"));
            var json = mapper.writeValueAsString(original);
            var deserialized = mapper.readValue(json, new TypeReference<NonEmptySet<String>>() {});
            assertThat(deserialized.head()).isEqualTo("a");
            assertThat(deserialized.toSet()).isEqualTo(Set.of("a", "b"));
        }

        @Test
        void deserializeDropsDuplicates() throws Exception {
            var deserialized = mapper.readValue("[1,2,1]", new TypeReference<NonEmptySet<Integer>>() {});
            assertThat(deserialized.head()).isEqualTo(1);
            assertThat(deserialized.size()).isEqualTo(2);
        }

        @Test
        void emptyArrayThrows() {
            var ex = assertThrows(InvalidFormatException.class, () ->
                mapper.readValue("[]", new TypeReference<NonEmptySet<Integer>>() {}));
            assertThat(ex.getMessage()).contains("NonEmptySet");
        }

        @Test
        @SuppressWarnings("unchecked")
        void deserializeRawType_coversNullElementTypePath() throws Exception {
            var result = mapper.readValue("[1,2]", NonEmptySet.class);
            assertThat(result.head()).isEqualTo(1);
            assertThat(result.toSet()).isEqualTo(Set.of(1, 2));
        }

        @Test
        void deserializeFromNonArray_throws() {
            assertThrows(DatabindException.class, () ->
                mapper.readValue("42", new TypeReference<NonEmptySet<Integer>>() {}));
        }

        @Test
        void declaredObjectElements_carryTypeIdsUnderDefaultTyping() throws Exception {
            var typed = nonFinalTypingMapper();
            var holder = new ObjectValues(NonEmptyMap.singleton("k", 1), NonEmptySet.of(new Item("x"), Set.of()));
            var json = typed.writeValueAsString(holder);
            assertThat(json).contains("\"elements\":[[\"" + Item.class.getName() + "\",{\"name\":\"x\"}]]");
            var back = typed.readValue(json, ObjectValues.class);
            assertThat(back.elements().head()).isEqualTo(new Item("x"));
        }

        @Test
        void inObjectSlot_carriesItsOwnTypeIdAndRoundTrips() throws Exception {
            var typed = nonFinalTypingMapper();
            var holder = new ObjectValues(NonEmptyMap.singleton("k", 1), NonEmptySet.of(NonEmptyMap.singleton("a", 1), Set.of()));
            var json = typed.writeValueAsString(holder);
            assertThat(json).contains("\"elements\":[[\"dmx.fun.NonEmptyMap\",{\"a\":1}]]");
            var back = typed.readValue(json, ObjectValues.class);
            assertThat(back.elements().head()).isInstanceOf(NonEmptyMap.class);
            assertThat(((NonEmptyMap<?, ?>) back.elements().head()).headValue()).isEqualTo(1);
        }

        @Test
        void asPropertyTypeId_roundTrips() throws Exception {
            var typed = permissiveTypeIdMapper();
            var json = typed.writeValueAsString(new Typed(NonEmptySet.singleton(1)));
            assertThat(json).isEqualTo("{\"value\":[\"dmx.fun.NonEmptySet\",[1]]}");
            var back = typed.readValue(json, Typed.class);
            assertThat(((NonEmptySet<?>) back.value()).head()).isEqualTo(1);
        }

        @Test
        void staticTyping_usesTheDeclaredElementSerializer() throws Exception {
            var zoo = new Zoo(Set.of(dog()), NonEmptySet.singleton(dog()), NonEmptyMap.singleton("a", dog()));
            assertThat(mapper.writeValueAsString(zoo))
                .isEqualTo("{\"plain\":[{\"breed\":\"collie\",\"name\":\"rex\"}],\"set\":[{\"breed\":\"collie\",\"name\":\"rex\"}],"
                    + "\"map\":{\"a\":{\"breed\":\"collie\",\"name\":\"rex\"}}}");
            var staticMapper = JsonMapper.builder().addModule(new DmxFunModule()).enable(MapperFeature.USE_STATIC_TYPING).build();
            assertThat(staticMapper.writeValueAsString(zoo))
                .isEqualTo("{\"plain\":[{\"name\":\"rex\"}],\"set\":[{\"name\":\"rex\"}],\"map\":{\"a\":{\"name\":\"rex\"}}}");
        }

        @Test
        void staticTyping_staysDynamicWhenTheElementTypeCarriesTypeIds() throws Exception {
            var staticMapper = JsonMapper.builder().addModule(new DmxFunModule()).enable(MapperFeature.USE_STATIC_TYPING).build();
            var json = staticMapper.writeValueAsString(new TypedZoo(Set.of(pet()), NonEmptySet.singleton(pet()), NonEmptyMap.singleton("a", pet())));
            var plain = json.substring(json.indexOf("[") + 1, json.indexOf("]"));
            assertThat(plain).contains("\"breed\":\"collie\"").contains("\"@type\":\"pet\"");
            assertThat(json).isEqualTo("{\"plain\":[" + plain + "],\"set\":[" + plain + "],\"map\":{\"a\":" + plain + "}}");
        }

        @Test
        void propertyFormat_appliesToElements() throws Exception {
            var holder = new Dated(NonEmptySet.singleton(JAN_2), NonEmptyMap.singleton("a", JAN_2));
            var json = mapper.writeValueAsString(holder);
            assertThat(json).isEqualTo("{\"dates\":[\"2024-01-02\"],\"byName\":{\"a\":\"2024-01-02\"}}");
            var back = mapper.readValue(json, Dated.class);
            assertThat(back.dates().head()).isEqualTo(JAN_2);
            assertThat(back.byName().headValue()).isEqualTo(JAN_2);
        }

        @Test
        void nullElementThrows() {
            var ex = assertThrows(MismatchedInputException.class, () ->
                mapper.readValue("[null]", new TypeReference<NonEmptySet<Integer>>() {}));
            assertThat(ex.getMessage()).contains("null element");
        }
    }

    // -------------------------------------------------------------------------
    // NonEmptyMap
    // -------------------------------------------------------------------------

    @Nested
    class NonEmptyMapTests {

        @Test
        void serializeAsObjectHeadFirst() throws Exception {
            var nem = NonEmptyMap.of("a", 1, Map.of("b", 2));
            var json = mapper.writeValueAsString(nem);
            assertThat(json).isEqualTo("{\"a\":1,\"b\":2}");
        }

        @Test
        void roundTrip() throws Exception {
            var original = NonEmptyMap.of("a", 1, Map.of("b", 2));
            var json = mapper.writeValueAsString(original);
            var deserialized = mapper.readValue(json, new TypeReference<NonEmptyMap<String, Integer>>() {});
            assertThat(deserialized.headKey()).isEqualTo("a");
            assertThat(deserialized.headValue()).isEqualTo(1);
            assertThat(deserialized.toMap()).isEqualTo(Map.of("a", 1, "b", 2));
        }

        @Test
        void deserializeConvertsKeysAndValues() throws Exception {
            var deserialized = mapper.readValue("{\"1\":\"x\",\"2\":\"y\"}",
                new TypeReference<NonEmptyMap<Integer, String>>() {});
            assertThat(deserialized.headKey()).isEqualTo(1);
            assertThat(deserialized.toMap()).isEqualTo(Map.of(1, "x", 2, "y"));
        }

        @Test
        void emptyObjectThrows() {
            var ex = assertThrows(InvalidFormatException.class, () ->
                mapper.readValue("{}", new TypeReference<NonEmptyMap<String, Integer>>() {}));
            assertThat(ex.getMessage()).contains("NonEmptyMap");
        }

        @Test
        @SuppressWarnings("unchecked")
        void deserializeRawType_coversNullTypesPath() throws Exception {
            var result = mapper.readValue("{\"a\":1}", NonEmptyMap.class);
            assertThat(result.headKey()).isEqualTo("a");
            assertThat(result.headValue()).isEqualTo(1);
        }

        @Test
        void deserializeFromNonObject_throws() {
            assertThrows(DatabindException.class, () ->
                mapper.readValue("[1]", new TypeReference<NonEmptyMap<String, Integer>>() {}));
        }

        @Test
        void inObjectSlot_carriesItsOwnTypeIdAndRoundTrips() throws Exception {
            var typed = nonFinalTypingMapper();
            var holder = new ObjectValues(NonEmptyMap.singleton("s", NonEmptySet.singleton(1)), NonEmptySet.of(1, Set.of()));
            var json = typed.writeValueAsString(holder);
            assertThat(json).contains("\"values\":{\"s\":[\"dmx.fun.NonEmptySet\",[1]]}");
            var back = typed.readValue(json, ObjectValues.class);
            assertThat(back.values().headValue()).isInstanceOf(NonEmptySet.class);
            assertThat(((NonEmptySet<?>) back.values().headValue()).head()).isEqualTo(1);
        }

        @Test
        void asPropertyTypeId_roundTrips() throws Exception {
            var typed = permissiveTypeIdMapper();
            var json = typed.writeValueAsString(new Typed(NonEmptyMap.singleton("a", 1)));
            assertThat(json).isEqualTo("{\"value\":{\"@class\":\"dmx.fun.NonEmptyMap\",\"a\":1}}");
            var back = typed.readValue(json, Typed.class);
            assertThat(((NonEmptyMap<?, ?>) back.value()).headValue()).isEqualTo(1);
            assertThrows(InvalidFormatException.class, () ->
                typed.readValue("{\"value\":{\"@class\":\"dmx.fun.NonEmptyMap\"}}", Typed.class));
        }

        @Test
        void nullValueThrows() {
            var ex = assertThrows(MismatchedInputException.class, () ->
                mapper.readValue("{\"a\":null}", new TypeReference<NonEmptyMap<String, Integer>>() {}));
            assertThat(ex.getMessage()).contains("null value for key 'a'");
        }

        @Test
        void unknownEnumKeyReadAsNull_throws() throws Exception {
            var lenient = JsonMapper.builder().addModule(new DmxFunModule())
                .enable(EnumFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL)
                .build();
            var ex = assertThrows(MismatchedInputException.class, () ->
                lenient.readValue("{\"BLUE\":1}", new TypeReference<NonEmptyMap<Color, Integer>>() {}));
            assertThat(ex.getMessage()).contains("null key for 'BLUE'");
            var known = lenient.readValue("{\"RED\":1}", new TypeReference<NonEmptyMap<Color, Integer>>() {});
            assertThat(known.toMap()).isEqualTo(Map.of(Color.RED, 1));
        }

        @Test
        void declaredObjectValues_carryTypeIdsUnderDefaultTyping() throws Exception {
            var typed = nonFinalTypingMapper();
            var holder = new ObjectValues(NonEmptyMap.singleton("a", new Item("x")), NonEmptySet.of(1, Set.of()));
            var json = typed.writeValueAsString(holder);
            assertThat(json).contains("\"values\":{\"a\":[\"" + Item.class.getName() + "\",{\"name\":\"x\"}]}");
            var back = typed.readValue(json, ObjectValues.class);
            assertThat(back.values().headValue()).isEqualTo(new Item("x"));
        }

        @Test
        void declaredFinalValues_stayPlainUnderNonFinalTyping() throws Exception {
            var typed = nonFinalTypingMapper();
            var holder = new ItemValues(NonEmptyMap.singleton("a", new Item("x")), NonEmptySet.of(new Item("y"), Set.of()));
            var json = typed.writeValueAsString(holder);
            assertThat(json).isEqualTo("{\"values\":{\"a\":{\"name\":\"x\"}},\"elements\":[{\"name\":\"y\"}]}");
            var back = typed.readValue(json, ItemValues.class);
            assertThat(back.values().headValue()).isEqualTo(new Item("x"));
            assertThat(back.elements().head()).isEqualTo(new Item("y"));
        }

        @Test
        void nestedGenericValues_keepTheirBindingsUnderNonFinalTyping() throws Exception {
            var typed = nonFinalTypingMapper();
            var holder = new ListValues(
                NonEmptyMap.singleton("a", List.of(new Item("x"))),
                NonEmptySet.of(List.of(new Item("y")), Set.of()));
            var json = typed.writeValueAsString(holder);
            assertThat(json).doesNotContain(Item.class.getName());
            var back = typed.readValue(json, ListValues.class);
            assertThat(back.values().headValue()).isEqualTo(List.of(new Item("x")));
            assertThat(back.elements().head()).isEqualTo(List.of(new Item("y")));
        }

        @Test
        void nestedInPlainMap_usesItsOwnElementType() throws Exception {
            var typed = nonFinalTypingMapper();
            var holder = new SetsByKey(Map.of("k", NonEmptySet.of(new Item("q"), Set.of())));
            var json = typed.writeValueAsString(holder);
            assertThat(json).contains("[\"" + Item.class.getName() + "\",{\"name\":\"q\"}]");
            var back = typed.readValue(json, SetsByKey.class);
            assertThat(back.byKey().get("k").head()).isEqualTo(new Item("q"));
        }

        @Test
        void keys_useTheKeySerializerOfTheDeclaredKeyType() throws Exception {
            var custom = new tools.jackson.databind.module.SimpleModule("keys").addKeySerializer(Key.class, new KeySerializer());
            var withKeys = JsonMapper.builder().addModule(new DmxFunModule()).addModule(custom).build();
            var holder = new KeyedValues(NonEmptyMap.singleton(new Id("a"), 1), Map.of(new Id("a"), 1));
            assertThat(withKeys.writeValueAsString(holder))
                .isEqualTo("{\"keyed\":{\"key:Id\":1},\"plain\":{\"key:Id\":1}}");
        }

        @Test
        void callerProvidedSerializer_takesPrecedence() throws Exception {
            var module = new DmxFunModule();
            module.addSerializer(NonEmptyMap.class, new tools.jackson.databind.ser.std.StdSerializer<>(NonEmptyMap.class) {
                @Override
                public void serialize(NonEmptyMap value, tools.jackson.core.JsonGenerator gen,
                                      tools.jackson.databind.SerializationContext ctxt) {
                    gen.writeString("custom");
                }
            });
            var custom = JsonMapper.builder().addModule(module).build();
            assertThat(custom.writeValueAsString(NonEmptyMap.singleton("a", 1))).isEqualTo("\"custom\"");
            assertThat(custom.writeValueAsString(NonEmptySet.singleton(1))).isEqualTo("[1]");
        }

        @Test
        void mapValue_isSerializedThroughAContextualizedSerializer() throws Exception {
            var typed = nonFinalTypingMapper();
            var json = typed.writeValueAsString(NonEmptyMap.singleton("a", Map.of("x", 1)));
            assertThat(json).isEqualTo("{\"a\":[\"java.util.ImmutableCollections$Map1\",{\"x\":1}]}");
            assertThat(mapper.writeValueAsString(NonEmptyMap.singleton("a", Map.of("x", 1))))
                .isEqualTo("{\"a\":{\"x\":1}}");
        }

        @Test
        void serializeWithDefaultTyping_staysAPlainObject() throws Exception {
            var typed = nonFinalTypingMapper();
            var json = typed.writeValueAsString(NonEmptyMap.of("a", 1, Map.of()));
            assertThat(json).isEqualTo("{\"a\":1}");
            var back = typed.readValue(json, new TypeReference<NonEmptyMap<String, Integer>>() {});
            assertThat(back.toMap()).isEqualTo(Map.of("a", 1));
        }

        @Test
        void nestedInResult_roundTrips() throws Exception {
            Result<NonEmptyMap<String, Integer>, String> original = Result.ok(NonEmptyMap.singleton("a", 1));
            var json = mapper.writeValueAsString(original);
            assertThat(json).isEqualTo("{\"ok\":{\"a\":1}}");
            var back = mapper.readValue(json, new TypeReference<Result<NonEmptyMap<String, Integer>, String>>() {});
            assertThat(back.get().toMap()).isEqualTo(Map.of("a", 1));
        }
    }

    // -------------------------------------------------------------------------
    // Type ids on the dmx-fun values themselves (@JsonTypeInfo on an Object slot)
    // -------------------------------------------------------------------------

    @Nested
    class TypeIdTests {

        private String write(Object value) throws Exception {
            return permissiveTypeIdMapper().writeValueAsString(new Typed(value));
        }

        private Object read(String json) throws Exception {
            return permissiveTypeIdMapper().readValue(json, Typed.class).value();
        }

        @Test
        void option_isWrappedLikeAScalar() throws Exception {
            var some = write(Option.some(42));
            assertThat(some).isEqualTo("{\"value\":[\"dmx.fun.Option\",42]}");
            assertThat(((Option<?>) read(some)).get()).isEqualTo(42);

            var none = write(Option.none());
            assertThat(none).isEqualTo("{\"value\":[\"dmx.fun.Option\",null]}");
            assertThat(((Option<?>) read(none)).isEmpty()).isTrue();
        }

        @Test
        void result_carriesTheIdInsideItsObject() throws Exception {
            var ok = write(Result.ok(42));
            assertThat(ok).isEqualTo("{\"value\":{\"@class\":\"dmx.fun.Result\",\"ok\":42}}");
            assertThat(((Result<?, ?>) read(ok)).get()).isEqualTo(42);

            var err = write(Result.err("bad"));
            assertThat(err).isEqualTo("{\"value\":{\"@class\":\"dmx.fun.Result\",\"err\":\"bad\"}}");
            assertThat(((Result<?, ?>) read(err)).getError()).isEqualTo("bad");
        }

        @Test
        void try_successIsWrappedAndFailureCarriesTheIdInsideItsObject() throws Exception {
            var success = write(Try.success(42));
            assertThat(success).isEqualTo("{\"value\":[\"dmx.fun.Try\",42]}");
            assertThat(((Try<?>) read(success)).get()).isEqualTo(42);

            var failure = write(Try.failure(new RuntimeException("boom")));
            assertThat(failure).isEqualTo("{\"value\":{\"@class\":\"dmx.fun.Try\",\"error\":\"boom\"}}");
            var back = (Try<?>) read(failure);
            assertThat(back.isFailure()).isTrue();
            assertThat(back.getCause().getMessage()).isEqualTo("boom");
        }

        @Test
        void validated_carriesTheIdInsideItsObject() throws Exception {
            var json = write(Validated.valid(42));
            assertThat(json).isEqualTo("{\"value\":{\"@class\":\"dmx.fun.Validated\",\"valid\":42}}");
            assertThat(((Validated<?, ?>) read(json)).get()).isEqualTo(42);
        }

        @Test
        void either_carriesTheIdInsideItsObject() throws Exception {
            var json = write(Either.left("l"));
            assertThat(json).isEqualTo("{\"value\":{\"@class\":\"dmx.fun.Either\",\"left\":\"l\"}}");
            assertThat(((Either<?, ?>) read(json)).getLeft()).isEqualTo("l");
        }

        @Test
        void tuples_carryTheIdAroundTheirArray() throws Exception {
            var t2 = write(Tuple2.of(1, 2));
            assertThat(t2).isEqualTo("{\"value\":[\"dmx.fun.Tuple2\",[1,2]]}");
            assertThat(read(t2)).isEqualTo(Tuple2.of(1, 2));

            var t3 = write(Tuple3.of(1, 2, 3));
            assertThat(t3).isEqualTo("{\"value\":[\"dmx.fun.Tuple3\",[1,2,3]]}");
            assertThat(read(t3)).isEqualTo(Tuple3.of(1, 2, 3));

            var t4 = write(Tuple4.of(1, 2, 3, 4));
            assertThat(t4).isEqualTo("{\"value\":[\"dmx.fun.Tuple4\",[1,2,3,4]]}");
            assertThat(read(t4)).isEqualTo(Tuple4.of(1, 2, 3, 4));
        }

        @Test
        void nonEmptyList_carriesTheIdAroundItsArray() throws Exception {
            var json = write(NonEmptyList.of(1, List.of(2)));
            assertThat(json).isEqualTo("{\"value\":[\"dmx.fun.NonEmptyList\",[1,2]]}");
            assertThat(((NonEmptyList<?>) read(json)).toList()).isEqualTo(List.of(1, 2));
        }

        @Test
        void nameIds_nameTheDmxFunType() throws Exception {
            var named = JsonMapper.builder().addModule(new DmxFunModule())
                .polymorphicTypeValidator(BasicPolymorphicTypeValidator.builder().allowIfBaseType(Object.class).build())
                .registerSubtypes(new NamedType(Result.class, "Result"), new NamedType(Option.class, "Option"))
                .build();
            var result = named.writeValueAsString(new Named(Result.ok(42)));
            assertThat(result).isEqualTo("{\"value\":{\"@type\":\"Result\",\"ok\":42}}");
            assertThat(((Result<?, ?>) named.readValue(result, Named.class).value()).get()).isEqualTo(42);

            var option = named.writeValueAsString(new Named(Option.some(42)));
            assertThat(option).isEqualTo("{\"value\":[\"Option\",42]}");
            assertThat(((Option<?>) named.readValue(option, Named.class).value()).get()).isEqualTo(42);
        }

        @Test
        void jsonSerializeWithoutTyping_doesNotDisableStaticTyping() throws Exception {
            var staticMapper = JsonMapper.builder().addModule(new DmxFunModule()).enable(MapperFeature.USE_STATIC_TYPING).build();
            assertThat(staticMapper.writeValueAsString(new AnnotatedZoo(NonEmptySet.singleton(dog()))))
                .isEqualTo("{\"set\":[{\"name\":\"rex\"}]}");
        }

        @Test
        void deductionIds_writeNothingAndDoNotFail() throws Exception {
            assertThat(mapper.writeValueAsString(new Deduced(Result.ok(1)))).isEqualTo("{\"value\":{\"ok\":1}}");
            assertThat(mapper.writeValueAsString(new Deduced(Option.some(1)))).isEqualTo("{\"value\":1}");
        }

        @Test
        void payloadsDeclaredAsObject_carryTypeIdsUnderDefaultTyping() throws Exception {
            var typed = nonFinalTypingMapper();
            var item = new Item("x");
            var holder = new ObjectPayloads(Option.some(item), Try.success(item), Result.ok(item), Validated.valid(item),
                Either.right(item), Tuple2.of(item, 1), Tuple3.of(item, 1, 2), Tuple4.of(item, 1, 2, 3));
            var json = typed.writeValueAsString(holder);
            assertThat(json.split(java.util.regex.Pattern.quote(Item.class.getName()), -1)).hasSize(9);
            var back = typed.readValue(json, ObjectPayloads.class);
            assertThat(back.option().get()).isEqualTo(item);
            assertThat(back.attempt().get()).isEqualTo(item);
            assertThat(back.result().get()).isEqualTo(item);
            assertThat(back.validated().get()).isEqualTo(item);
            assertThat(back.either().getRight()).isEqualTo(item);
            assertThat(back.t2()._1()).isEqualTo(item);
            assertThat(back.t3()._1()).isEqualTo(item);
            assertThat(back.t4()._1()).isEqualTo(item);
        }

        @Test
        void nestedSameType_roundTrips() throws Exception {
            var nested = new SameTypeNesting(Option.some(Option.some(new Item("x"))), Tuple2.of(Tuple2.of("a", 1), "b"),
                Result.ok(Result.ok("v")), Either.left(Either.right(1)), NonEmptyList.of(NonEmptyList.of(1, List.of(2)), List.of()));
            var plain = mapper.writeValueAsString(nested);
            assertThat(plain).isEqualTo("{\"option\":{\"name\":\"x\"},\"tuple\":[[\"a\",1],\"b\"],\"result\":{\"ok\":{\"ok\":\"v\"}},"
                + "\"either\":{\"left\":{\"right\":1}},\"list\":[[1,2]]}");
            assertThat(mapper.readValue(plain, SameTypeNesting.class)).isEqualTo(nested);

            var typed = nonFinalTypingMapper();
            assertThat(typed.readValue(typed.writeValueAsString(nested), SameTypeNesting.class)).isEqualTo(nested);
        }

        @Test
        void rootValues_underDefaultTyping_resolvePayloadsAsObject() throws Exception {
            var typed = nonFinalTypingMapper();
            var json = typed.writeValueAsString(Result.<Item, String>ok(new Item("x")));
            // Jackson resolves a root value by its runtime class (Result.Ok, with no bindings), so the
            // payload type is Object and the final Item still gets an id, as inside a raw List at the root.
            assertThat(json).isEqualTo("{\"ok\":[\"" + Item.class.getName() + "\",{\"name\":\"x\"}]}");
        }

        @Test
        void payloadsDeclaredAsFinalType_stayPlainUnderNonFinalTyping() throws Exception {
            var typed = nonFinalTypingMapper();
            var item = new Item("x");
            var holder = new ItemPayloads(Option.some(item), Try.success(item), Result.ok(item), Validated.valid(item),
                Either.right(item), Tuple2.of(item, 1), Tuple3.of(item, 1, 2), Tuple4.of(item, 1, 2, 3));
            var json = typed.writeValueAsString(holder);
            assertThat(json).doesNotContain(Item.class.getName());
            assertThat(typed.readValue(json, ItemPayloads.class)).isEqualTo(holder);
        }

        @Test
        void propertyTypingOverride_isHonouredByTheNonEmptyContainers() throws Exception {
            var dynamic = new DynamicZoo(NonEmptySet.singleton(dog()), NonEmptyMap.singleton("a", dog()), NonEmptyList.of(dog(), List.of()));
            var staticMapper = JsonMapper.builder().addModule(new DmxFunModule()).enable(MapperFeature.USE_STATIC_TYPING).build();
            assertThat(staticMapper.writeValueAsString(dynamic))
                .doesNotContain("[{\"name\":\"rex\"}]")
                .contains("\"breed\":\"collie\"");

            var fixed = new StaticZoo(NonEmptySet.singleton(dog()), NonEmptyMap.singleton("a", dog()), NonEmptyList.of(dog(), List.of()));
            assertThat(mapper.writeValueAsString(fixed))
                .isEqualTo("{\"set\":[{\"name\":\"rex\"}],\"map\":{\"a\":{\"name\":\"rex\"}},\"list\":[{\"name\":\"rex\"}]}");
        }
    }

    // -------------------------------------------------------------------------
    // ServiceLoader auto-discovery
    // -------------------------------------------------------------------------

    @Nested
    class ServiceLoaderTests {

        private ObjectMapper autoMapper;

        @BeforeEach
        void setUp() {
            autoMapper = JsonMapper.builder().findAndAddModules().build();
        }

        @Test
        void autoDiscoveryWorksForOption() throws Exception {
            var some = Option.some("auto");
            var json = autoMapper.writeValueAsString(some);
            assertThat(json).isEqualTo("\"auto\"");

            var deserialized = autoMapper.readValue("\"auto\"", new TypeReference<Option<String>>() {});
            assertThat(deserialized).isSome().containsValue("auto");
        }

        @Test
        void autoDiscoveryWorksForResult() throws Exception {
            var ok = Result.ok("found");
            var json = autoMapper.writeValueAsString(ok);
            assertThat(json).isEqualTo("{\"ok\":\"found\"}");

            var deserialized = autoMapper.readValue(json, new TypeReference<Result<String, Integer>>() {});
            assertThat(deserialized).isOk().containsValue("found");
        }

        @Test
        void autoDiscoveryWorksForTry() throws Exception {
            var success = Try.success("done");
            var json = autoMapper.writeValueAsString(success);
            assertThat(json).isEqualTo("\"done\"");

            var deserialized = autoMapper.readValue(json, new TypeReference<Try<String>>() {});
            assertThat(deserialized).isSuccess().containsValue("done");
        }

        @Test
        void autoDiscoveryWorksForValidated() throws Exception {
            var valid = Validated.valid(7);
            var json = autoMapper.writeValueAsString(valid);
            assertThat(json).isEqualTo("{\"valid\":7}");

            var deserialized = autoMapper.readValue(json, new TypeReference<Validated<String, Integer>>() {});
            assertThat(deserialized).isValid().containsValue(7);
        }

        @Test
        void autoDiscoveryWorksForEither() throws Exception {
            var right = Either.right(3);
            var json = autoMapper.writeValueAsString(right);
            assertThat(json).isEqualTo("{\"right\":3}");

            var deserialized = autoMapper.readValue(json, new TypeReference<Either<String, Integer>>() {});
            assertThat(deserialized).isRight().containsRight(3);
        }

        @Test
        void autoDiscoveryWorksForTuple2() throws Exception {
            var t = Tuple2.of("x", 1);
            var json = autoMapper.writeValueAsString(t);
            assertThat(json).isEqualTo("[\"x\",1]");

            var deserialized = autoMapper.readValue(json, new TypeReference<Tuple2<String, Integer>>() {});
            assertThat(deserialized._1()).isEqualTo("x");
            assertThat(deserialized._2()).isEqualTo(1);
        }

        @Test
        void autoDiscoveryWorksForTuple3() throws Exception {
            var t = Tuple3.of("y", 2, true);
            var json = autoMapper.writeValueAsString(t);
            assertThat(json).isEqualTo("[\"y\",2,true]");

            var deserialized = autoMapper.readValue(json, new TypeReference<Tuple3<String, Integer, Boolean>>() {});
            assertThat(deserialized._1()).isEqualTo("y");
            assertThat(deserialized._2()).isEqualTo(2);
            assertThat(deserialized._3()).isEqualTo(true);
        }

        @Test
        void autoDiscoveryWorksForTuple4() throws Exception {
            var t = Tuple4.of("z", 3, false, 1.5);
            var json = autoMapper.writeValueAsString(t);
            assertThat(json).isEqualTo("[\"z\",3,false,1.5]");

            var deserialized = autoMapper.readValue(json, new TypeReference<Tuple4<String, Integer, Boolean, Double>>() {});
            assertThat(deserialized._1()).isEqualTo("z");
            assertThat(deserialized._2()).isEqualTo(3);
            assertThat(deserialized._3()).isEqualTo(false);
            assertThat((double) deserialized._4()).isEqualTo(1.5);
        }

        @Test
        void autoDiscoveryWorksForNonEmptyList() throws Exception {
            var nel = NonEmptyList.of(1, List.of(2, 3));
            var json = autoMapper.writeValueAsString(nel);
            assertThat(json).isEqualTo("[1,2,3]");

            var deserialized = autoMapper.readValue(json, new TypeReference<NonEmptyList<Integer>>() {});
            assertThat(deserialized.head()).isEqualTo(1);
            assertThat(deserialized.tail()).isEqualTo(List.of(2, 3));
        }
    }
}
