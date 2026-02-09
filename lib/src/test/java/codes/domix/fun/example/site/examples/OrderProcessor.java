package codes.domix.fun.example.site.examples;

import codes.domix.fun.Option;
import codes.domix.fun.Try;

public class OrderProcessor {
    private final ProductRepo productRepo = new ProductRepo();
    private final CustomerRepo customerRepo = new CustomerRepo();

    record Customer() {
    }

    record Product() {
        public boolean isAvailable() {
            return true;
        }

        public boolean isDefined() {
            return true;
        }
    }

    public record OrderRequest(String customerId, String productId, int quantity) {
    }

    public record OrderResult(Customer customer, Product product, int quantity) {
    }

    static class ProductRepo {
        public Try<Product> findById(String id) {
            return Try.success(new Product());
        }
    }

    static class CustomerRepo {

        public Option<Customer> findById(String id) {
            return Option.some(new Customer());
        }
    }

    static class NotFoundException extends RuntimeException {
        public NotFoundException(String message) {
            super(message);
        }
    }

    // main processing logic
    public Try<OrderResult> processOrder(OrderRequest request) {
        Try<Customer> customer = validateCustomer(request.customerId());
        Try<Product> product = validateProduct(request.productId());
        Try<Integer> quantity = validateQuantity(request.quantity());

        // Combine all validations
        return customer.flatMap(c ->
            product.flatMap(p ->
                quantity.map(q ->
                    createOrder(c, p, q)
                )
            )
        );
    }

    private Try<Customer> validateCustomer(String id) {
        return Try.of(() -> customerRepo.findById(id))
            .flatMap(opt -> opt.isDefined()
                ? Try.success(opt.get())
                : Try.failure(new NotFoundException("Customer not found"))
            );
    }

    private Try<Product> validateProduct(String id) {
        return productRepo.findById(id)
            .filter(Product::isAvailable)
            .flatMap(opt -> opt.isDefined()
                ? Try.success(opt)
                : Try.failure(new NotFoundException("Product %s unavailable".formatted(id)))
            );
    }

    private Try<Integer> validateQuantity(int qty) {
        return Try.success(qty)
            .filter(
                q -> q > 0 && q <= 100,
                () -> new IllegalArgumentException("Invalid quantity")
            );
    }

    private OrderResult createOrder(Customer c, Product p, int qty) {
        return new OrderResult(c, p, qty);
    }

    private void handleError(Throwable error) {
        System.out.println("Error processing order: " + error.getMessage());
    }

    private void confirmOrder(OrderResult order) {
        System.out.println("Order created: " + order);
    }

    void main() {
        // Usage
        OrderProcessor processor = new OrderProcessor();

        Try<OrderResult> result = processor
            .processOrder(
                new OrderRequest("cust123", "prod456", 5)
            )
            .onSuccess(processor::confirmOrder)
            .onFailure(processor::handleError);
    }
}
