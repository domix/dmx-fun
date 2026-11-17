package dmx.fun.tutorial.domain;

/**
 * A third-party payment API. It reports failure the only way it knows: by throwing.
 * The tutorial never changes this interface; it is the kind of code you integrate, not write.
 */
public interface PaymentGateway {
    void charge(String studentId, int amountCents) throws PaymentException;
}
