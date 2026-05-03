package dmx.fun.sample.item;

import dmx.fun.sample.SampleApplication;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for the REST layer.
 *
 * <p>Verifies HTTP mapping for dmx-fun return types in Spring MVC:
 * {@link dmx.fun.Option} maps to 200/404 and {@link dmx.fun.Result}
 * maps to 200/500 with the unwrapped body.
 */
@SpringBootTest(classes = SampleApplication.class)
@Testcontainers
class ItemControllerTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
        new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url",      POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired WebApplicationContext context;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    // ── GET /items/{id} ───────────────────────────────────────────────────────

    @Test
    void getItem_returns404_forMissingItem() throws Exception {
        mockMvc.perform(get("/items/-1"))
            .andExpect(status().isNotFound());
    }

    @Test
    void getItem_returns200_forExistingItem() throws Exception {
        long id = createItemAndExtractId("GetMe", "desc");

        mockMvc.perform(get("/items/" + id))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("GetMe"));
    }

    // ── POST /items ───────────────────────────────────────────────────────────

    @Test
    void createItem_returnsOkResult_asJson() throws Exception {
        mockMvc.perform(post("/items")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Widget\",\"description\":\"A fine widget\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Widget"))
            .andExpect(jsonPath("$.description").value("A fine widget"))
            .andExpect(jsonPath("$.id").isNumber());
    }

    @Test
    void createItem_returnsErrResult_forBlankName() throws Exception {
        mockMvc.perform(post("/items")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"\",\"description\":\"desc\"}"))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$").value("name must not be blank"));
    }

    // ── PUT /items/{id} ───────────────────────────────────────────────────────

    @Test
    void updateItem_returnsOkResult_forExistingItem() throws Exception {
        long id = createItemAndExtractId("Original", null);

        mockMvc.perform(put("/items/" + id)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Renamed\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Renamed"));
    }

    @Test
    void updateItem_returnsErrResult_forMissingItem() throws Exception {
        mockMvc.perform(put("/items/-1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Ghost\"}"))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$", containsString("item not found")));
    }

    // ── GET /items ────────────────────────────────────────────────────────────

    @Test
    void findAll_returnsJsonArray() throws Exception {
        createItemAndExtractId("Listed", null);

        mockMvc.perform(get("/items"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray());
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private long createItemAndExtractId(String name, String description) throws Exception {
        String descJson = description == null ? "null" : "\"" + description + "\"";
        String body = mockMvc.perform(post("/items")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"" + name + "\",\"description\":" + descJson + "}"))
            .andExpect(jsonPath("$.id").isNumber())
            .andReturn()
            .getResponse()
            .getContentAsString();

        // body looks like {"id":42,...}
        int start = body.indexOf("\"id\":") + 5;
        int end   = body.indexOf(",", start);
        if (end < 0) end = body.indexOf("}", start);
        return Long.parseLong(body.substring(start, end).trim());
    }
}
