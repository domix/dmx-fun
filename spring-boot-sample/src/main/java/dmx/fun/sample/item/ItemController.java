package dmx.fun.sample.item;

import dmx.fun.Result;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller demonstrating JSON serialization of dmx-fun types via fun-jackson.
 *
 * <ul>
 *   <li>{@code GET /items/{id}} — returns 200 with the item, or 404 for missing items.
 *       Uses {@link dmx.fun.Option} in the service; the controller maps it to HTTP semantics.</li>
 *   <li>{@code POST /items} — returns {@link Result}{@code <Item,String>} serialized as JSON.
 *       Success: {@code {"ok":{...}}}; failure: {@code {"error":"..."}}.</li>
 *   <li>{@code PUT /items/{id}} — same {@code Result} pattern, demonstrating the declarative
 *       {@link dmx.fun.spring.TransactionalResult} style from the service layer.</li>
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

    @GetMapping("/{id}")
    public ResponseEntity<?> findById(@PathVariable("id") Long id) {
        return service.findById(id)
            .map(ResponseEntity::ok)
            .getOrElse(ResponseEntity.notFound().build());
    }

    // Result<Item,String> is serialized by DmxFunModule:
    //   success → {"ok":{"id":1,"name":"Widget","description":"…"}}
    //   failure → {"err":"name must not be blank"}
    @PostMapping
    public Result<Item, String> create(@RequestBody CreateItemRequest request) {
        return service.create(request.name(), request.description());
    }

    @PutMapping("/{id}")
    public Result<Item, String> update(@PathVariable("id") Long id, @RequestBody UpdateItemRequest request) {
        return service.update(id, request.name());
    }

    public record CreateItemRequest(String name, String description) {}

    public record UpdateItemRequest(String name) {}
}
