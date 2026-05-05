package dmx.fun.sample.item;

import dmx.fun.Result;
import dmx.fun.sample.SampleApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static dmx.fun.assertj.DmxFunAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = SampleApplication.class)
@Testcontainers
class ItemServiceTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
        new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url",      POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired ItemService service;

    // ── create ────────────────────────────────────────────────────────────────

    @Test
    void create_returnsOk_forValidInput() {
        Result<Item, String> result = service.create("Widget", "A fine widget");

        assertThat(result).isOk();
        Item item = result.get();
        assertThat(item.id()).isNotNull();
        assertThat(item.name()).isEqualTo("Widget");
        assertThat(item.description()).isEqualTo("A fine widget");
    }

    @Test
    void create_returnsErr_forBlankName() {
        assertThat(service.create("  ", "desc")).isErr().containsError("name must not be blank");
    }

    @Test
    void create_returnsErr_forNullName() {
        assertThat(service.create(null, "desc")).isErr().containsError("name must not be blank");
    }

    @Test
    void create_stripsWhitespace_fromName() {
        Result<Item, String> result = service.create("  Gadget  ", null);

        assertThat(result).isOk();
        assertThat(result.get().name()).isEqualTo("Gadget");
    }

    // ── update (@TransactionalResult declarative style) ───────────────────────

    @Test
    void update_returnsOk_forExistingItem() {
        Item created = service.create("Original", "desc").get();

        Result<Item, String> result = service.update(created.id(), "Renamed");

        assertThat(result).isOk();
        assertThat(result.get().name()).isEqualTo("Renamed");
    }

    @Test
    void update_returnsErr_forMissingItem() {
        Result<Item, String> result = service.update(-1L, "Ghost");

        assertThat(result).isErr();
        assertThat(result.getError()).contains("item not found");
    }

    @Test
    void update_returnsErr_forBlankName() {
        Item created = service.create("ToUpdate", null).get();

        assertThat(service.update(created.id(), "")).isErr().containsError("name must not be blank");
    }

    // ── findById — Option semantics ───────────────────────────────────────────

    @Test
    void findById_returnsSome_forExistingItem() {
        Item created = service.create("Findable", null).get();

        assertThat(service.findById(created.id())).isSome().containsValue(created);
    }

    @Test
    void findById_returnsNone_forMissingItem() {
        assertThat(service.findById(-1L)).isNone();
    }

    // ── findAll ───────────────────────────────────────────────────────────────

    @Test
    void findAll_returnsAllItems() {
        service.create("Item-" + System.nanoTime(), null);

        assertThat(service.findAll()).isNotEmpty();
    }
}
