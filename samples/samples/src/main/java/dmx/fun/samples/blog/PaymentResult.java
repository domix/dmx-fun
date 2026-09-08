package dmx.fun.samples.blog;

import java.time.Instant;

public sealed interface PaymentResult {
    record Captured(String reference)        implements PaymentResult {}
    record Declined(String reason)           implements PaymentResult {}
    record Pending(Instant retryAfter)       implements PaymentResult {}


}
