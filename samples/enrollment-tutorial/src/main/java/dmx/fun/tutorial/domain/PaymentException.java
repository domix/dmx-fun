package dmx.fun.tutorial.domain;

/** Thrown by the third-party {@link PaymentGateway}; we do not control this type. */
public class PaymentException extends Exception {
    public PaymentException(String message) {
        super(message);
    }
}
