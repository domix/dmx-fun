package dmx.fun.sample.item;

import dmx.fun.NonEmptyList;
import dmx.fun.Option;
import dmx.fun.Result;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller demonstrating JSON serialization of dmx-fun types via the dmx-fun Jackson
 * module ({@code fun-jackson3} on Spring Boot 4, {@code fun-jackson2} on Spring Boot 3).
 *
 * <ul>
 *   <li>{@code GET /items/{id}} — returns {@link dmx.fun.Option}{@code <Item>} directly;
 *       {@link dmx.fun.spring.boot.web.DmxFunWebMvcAutoConfiguration} converts it to
 *       HTTP 200 (Some) or 404 (None) automatically.</li>
 *   <li>{@code POST /items} — returns {@link Result}{@code <Item,String>} directly;
 *       Spring MVC maps {@code ok} to HTTP 200 with the value body and {@code err} to
 *       HTTP 500 with the error body.</li>
 *   <li>{@code PUT /items/{id}} — same {@code Result} pattern, demonstrating the declarative
 *       {@link dmx.fun.spring.TransactionalResult} style from the service layer.</li>
 *   <li>{@code GET /items/search?name=...} — returns {@link Result}{@code <NonEmptyList<Item>,String>};
 *       the handler unwraps the {@code Result} and the {@link dmx.fun.NonEmptyList} body is
 *       serialized by {@code DmxFunModule} as a plain JSON array. This is the endpoint that
 *       exercises the Jackson integration itself.</li>
 * </ul>
 */
@RestController
@RequestMapping("/items")
public class ItemController {

    private final ItemService service;

    public ItemController(ItemService service) {
        this.service = service;
    }

    @GetMapping
    public List<Item> findAll() {
        return service.findAll();
    }

    // Option<Item> is handled by OptionHandlerMethodReturnValueHandler (auto-configured):
    //   some(item) → HTTP 200 {"id":1,"name":"...","description":"..."}
    //   none()     → HTTP 404 (empty body)
    @GetMapping("/{id}")
    public Option<Item> findById(@PathVariable("id") Long id) {
        return service.findById(id);
    }

    // Result<Item,String> is handled by ResultHandlerMethodReturnValueHandler:
    //   ok(item)   → HTTP 200 {"id":1,"name":"Widget","description":"…"}
    //   err(error) → HTTP 500 "name must not be blank"
    @PostMapping
    public Result<Item, String> create(@RequestBody CreateItemRequest request) {
        return service.create(request.name(), request.description());
    }

    // Result<NonEmptyList<Item>,String>: the handler unwraps the Result, then Jackson serializes
    // the NonEmptyList through DmxFunModule (registered by fun-spring-boot):
    //   ok(items) → HTTP 200 [{"id":1,...},{"id":2,...}]
    //   err(msg)  → HTTP 500 "no items named 'x'"
    @GetMapping("/search")
    public Result<NonEmptyList<Item>, String> search(@RequestParam("name") String name) {
        return service.search(name);
    }

    @PutMapping("/{id}")
    public Result<Item, String> update(@PathVariable("id") Long id, @RequestBody UpdateItemRequest request) {
        return service.update(id, request.name());
    }

    public record CreateItemRequest(String name, String description) {}

    public record UpdateItemRequest(String name) {}
}
