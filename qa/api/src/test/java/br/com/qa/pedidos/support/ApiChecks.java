package br.com.qa.pedidos.support;

import io.restassured.response.Response;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

public final class ApiChecks {
    private ApiChecks() { }

    public static void status(Response response, int expected) {
        assertEquals(expected, response.statusCode(), () -> "Resposta HTTP inesperada: " + response.asString());
    }

    public static void error(Response response, int httpStatus, String code) {
        status(response, httpStatus);
        assertEquals(code, response.jsonPath().getString("error.code"), response.asString());
        assertNotNull(response.jsonPath().getString("error.message"), "error.message obrigatório");
        assertTrue(response.jsonPath().get("error.details") instanceof List, "error.details deve ser array");
        validUuid(response.jsonPath().getString("requestId"));
    }

    public static void validUuid(String value) {
        assertNotNull(value, "UUID obrigatório");
        assertDoesNotThrow(() -> UUID.fromString(value), "UUID inválido: " + value);
    }

    public static BigDecimal decimal(Response response, String path) {
        String value = response.jsonPath().getString(path);
        assertNotNull(value, "Campo monetário ausente: " + path);
        return new BigDecimal(value);
    }

    public static void money(Response response, String path, String expected) {
        BigDecimal actual = decimal(response, path);
        assertEquals(0, actual.compareTo(new BigDecimal(expected)),
                () -> path + ": esperado " + expected + ", obtido " + actual);
    }

    public static void created(Response response) {
        status(response, 201);
        String id = response.jsonPath().getString("id");
        validUuid(id);
        assertEquals("CRIADO", response.jsonPath().getString("status"));
        String location = response.getHeader("Location");
        assertNotNull(location, "Location obrigatório em 201");
        assertTrue(location.endsWith("/api/v1/orders/" + id), "Location não aponta para o pedido criado");
        assertNotNull(response.jsonPath().getString("createdAt"));
    }

    public static long totalOrders() {
        Response response = ApiClient.listOrders(0, 1);
        status(response, 200);
        return response.jsonPath().getLong("totalElements");
    }
}
