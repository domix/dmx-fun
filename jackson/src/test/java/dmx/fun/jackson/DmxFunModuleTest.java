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

import static dmx.fun.assertj.DmxFunAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
            var ex = assertThrows(JsonMappingException.class, () ->
                mapper.readValue("{\"unknown\":42}", new TypeReference<Result<Integer, String>>() {}));
            assertThat(ex.getMessage()).contains("unknown");
        }

        @Test
        void deserializeFromNonObject_throws() {
            assertThrows(JsonMappingException.class, () ->
                mapper.readValue("42", new TypeReference<Result<Integer, String>>() {}));
        }

        @Test
        void deserializeEmptyObject_throws() {
            assertThrows(JsonMappingException.class, () ->
                mapper.readValue("{}", new TypeReference<Result<Integer, String>>() {}));
        }

        @Test
        void deserializeExtraField_throws() {
            var ex = assertThrows(JsonMappingException.class, () ->
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
            assertThat(((JsonNode) result.get()).get("key").asText()).isEqualTo("v");
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
            var ex = assertThrows(JsonMappingException.class, () ->
                mapper.readValue("{\"unknown\":42}", new TypeReference<Validated<String, Integer>>() {}));
            assertThat(ex.getMessage()).contains("unknown");
        }

        @Test
        void deserializeFromNonObject_throws() {
            assertThrows(JsonMappingException.class, () ->
                mapper.readValue("42", new TypeReference<Validated<String, Integer>>() {}));
        }

        @Test
        void deserializeEmptyObject_throws() {
            assertThrows(JsonMappingException.class, () ->
                mapper.readValue("{}", new TypeReference<Validated<String, Integer>>() {}));
        }

        @Test
        void deserializeExtraField_throws() {
            var ex = assertThrows(JsonMappingException.class, () ->
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
            var ex = assertThrows(JsonMappingException.class, () ->
                mapper.readValue("{\"unknown\":42}", new TypeReference<Either<String, Integer>>() {}));
            assertThat(ex.getMessage()).contains("unknown");
        }

        @Test
        void deserializeFromNonObject_throws() {
            assertThrows(JsonMappingException.class, () ->
                mapper.readValue("42", new TypeReference<Either<String, Integer>>() {}));
        }

        @Test
        void deserializeEmptyObject_throws() {
            assertThrows(JsonMappingException.class, () ->
                mapper.readValue("{}", new TypeReference<Either<String, Integer>>() {}));
        }

        @Test
        void deserializeExtraField_throws() {
            var ex = assertThrows(JsonMappingException.class, () ->
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
            assertThrows(JsonMappingException.class, () ->
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
            assertThrows(InvalidFormatException.class, () ->
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
            assertThrows(JsonMappingException.class, () ->
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
            assertThrows(JsonMappingException.class, () ->
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
            assertThrows(JsonMappingException.class, () ->
                mapper.readValue("42", new TypeReference<NonEmptyList<Integer>>() {}));
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
            autoMapper = new ObjectMapper().findAndRegisterModules();
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
