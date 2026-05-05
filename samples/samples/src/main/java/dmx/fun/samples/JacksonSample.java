package dmx.fun.samples;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dmx.fun.Option;
import dmx.fun.Result;
import dmx.fun.Try;
import dmx.fun.jackson.DmxFunModule;

/**
 * Demonstrates fun-jackson: JSON serialization and deserialization for dmx-fun types.
 * Register DmxFunModule with your ObjectMapper once, then use it everywhere.
 */
public class JacksonSample {

    record UserDto(String name, Option<String> nickname, Result<Integer, String> age) {}

    static void main(String[] args) {
        ObjectMapper mapper = new ObjectMapper().registerModule(new DmxFunModule());

        // Option<T> — Some serializes as {"value":"alice"}, None as {}
        Try<String> optionJson = Try.of(() ->
            mapper.writeValueAsString(Option.some("alice")));
        optionJson.onSuccess(json -> System.out.println("Some JSON:  " + json));

        Try<String> noneJson = Try.of(() ->
            mapper.writeValueAsString(Option.none()));
        noneJson.onSuccess(json -> System.out.println("None JSON:  " + json));

        // Deserialize Option<String>
        Try<Option<String>> parsedSome = Try.of(() ->
            mapper.readValue("{\"value\":\"alice\"}", new TypeReference<>() {}));
        parsedSome.onSuccess(opt -> System.out.println("Parsed Some: " + opt.getOrElse("empty")));

        Try<Option<String>> parsedNone = Try.of(() ->
            mapper.readValue("{}", new TypeReference<>() {}));
        parsedNone.onSuccess(opt -> System.out.println("Parsed None: " + opt.isEmpty()));

        // Result<V, E> — Ok as {"ok":42}, Err as {"err":"not found"}
        Try<String> okJson = Try.of(() ->
            mapper.writeValueAsString(Result.<Integer, String>ok(42)));
        okJson.onSuccess(json -> System.out.println("Ok JSON:    " + json));

        Try<String> errJson = Try.of(() ->
            mapper.writeValueAsString(Result.<Integer, String>err("not found")));
        errJson.onSuccess(json -> System.out.println("Err JSON:   " + json));

        // Deserialize Result<Integer, String>
        Try<Result<Integer, String>> parsedOk = Try.of(() ->
            mapper.readValue("{\"ok\":42}", new TypeReference<>() {}));
        parsedOk.onSuccess(r -> System.out.println("Parsed Ok value: " + r.getOrElse(-1)));

        // Round-trip a record containing dmx-fun types
        UserDto user = new UserDto("Alice", Option.some("ali"), Result.ok(30));
        Try<String> userJson = Try.of(() -> mapper.writeValueAsString(user));
        userJson.onSuccess(json -> System.out.println("UserDto JSON: " + json));

        userJson.flatMap(json -> Try.of(() ->
            mapper.readValue(json, UserDto.class)))
            .onSuccess(parsed -> System.out.println(
                "Round-tripped name: " + parsed.name()
                + ", nick: " + parsed.nickname().getOrElse("none")));
    }
}
