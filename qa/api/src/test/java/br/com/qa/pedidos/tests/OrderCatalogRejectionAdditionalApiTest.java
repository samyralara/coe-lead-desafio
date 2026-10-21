package br.com.qa.pedidos.tests;

import br.com.qa.pedidos.config.ApiConfig;
import br.com.qa.pedidos.support.ApiChecks;
import br.com.qa.pedidos.support.ApiClient;
import br.com.qa.pedidos.support.TestData;
import io.restassured.response.Response;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * INT-02: correlação entre resposta de consulta ao catálogo e rejeição do pedido.
 * Produto inexistente = cobertura base. Classes inativo/sem preço só rodam se IDs já existirem.
 */
public class OrderCatalogRejectionAdditionalApiTest {

    @Test
    @DisplayName("INT-02 (parcial) | notFound do catálogo vira 422 e rejeita pedido composto")
    void int02_propagarFalhasSemanticasConhecidas() {
        String missing = TestData.missingProduct();
        Response prices = ApiClient.prices(List.of(ApiConfig.product1(), missing));
        ApiChecks.status(prices, 200);
        assertTrue(prices.jsonPath().getList("notFound", String.class).contains(missing));
        assertTrue(prices.jsonPath().getList("prices.productId", String.class).contains(ApiConfig.product1()));

        Response rejected = ApiClient.createOrder(TestData.order(List.of(
                TestData.item(ApiConfig.product1(), 1), TestData.item(missing, 1))));
        ApiChecks.error(rejected, 422, "PRODUCT_NOT_FOUND");
        List<String> details = rejected.jsonPath().getList("error.details", String.class);
        assertTrue(details.contains(missing), "O ID inválido deve ser identificável na resposta");

        // Reutiliza exclusivamente dados existentes se o ambiente informar os respectivos IDs.
        ApiConfig.value("qa.inactiveProductId", "QA_INACTIVE_PRODUCT_ID")
                .ifPresent(id -> verifyExistingSpecialProduct(id, "PRODUCT_INACTIVE", false));
        ApiConfig.value("qa.noPriceProductId", "QA_NO_PRICE_PRODUCT_ID")
                .ifPresent(id -> verifyExistingSpecialProduct(id, "NO_CURRENT_PRICE", true));
    }

    private static void verifyExistingSpecialProduct(String id, String expectedCode, boolean noCurrentPrice) {
        Response prices = ApiClient.prices(List.of(id));
        ApiChecks.status(prices, 200);
        List<Map<String, Object>> found = prices.jsonPath().getList("prices");
        assertEquals(1, found.size(), "O produto especial precisa existir no catálogo");
        assertEquals(id, found.get(0).get("productId"));
        if (noCurrentPrice) {
            assertNull(found.get(0).get("currentPrice"));
        } else {
            assertEquals("INATIVO", found.get(0).get("status"));
        }
        ApiChecks.error(ApiClient.createOrder(TestData.single(id, 1)), 422, expectedCode);
    }
}
