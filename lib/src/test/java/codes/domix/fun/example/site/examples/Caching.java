package codes.domix.fun.example.site.examples;

import codes.domix.fun.Option;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public class Caching<K, V> {
    public record UserData(String name) {
    }

    private final ConcurrentHashMap<K, V> store;

    public Caching() {
        this.store = new ConcurrentHashMap<>();
    }

    public Option<V> get(K key) {
        return Option.ofNullable(store.get(key));
    }

    public V getOrCompute(K key, Supplier<V> supplier) {
        return get(key).getOrElseGet(() -> {
            V value = supplier.get();
            store.put(key, value);
            return value;
        });
    }

    public Option<V> put(K key, V value) {
        return Option.some(store.put(key, value));
    }

    public Option<V> remove(K key) {
        return Option.ofNullable(store.remove(key));
    }

    public boolean contains(K key) {
        return get(key).isDefined();
    }

    class Database {
        public UserData loadUser(String userId) {
            return new UserData(userId);
        }
    }

    void main() {
        Database database = new Database();
        // Usage
        Caching<String, UserData> userCache = new Caching<>();

        // Get from cache or compute
        UserData user = userCache.getOrCompute(
            "user:123",
            () -> database.loadUser("123")
        );

        // Safe lookup
        Option<UserData> maybeUser = userCache.get("user:456");
        maybeUser.peek(userData -> System.out.printf("Found %s%n", userData.name()));
    }
}
