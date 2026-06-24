package com.ordersystem.orderservice.infrastructure.web;

import com.jayway.jsonpath.JsonPath;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// WebEnvironment.MOCK levanta el contexto completo pero sin servidor HTTP real
// — MockMvc simula las requests directamente contra los controllers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
class OrderControllerIntegrationTest {

    // El contenedor arranca en el bloque estático — antes de que Spring inicie el contexto.
    // Así @DynamicPropertySource puede leer la URL/credenciales del contenedor ya corriendo.
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15");

    static {
        postgres.start();
    }

    // Inyecta las propiedades del contenedor en el contexto de Spring antes de que arranque.
    // Sobreescribe spring.datasource.* del application.yaml solo para estos tests.
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    WebApplicationContext context;

    MockMvc mockMvc;

    // MockMvcBuilders.webAppContextSetup inyecta los filtros y configuración real de Spring
    // (validaciones, exception handlers, etc.) — equivalente a @AutoConfigureMockMvc de Boot 3.x
    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    // ─── POST /orders ─────────────────────────────────────────────────────────

    @Test
    void createOrder_shouldReturn201_withCreatedOrder() throws Exception {
        // text block de Java — JSON sin necesidad de ObjectMapper
        String body = """
                {"customerId": "customer-1", "productId": "product-5", "quantity": 3}
                """;

        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.customerId").value("customer-1"))
                .andExpect(jsonPath("$.productId").value("product-5"))
                .andExpect(jsonPath("$.quantity").value(3));
    }

    @Test
    void createOrder_shouldPersistOrder_retrievableByGet() throws Exception {
        String body = """
                {"customerId": "customer-2", "productId": "product-3", "quantity": 1}
                """;

        // Paso 1 — creamos la orden y extraemos el id con JsonPath
        String responseBody = mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        // JsonPath.read extrae el valor del campo "id" del JSON de respuesta
        String id = JsonPath.read(responseBody, "$.id");

        // Paso 2 — GET con ese id debe retornar la misma orden persistida en DB
        mockMvc.perform(get("/orders/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.customerId").value("customer-2"));
    }

    // ─── GET /orders/{id} ────────────────────────────────────────────────────

    @Test
    void getOrder_shouldReturn404_whenOrderNotFound() throws Exception {
        mockMvc.perform(get("/orders/00000000-0000-0000-0000-000000000000"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("RESOURCE_NOT_FOUND"));
    }

    // ─── Validaciones ─────────────────────────────────────────────────────────

    @Test
    void createOrder_shouldReturn400_whenQuantityIsZero() throws Exception {
        String body = """
                {"customerId": "customer-1", "productId": "product-5", "quantity": 0}
                """;

        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    void createOrder_shouldReturn400_whenCustomerIdIsBlank() throws Exception {
        String body = """
                {"customerId": "", "productId": "product-5", "quantity": 3}
                """;

        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }
}
