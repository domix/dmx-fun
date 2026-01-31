package codes.domix.fun;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ResultTest {
    @Test
    void of_shouldInferRightTypes() {
        Result<String, RuntimeException> boom = Result.err(new RuntimeException("boom"), String.class);
        Result<BigDecimal, String> ok = Result.ok(BigDecimal.ONE, String.class);

        assertTrue(boom.isError());
        assertFalse(boom.isOk());
        assertTrue(ok.isOk());
        assertFalse(ok.isError());
    }

    @Test
    void test_ResultCanMapErrorOnFlatMap() {
        Result<String, Integer> hello = okValue("hello");
        Result<Integer, Integer> helloMapped = hello
            .flatMap(this::okMap);

        assertTrue(helloMapped.isOk());
        assertEquals(5, helloMapped.get());

        Result<Integer, String> mapErrorOnFlatMap = helloMapped
            .flatMap(
                this::test1,
                error -> "" + error
            );
        
        assertTrue(mapErrorOnFlatMap.isOk());
        assertEquals(5, mapErrorOnFlatMap.get());
        
        // Test the error mapping path
        Result<Integer, Integer> errResult = Result.err(42);
        Result<Integer, String> mappedErr = errResult
            .flatMap(
                this::test1,
                error -> "Error: " + error
            );
        
        assertTrue(mappedErr.isError());
        assertEquals("Error: 42", mappedErr.getError());
    }

    Result<String, Integer> okValue(String value) {
        return Result.ok(value);
    }

    Result<Integer, Integer> okMap(String value) {
        return Result.ok(value.length());
    }

    Result<Integer, String> test1(Integer value) {
        return Result.ok(value);
    }
}
