package dmx.fun.samples;

import dmx.fun.Guard;
import dmx.fun.Option;
import dmx.fun.Result;
import dmx.fun.Try;
import dmx.fun.micrometer.DmxMetered;
import dmx.fun.micrometer.DmxMicrometer;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.jspecify.annotations.NullMarked;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Real-world sample for fun-micrometer:
 * instruments checkout operations end-to-end while keeping Try/Result semantics.
 */
@NullMarked
public class MicrometerRealWorldSample {

    record Order(String id, String sku, int qty, double amountUsd) {
    }

    record Receipt(String orderId, String paymentId) {
    }

    sealed interface CheckoutError permits CheckoutError.InventoryRejected, CheckoutError.PaymentFailed {
        record InventoryRejected(String reason) implements CheckoutError {
        }

        record PaymentFailed(String reason) implements CheckoutError {
        }

        static CheckoutError inventoryRejected(String reason) {
            return new InventoryRejected(reason);
        }

        static CheckoutError paymentFailed(String reason) {
            return new PaymentFailed(reason);
        }
    }

    /**
     * Provides an interface to manage and interact with the inventory system.
     * This class includes operations for reserving stock and validating availability.
     * It ensures that quantities are positive, checks stock levels, and decrements inventory accordingly.
     * The internal logic uses a fluent style with functional constructs for streamlined flow operations.
     *
     * This class operates on a stock map where stock quantities are stored against product SKUs.
     * Operations are thread-safe, as modifications to the stock map are managed using a concurrent data structure.
     *
     * Key Features:
     * - Validates that quantities are positive.
     * - Checks and handles stock availability for a given SKU.
     * - Updates inventory by decrementing stock on successful reservation.
     */
    static final class InventoryGateway {
        private static final Guard<Integer> POSITIVE_QTY = Guard.of(
            q -> q > 0,
            "qty must be > 0, got: %d"::formatted
        );

        private final Map<String, Integer> stockBySku = new ConcurrentHashMap<>();

        InventoryGateway() {
            stockBySku.put("book-001", 3);
        }

        Result<String, CheckoutError> reserve(String sku, int qty) {
            return validateQuantity(qty)
                .flatMap(_ -> checkStockFor(sku))
                .flatMap(currentStock -> ensureEnoughStock(currentStock, qty))
                .flatMap(currentStock -> decrementStock(sku, currentStock, qty))
                .map(_ -> "reserved");
        }

        private Result<Integer, CheckoutError> validateQuantity(int qty) {
            return POSITIVE_QTY.check(qty)
                .toResult()
                .mapError(errors -> CheckoutError.inventoryRejected(errors.head()));
        }

        private Result<Integer, CheckoutError> checkStockFor(String sku) {
            return Option.ofNullable(stockBySku.get(sku))
                .toResult(CheckoutError.inventoryRejected("unknown sku: %s".formatted(sku)));
        }

        private Result<Integer, CheckoutError> ensureEnoughStock(int currentStock, int qty) {
            return Result.<Integer, CheckoutError>ok(currentStock)
                .filter(
                    stock -> stock >= qty,
                    CheckoutError.inventoryRejected("insufficient stock")
                );
        }

        private Result<Integer, CheckoutError> decrementStock(String sku, int currentStock, int qty) {
            stockBySku.put(sku, currentStock - qty);
            return Result.ok(currentStock);
        }
    }

    static final class PaymentGateway {
        Try<String> charge(String orderId, double amountUsd) {
            if (amountUsd > 1000) {
                return Try.failure(new IOException("payment provider timeout"));
            }
            return Try.success("pay_" + orderId);
        }
    }

    static final class CheckoutService {
        private final InventoryGateway inventory;
        private final PaymentGateway payment;

        CheckoutService(InventoryGateway inventory, PaymentGateway payment) {
            this.inventory = inventory;
            this.payment = payment;
        }

