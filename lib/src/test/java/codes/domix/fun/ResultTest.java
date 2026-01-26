package codes.domix.fun;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

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
}
