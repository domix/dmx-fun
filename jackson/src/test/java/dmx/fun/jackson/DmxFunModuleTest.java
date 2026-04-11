package dmx.fun.jackson;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.JsonNode;
import dmx.fun.Either;
import dmx.fun.NonEmptyList;
import dmx.fun.Option;
import dmx.fun.Result;
import dmx.fun.Try;
import dmx.fun.Tuple2;
import dmx.fun.Tuple3;
import dmx.fun.Tuple4;
import dmx.fun.Validated;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;

import static org.junit.jupiter.api.Assertions.*;

class DmxFunModuleTest {

    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new ObjectMapper().registerModule(new DmxFunModule());
    }

    // -------------------------------------------------------------------------
    // Option
    // -------------------------------------------------------------------------

    @Nested
    class OptionTests {

        @Test
        void serializeSome() throws Exception {
            Option<String> some = Option.some("hello");
            String json = mapper.writeValueAsString(some);
            assertEquals("\"hello\"", json);
        }

        @Test
        void serializeNone() throws Exception {
            Option<String> none = Option.none();
            String json = mapper.writeValueAsString(none);
            assertEquals("null", json);
        }

        @Test
        void deserializeSome() throws Exception {
            Option<String> result = mapper.readValue("\"world\"", new TypeReference<Option<String>>() {});
            assertTrue(result.isDefined());
            assertEquals("world", result.get());
        }

        @Test
        void deserializeNone() throws Exception {
            Option<String> result = mapper.readValue("null", new TypeReference<Option<String>>() {});
            assertTrue(result.isEmpty());
        }

        @Test
        void roundTripSome() throws Exception {
            Option<Integer> original = Option.some(42);
            String json = mapper.writeValueAsString(original);
            Option<Integer> deserialized = mapper.readValue(json, new TypeReference<Option<Integer>>() {});
            assertTrue(deserialized.isDefined());
            assertEquals(42, deserialized.get());
        }

        @Test
        void roundTripNone() throws Exception {
            Option<Integer> original = Option.none();
            String json = mapper.writeValueAsString(original);
            Option<Integer> deserialized = mapper.readValue(json, new TypeReference<Option<Integer>>() {});
            assertTrue(deserialized.isEmpty());
        }

        @Test
        @SuppressWarnings("unchecked")
        void deserializeRawType_coversNullValueTypePath() throws Exception {
            // No TypeReference → createContextual returns `this` (no type params),
            // deserialize falls back to p.readValueAs(Object.class)
            Option<?> result = mapper.readValue("\"raw\"", Option.class);
            assertTrue(result.isDefined());
            assertEquals("raw", result.get());
        }
    }

    // -------------------------------------------------------------------------
    // Result
    // -------------------------------------------------------------------------

    @Nested
    class ResultTests {

        @Test
        void serializeOk() throws Exception {
            Result<String, Integer> ok = Result.ok("success");
            String json = mapper.writeValueAsString(ok);
            assertEquals("{\"ok\":\"success\"}", json);
        }

        @Test
        void serializeErr() throws Exception {
            Result<String, Integer> err = Result.err(404);
            String json = mapper.writeValueAsString(err);
            assertEquals("{\"err\":404}", json);
        }

        @Test
        void roundTripOk() throws Exception {
            Result<String, Integer> original = Result.ok("hello");
            String json = mapper.writeValueAsString(original);
            Result<String, Integer> deserialized = mapper.readValue(json, new TypeReference<Result<String, Integer>>() {});
            assertTrue(deserialized.isOk());
            assertEquals("hello", deserialized.get());
        }

        @Test
        void roundTripErr() throws Exception {
            Result<String, Integer> original = Result.err(500);
            String json = mapper.writeValueAsString(original);
            Result<String, Integer> deserialized = mapper.readValue(json, new TypeReference<Result<String, Integer>>() {});
            assertTrue(deserialized.isError());
            assertEquals(500, deserialized.getError());
        }

        @Test
        @SuppressWarnings("unchecked")
        void deserializeOk_rawType_coversNullValueTypePath() throws Exception {
            Result<?, ?> result = mapper.readValue("{\"ok\":99}", Result.class);
            assertTrue(result.isOk());
            assertEquals(99, result.get());
        }

        @Test
        @SuppressWarnings("unchecked")
        void deserializeErr_rawType_coversNullErrorTypePath() throws Exception {
            Result<?, ?> result = mapper.readValue("{\"err\":\"oops\"}", Result.class);
            assertTrue(result.isError());
            assertEquals("oops", result.getError());
        }

        @Test
        void deserializeUnknownField_throws() {
            JsonMappingException ex = assertThrows(JsonMappingException.class, () ->
                mapper.readValue("{\"unknown\":42}", new TypeReference<Result<Integer, String>>() {}));
            assertTrue(ex.getMessage().contains("unknown"));
        }
    }

    // -------------------------------------------------------------------------
    // Try
    // -------------------------------------------------------------------------

    @Nested
    class TryTests {

        @Test
        void serializeSuccess() throws Exception {
            Try<String> success = Try.success("ok");
            String json = mapper.writeValueAsString(success);
            assertEquals("\"ok\"", json);
        }

        @Test
        void serializeFailure() throws Exception {
            Try<String> failure = Try.failure(new RuntimeException("boom"));
            String json = mapper.writeValueAsString(failure);
            assertEquals("{\"error\":\"boom\"}", json);
        }

        @Test
        void roundTripSuccess() throws Exception {
            Try<Integer> original = Try.success(99);
            String json = mapper.writeValueAsString(original);
            Try<Integer> deserialized = mapper.readValue(json, new TypeReference<Try<Integer>>() {});
            assertTrue(deserialized.isSuccess());
            assertEquals(99, deserialized.get());
        }

        @Test
        void roundTripFailure() throws Exception {
            Try<Integer> original = Try.failure(new RuntimeException("error message"));
            String json = mapper.writeValueAsString(original);
            Try<Integer> deserialized = mapper.readValue(json, new TypeReference<Try<Integer>>() {});
            assertTrue(deserialized.isFailure());
            assertEquals("error message", deserialized.getCause().getMessage());
        }

        @Test
        void deserializeSuccessObject_coversReadTreeAsValuePath() throws Exception {
            // When the JSON is a START_OBJECT with no "error" key and valueType is known,
            // TryDeserializer calls ctxt.readTreeAsValue(node, valueType)
            String json = "{\"name\":\"alice\"}";
            Try<Map<String, Object>> result = mapper.readValue(json,
                new TypeReference<Try<Map<String, Object>>>() {});
            assertTrue(result.isSuccess());
            assertEquals("alice", result.get().get("name"));
        }

        @Test
        @SuppressWarnings("unchecked")
        void deserializeSuccessObject_rawType_coversNodeReturnPath() throws Exception {
            // START_OBJECT, no "error" key, valueType == null → returns Try.success(node)
            Try<?> result = mapper.readValue("{\"key\":\"v\"}", Try.class);
            assertTrue(result.isSuccess());
            assertInstanceOf(JsonNode.class, result.get());
            assertEquals("v", ((JsonNode) result.get()).get("key").asText());
        }

        @Test
        @SuppressWarnings("unchecked")
        void deserializeSuccess_rawType_coversPrimitiveNullTypePath() throws Exception {
            // Non-object token, valueType == null → p.readValueAs(Object.class)
            Try<?> result = mapper.readValue("42", Try.class);
            assertTrue(result.isSuccess());
            assertEquals(42, result.get());
        }
    }

    // -------------------------------------------------------------------------
    // Validated
    // -------------------------------------------------------------------------

    @Nested
    class ValidatedTests {

        @Test
        void serializeValid() throws Exception {
            Validated<String, Integer> valid = Validated.valid(42);
            String json = mapper.writeValueAsString(valid);
            assertEquals("{\"valid\":42}", json);
        }

        @Test
        void serializeInvalid() throws Exception {
            Validated<String, Integer> invalid = Validated.invalid("error");
            String json = mapper.writeValueAsString(invalid);
            assertEquals("{\"invalid\":\"error\"}", json);
        }

        @Test
        void roundTripValid() throws Exception {
            Validated<String, Integer> original = Validated.valid(7);
            String json = mapper.writeValueAsString(original);
            Validated<String, Integer> deserialized = mapper.readValue(json, new TypeReference<Validated<String, Integer>>() {});
            assertTrue(deserialized.isValid());
            assertEquals(7, deserialized.get());
        }

        @Test
        void roundTripInvalid() throws Exception {
            Validated<String, Integer> original = Validated.invalid("bad input");
            String json = mapper.writeValueAsString(original);
            Validated<String, Integer> deserialized = mapper.readValue(json, new TypeReference<Validated<String, Integer>>() {});
            assertTrue(deserialized.isInvalid());
            assertEquals("bad input", deserialized.getError());
        }

        @Test
        @SuppressWarnings("unchecked")
        void deserializeValid_rawType_coversNullValueTypePath() throws Exception {
            Validated<?, ?> result = mapper.readValue("{\"valid\":7}", Validated.class);
            assertTrue(result.isValid());
            assertEquals(7, result.get());
        }

        @Test
        @SuppressWarnings("unchecked")
        void deserializeInvalid_rawType_coversNullErrorTypePath() throws Exception {
            Validated<?, ?> result = mapper.readValue("{\"invalid\":\"e\"}", Validated.class);
            assertTrue(result.isInvalid());
            assertEquals("e", result.getError());
        }

        @Test
        void deserializeUnknownField_throws() {
            JsonMappingException ex = assertThrows(JsonMappingException.class, () ->
                mapper.readValue("{\"unknown\":42}", new TypeReference<Validated<String, Integer>>() {}));
            assertTrue(ex.getMessage().contains("unknown"));
        }
    }

    // -------------------------------------------------------------------------
    // Either
    // -------------------------------------------------------------------------

    @Nested
    class EitherTests {

        @Test
        void serializeRight() throws Exception {
            Either<String, Integer> right = Either.right(1);
            String json = mapper.writeValueAsString(right);
            assertEquals("{\"right\":1}", json);
        }

        @Test
        void serializeLeft() throws Exception {
            Either<String, Integer> left = Either.left("oops");
            String json = mapper.writeValueAsString(left);
            assertEquals("{\"left\":\"oops\"}", json);
        }

        @Test
        void roundTripRight() throws Exception {
            Either<String, Integer> original = Either.right(5);
            String json = mapper.writeValueAsString(original);
            Either<String, Integer> deserialized = mapper.readValue(json, new TypeReference<Either<String, Integer>>() {});
            assertTrue(deserialized.isRight());
            assertEquals(5, deserialized.getRight());
        }

        @Test
        void roundTripLeft() throws Exception {
            Either<String, Integer> original = Either.left("left value");
            String json = mapper.writeValueAsString(original);
            Either<String, Integer> deserialized = mapper.readValue(json, new TypeReference<Either<String, Integer>>() {});
            assertTrue(deserialized.isLeft());
            assertEquals("left value", deserialized.getLeft());
        }

        @Test
        @SuppressWarnings("unchecked")
        void deserializeRight_rawType_coversNullRightTypePath() throws Exception {
            Either<?, ?> result = mapper.readValue("{\"right\":5}", Either.class);
            assertTrue(result.isRight());
            assertEquals(5, result.getRight());
        }

        @Test
        @SuppressWarnings("unchecked")
        void deserializeLeft_rawType_coversNullLeftTypePath() throws Exception {
            Either<?, ?> result = mapper.readValue("{\"left\":\"e\"}", Either.class);
            assertTrue(result.isLeft());
            assertEquals("e", result.getLeft());
        }

        @Test
        void deserializeUnknownField_throws() {
            JsonMappingException ex = assertThrows(JsonMappingException.class, () ->
                mapper.readValue("{\"unknown\":42}", new TypeReference<Either<String, Integer>>() {}));
            assertTrue(ex.getMessage().contains("unknown"));
        }
    }

    // -------------------------------------------------------------------------
    // Tuple2
    // -------------------------------------------------------------------------

    @Nested
    class Tuple2Tests {

        @Test
        void serialize() throws Exception {
            Tuple2<String, Integer> t = new Tuple2<>("a", 1);
            String json = mapper.writeValueAsString(t);
            assertEquals("[\"a\",1]", json);
        }

        @Test
        void roundTrip() throws Exception {
            Tuple2<String, Integer> original = new Tuple2<>("hello", 42);
            String json = mapper.writeValueAsString(original);
            Tuple2<String, Integer> deserialized = mapper.readValue(json, new TypeReference<Tuple2<String, Integer>>() {});
            assertEquals("hello", deserialized._1());
            assertEquals(42, deserialized._2());
        }

        @Test
        @SuppressWarnings("unchecked")
        void deserializeRawType_coversNullTypePaths() throws Exception {
            Tuple2<?, ?> result = mapper.readValue("[\"a\",1]", Tuple2.class);
            assertEquals("a", result._1());
            assertEquals(1, result._2());
        }
    }

    // -------------------------------------------------------------------------
    // Tuple3
    // -------------------------------------------------------------------------

    @Nested
    class Tuple3Tests {

        @Test
        void serialize() throws Exception {
            Tuple3<String, Integer, Boolean> t = Tuple3.of("a", 1, true);
            String json = mapper.writeValueAsString(t);
            assertEquals("[\"a\",1,true]", json);
        }

        @Test
        void roundTrip() throws Exception {
            Tuple3<String, Integer, Boolean> original = Tuple3.of("x", 10, false);
            String json = mapper.writeValueAsString(original);
            Tuple3<String, Integer, Boolean> deserialized = mapper.readValue(json, new TypeReference<Tuple3<String, Integer, Boolean>>() {});
            assertEquals("x", deserialized._1());
            assertEquals(10, deserialized._2());
            assertEquals(false, deserialized._3());
        }

        @Test
        @SuppressWarnings("unchecked")
        void deserializeRawType_coversNullTypePaths() throws Exception {
            Tuple3<?, ?, ?> result = mapper.readValue("[\"a\",1,true]", Tuple3.class);
            assertEquals("a", result._1());
            assertEquals(1, result._2());
            assertEquals(true, result._3());
        }
    }

    // -------------------------------------------------------------------------
    // Tuple4
    // -------------------------------------------------------------------------

    @Nested
    class Tuple4Tests {

        @Test
        void serialize() throws Exception {
            Tuple4<String, Integer, Boolean, Double> t = Tuple4.of("a", 1, true, 3.14);
            String json = mapper.writeValueAsString(t);
            assertEquals("[\"a\",1,true,3.14]", json);
        }

        @Test
        void roundTrip() throws Exception {
            Tuple4<String, Integer, Boolean, Double> original = Tuple4.of("z", 7, true, 2.71);
            String json = mapper.writeValueAsString(original);
            Tuple4<String, Integer, Boolean, Double> deserialized = mapper.readValue(json, new TypeReference<Tuple4<String, Integer, Boolean, Double>>() {});
            assertEquals("z", deserialized._1());
            assertEquals(7, deserialized._2());
            assertEquals(true, deserialized._3());
            assertEquals(2.71, deserialized._4(), 0.001);
        }

        @Test
        @SuppressWarnings("unchecked")
        void deserializeRawType_coversNullTypePaths() throws Exception {
            Tuple4<?, ?, ?, ?> result = mapper.readValue("[\"a\",1,true,3.14]", Tuple4.class);
            assertEquals("a", result._1());
            assertEquals(1, result._2());
            assertEquals(true, result._3());
            assertEquals(3.14, (double) result._4(), 0.001);
        }
    }

    // -------------------------------------------------------------------------
    // NonEmptyList
    // -------------------------------------------------------------------------

    @Nested
    class NonEmptyListTests {

        @Test
        void serializeSingleElement() throws Exception {
            NonEmptyList<Integer> nel = NonEmptyList.of(1, List.of());
            String json = mapper.writeValueAsString(nel);
            assertEquals("[1]", json);
        }

        @Test
        void serializeMultipleElements() throws Exception {
            NonEmptyList<Integer> nel = NonEmptyList.of(1, List.of(2, 3));
            String json = mapper.writeValueAsString(nel);
            assertEquals("[1,2,3]", json);
        }

        @Test
        void roundTrip() throws Exception {
            NonEmptyList<Integer> original = NonEmptyList.of(1, List.of(2, 3));
            String json = mapper.writeValueAsString(original);
            NonEmptyList<Integer> deserialized = mapper.readValue(json, new TypeReference<NonEmptyList<Integer>>() {});
            assertEquals(1, deserialized.head());
            assertEquals(List.of(2, 3), deserialized.tail());
        }

        @Test
        void emptyArrayThrows() {
            assertThrows(InvalidFormatException.class, () ->
                mapper.readValue("[]", new TypeReference<NonEmptyList<Integer>>() {}));
        }

        @Test
        @SuppressWarnings("unchecked")
        void deserializeRawType_coversNullElementTypePath() throws Exception {
            NonEmptyList<?> result = mapper.readValue("[1,2,3]", NonEmptyList.class);
            assertEquals(1, result.head());
            assertEquals(List.of(2, 3), result.tail());
        }
    }

    // -------------------------------------------------------------------------
    // ServiceLoader auto-discovery
    // -------------------------------------------------------------------------

    @Nested
    class ServiceLoaderTests {

        @Test
        void autoDiscoveryWorksForOption() throws Exception {
            ObjectMapper autoMapper = new ObjectMapper().findAndRegisterModules();
            Option<String> some = Option.some("auto");
            String json = autoMapper.writeValueAsString(some);
            assertEquals("\"auto\"", json);

            Option<String> deserialized = autoMapper.readValue("\"auto\"", new TypeReference<Option<String>>() {});
            assertTrue(deserialized.isDefined());
            assertEquals("auto", deserialized.get());
        }

        @Test
        void autoDiscoveryWorksForResult() throws Exception {
            ObjectMapper autoMapper = new ObjectMapper().findAndRegisterModules();
            Result<String, Integer> ok = Result.ok("found");
            String json = autoMapper.writeValueAsString(ok);
            assertEquals("{\"ok\":\"found\"}", json);

            Result<String, Integer> deserialized = autoMapper.readValue(json, new TypeReference<Result<String, Integer>>() {});
            assertTrue(deserialized.isOk());
            assertEquals("found", deserialized.get());
        }
    }
}
