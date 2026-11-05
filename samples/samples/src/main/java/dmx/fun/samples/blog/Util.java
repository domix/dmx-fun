package dmx.fun.samples.blog;

public class Util {
    String describe(PaymentResult r) {
        return switch (r) {                          // compiler enforces every case
            case PaymentResult.Captured c -> "captured " + c.reference();
            case PaymentResult.Declined d -> "declined: " + d.reason();
            case PaymentResult.Pending p  -> "retry after " + p.retryAfter();
        };
    }
}
