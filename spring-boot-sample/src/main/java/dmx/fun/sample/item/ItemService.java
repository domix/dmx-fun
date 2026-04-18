package dmx.fun.sample.item;

import dmx.fun.Option;
import dmx.fun.Result;
import dmx.fun.spring.TransactionalResult;
import dmx.fun.spring.TxResult;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Item service demonstrating both transactional styles provided by fun-spring-boot.
 *
 * <ul>
 *   <li><b>Programmatic</b> ({@link TxResult#execute}): {@link #create} — gives full control
 *       over the transaction boundary at the call site.</li>
 *   <li><b>Declarative</b> ({@link TransactionalResult}): {@link #update} — keeps the service
 *       method free of transaction boilerplate; the AOP aspect handles rollback on
 *       {@code Result.err}.</li>
 * </ul>
 */
@Service
public class ItemService {

    private final ItemRepository repository;
    private final TxResult txResult;

    public ItemService(ItemRepository repository, TxResult txResult) {
        this.repository = repository;
        this.txResult   = txResult;
    }

    // ── Programmatic TxResult ─────────────────────────────────────────────────

    /**
     * Creates a new item inside a managed transaction.
     *
     * <p>Uses {@link TxResult#execute} so the transaction rolls back automatically
     * when the returned {@code Result} is {@code Err}.
     */
    public Result<Item, String> create(String name, String description) {
        return txResult.execute(() -> {
            if (name == null || name.isBlank()) {
                return Result.err("name must not be blank");
            }
            Item saved = repository.save(new Item(null, name.strip(), description));
            return Result.ok(saved);
        });
    }

    // ── Declarative @TransactionalResult ─────────────────────────────────────

    /**
     * Updates an existing item's name.
     *
     * <p>Annotated with {@link TransactionalResult}: {@link dmx.fun.spring.DmxTransactionalAspect}
     * intercepts the call and rolls back when the returned {@code Result} is {@code Err}.
     */
    @TransactionalResult
    public Result<Item, String> update(Long id, String name) {
        if (name == null || name.isBlank()) {
            return Result.err("name must not be blank");
        }
        return repository.findById(id)
            .<Result<Item, String>>map(existing -> {
                Item updated = repository.save(new Item(existing.id(), name.strip(), existing.description()));
                return Result.ok(updated);
            })
            .orElseGet(() -> Result.err("item not found: " + id));
    }

    // ── Queries ───────────────────────────────────────────────────────────────

    /**
     * Returns {@link Option#some} when the item exists, {@link Option#none} otherwise.
     *
     * <p>Converts {@code Optional} → {@code Option} so callers stay in the dmx-fun type world.
     */
    public Option<Item> findById(Long id) {
        return Option.fromOptional(repository.findById(id));
    }

    public List<Item> findAll() {
        List<Item> items = new ArrayList<>();
        repository.findAll().forEach(items::add);
        return items;
    }

    public List<Item> findByName(String name) {
        return repository.findByName(name);
    }
}
