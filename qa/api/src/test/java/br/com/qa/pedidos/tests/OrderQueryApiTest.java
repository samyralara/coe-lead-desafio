package br.com.qa.pedidos.tests;

import br.com.qa.pedidos.config.ApiConfig;
import br.com.qa.pedidos.support.ApiChecks;
import br.com.qa.pedidos.support.ApiClient;
import br.com.qa.pedidos.support.TestData;
import io.restassured.response.Response;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import static org.junit.jupiter.api.Assertions.*;

/** Casos ORD-01 até ORD-04 da matriz sintetizada. */
public class OrderQueryApiTest {

    @Test
    @Tag("smoke")
    @DisplayName("ORD-01 | Consultar pedido existente e inexistente")
    void ord01_consultaPorId() {
        Response created = ApiClient.createOrder(TestData.single(ApiConfig.product1(), 1));
        ApiChecks.created(created);
        String id = created.jsonPath().getString("id");
        Response fetched = ApiClient.getOrder(id);
        ApiChecks.status(fetched, 200);
        assertEquals(id, fetched.jsonPath().getString("id"));
        ApiChecks.money(fetched, "subtotal", "3500.00");
        ApiChecks.error(ApiClient.getOrder(UUID.randomUUID().toString()), 404, "ORDER_NOT_FOUND");
    }

    @Test
    @Tag("smoke")
    @DisplayName("ORD-02 | Paginação padrão e página além do último registro")
    void ord02_paginacao() {
        Response defaultPage = ApiClient.orderRequest().get(ApiConfig.ORDER_PATH);
        ApiChecks.status(defaultPage, 200);
        assertEquals(0, defaultPage.jsonPath().getInt("page"));
        assertEquals(20, defaultPage.jsonPath().getInt("size"));
        assertTrue(defaultPage.jsonPath().getList("content").size() <= 20);

        Response first = ApiClient.listOrders(0, 1);
        ApiChecks.status(first, 200);
        assertEquals(1, first.jsonPath().getInt("size"));
        int totalPages = first.jsonPath().getInt("totalPages");
        Response beyondLast = ApiClient.listOrders(totalPages + 1, 1);
        ApiChecks.status(beyondLast, 200);
        assertTrue(beyondLast.jsonPath().getList("content").isEmpty());
        assertEquals(totalPages + 1, beyondLast.jsonPath().getInt("page"));
    }

    @Test
    @DisplayName("ORD-03 | Filtro por status CRIADO e status inválido")
    void ord03_filtroStatus() {
        Response valid = ApiClient.orderRequest().queryParam("status", "CRIADO")
                .get(ApiConfig.ORDER_PATH);
        ApiChecks.status(valid, 200);
        assertTrue(valid.jsonPath().getList("content.status", String.class)
                .stream().allMatch("CRIADO"::equals));

        Response invalid = ApiClient.orderRequest().queryParam("status", "INVALIDO")
                .get(ApiConfig.ORDER_PATH);
        ApiChecks.error(invalid, 400, "VALIDATION_ERROR");
    }

    private static Stream<Arguments> pageSizes() {
        return Stream.of(Arguments.of(0, 400), Arguments.of(1, 200),
                Arguments.of(100, 200), Arguments.of(101, 400));
    }

    @ParameterizedTest(name = "size={0} => HTTP {1}")
    @MethodSource("pageSizes")
    @DisplayName("ORD-04 | AVL size da página: 0/1/100/101")
    void ord04_limitesSize(int size, int expectedStatus) {
        Response response = ApiClient.listOrders(0, size);
        if (expectedStatus == 400) ApiChecks.error(response, 400, "VALIDATION_ERROR");
        else {
            ApiChecks.status(response, 200);
            assertEquals(size, response.jsonPath().getInt("size"));
            assertTrue(response.jsonPath().getList("content").size() <= size);
        }
    }
}