        Result<Receipt, CheckoutError> checkout(Order order) {
            return reserveInventory(order)
                .flatMap(_ -> chargePayment(order))
                .map(paymentId -> new Receipt(order.id(), paymentId));
        }

        Result<String, CheckoutError> reserveInventory(Order order) {
            return inventory.reserve(order.sku(), order.qty());
        }

        Result<String, CheckoutError> chargePayment(Order order) {
            return chargePaymentRaw(order)
                .toResult(ex -> CheckoutError.paymentFailed(ex.getMessage()));
        }

        Try<String> chargePaymentRaw(Order order) {
            return payment.charge(order.id(), order.amountUsd());
        }
    }

    static final class MeteredCheckoutService {
        private static final String RESERVE_INVENTORY_METRIC = "checkout.reserve_inventory";
        private static final String CHARGE_PAYMENT_METRIC = "checkout.charge_payment";
        private static final Tags CHARGE_PAYMENT_TAGS = Tags.of("provider", "acme-pay");

        private final DmxMicrometer metrics;
        private final MeterRegistry registry;
        private final CheckoutService delegate;

        MeteredCheckoutService(DmxMicrometer metrics, MeterRegistry registry, CheckoutService delegate) {
            this.metrics = metrics;
            this.registry = registry;
            this.delegate = delegate;
        }

        Result<Receipt, CheckoutError> checkout(Order order) {
            return reserveInventory(order)
                .flatMap(_ -> chargePayment(order))
                .map(paymentId -> new Receipt(order.id(), paymentId));
        }

        private Result<String, CheckoutError> reserveInventory(Order order) {
            return metrics.recordResult(
                    RESERVE_INVENTORY_METRIC,
                    Tags.of("sku", order.sku()),
                    () -> delegate.reserveInventory(order)
                        .toTry(MeteredCheckoutService::checkoutErrorToException)
                        .getOrThrow()
                )
                .mapError(ex -> CheckoutError.inventoryRejected(ex.getMessage()));
        }

        private Result<String, CheckoutError> chargePayment(Order order) {
            return DmxMetered.of(CHARGE_PAYMENT_METRIC)
                .tags(CHARGE_PAYMENT_TAGS)
                .registry(registry)
                .recordTry(() -> delegate.chargePaymentRaw(order).getOrThrow())
                .toResult(ex -> CheckoutError.paymentFailed(ex.getMessage()));
        }

        private static IllegalStateException checkoutErrorToException(CheckoutError error) {
            return switch (error) {
                case CheckoutError.InventoryRejected inventoryRejected ->
                    new IllegalStateException(inventoryRejected.reason());
                case CheckoutError.PaymentFailed paymentFailed ->
                    new IllegalStateException(paymentFailed.reason());
            };
        }
    }

    static void main() {
        final var registry = new SimpleMeterRegistry();
        final var metrics = DmxMicrometer.of(registry);
        final var core = new CheckoutService(new InventoryGateway(), new PaymentGateway());

        final var service = new MeteredCheckoutService(
            metrics, registry, core
        );

        final var ok = service.checkout(
            new Order("ord-1001", "book-001", 1, 49.90)
        );
        final var fail = service.checkout(
            new Order("ord-1002", "book-001", 1, 1500.00)
        );

        IO.println("Checkout #1 success: " + ok.isSuccess());
        IO.println("Checkout #2 success: " + fail.isSuccess());
        IO.println("checkout.reserve_inventory.count success = " +
            registry.get("checkout.reserve_inventory.count")
                .tag("outcome", "success")
                .counter()
                .count()
        );
        IO.println("checkout.charge_payment.count failure = " +
            registry.get("checkout.charge_payment.count")
                .tag("outcome", "failure")
                .counter()
                .count()
        );
        IO.println("checkout.charge_payment.failure IOException = " +
            registry.get("checkout.charge_payment.failure")
                .tag("exception", "IOException")
                .counter()
                .count()
        );
    }
}
