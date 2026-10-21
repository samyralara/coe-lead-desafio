package br.com.qa.pedidos.tests;

import br.com.qa.pedidos.config.ApiConfig;
import br.com.qa.pedidos.support.ApiChecks;
import br.com.qa.pedidos.support.ApiClient;
import br.com.qa.pedidos.support.TestData;
import io.restassured.response.Response;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** FIN-03: usa PROD-001 e PROD-002 do seed, sem alterar o catálogo. */
public class OrderFinancialAdditionalApiTest {

    @Test
    @Tag("smoke")
    @DisplayName("FIN-03 | Compor subtotal a partir de itens e quantidades diferentes")
    void fin03_composicaoMonetariaDePedido() {
        // O seed original documenta: PROD-001=3500.00 e PROD-002=499.90.
        Response firstProduct = ApiClient.getProduct(ApiConfig.product1());
        Response secondProduct = ApiClient.getProduct(ApiConfig.product2());
        ApiChecks.status(firstProduct, 200);
        ApiChecks.status(secondProduct, 200);
        ApiChecks.money(firstProduct, "currentPrice.value", "3500.00");
        ApiChecks.money(secondProduct, "currentPrice.value", "499.90");

        Response created = ApiClient.createOrder(TestData.nominal());
        ApiChecks.created(created);

        Map<String, BigDecimal> expectedUnitPrices = Map.of(
                ApiConfig.product1(), new BigDecimal("3500.00"),
                ApiConfig.product2(), new BigDecimal("499.90"));
        Map<String, Integer> expectedQuantities = Map.of(
                ApiConfig.product1(), 2, ApiConfig.product2(), 1);

        List<Map<String, Object>> items = created.jsonPath().getList("items");
        assertNotNull(items);
        assertEquals(2, items.size(), "O pedido deve preservar os dois itens");
        BigDecimal calculatedSubtotal = BigDecimal.ZERO;
        for (Map<String, Object> item : items) {
            String productId = String.valueOf(item.get("productId"));
            assertTrue(expectedUnitPrices.containsKey(productId), "Produto inesperado: " + productId);
            BigDecimal price = expectedUnitPrices.get(productId);
            int quantity = ((Number) item.get("quantity")).intValue();
            assertEquals(expectedQuantities.get(productId).intValue(), quantity);
            BigDecimal line = price.multiply(BigDecimal.valueOf(quantity));
            assertMoney(item.get("unitPrice"), price);
            assertMoney(item.get("lineTotal"), line);
            calculatedSubtotal = calculatedSubtotal.add(line);
        }

        assertEquals(0, calculatedSubtotal.compareTo(new BigDecimal("7499.90")));
        BigDecimal expectedDiscount = calculatedSubtotal.multiply(new BigDecimal("0.10"))
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal expectedTotal = calculatedSubtotal.subtract(expectedDiscount);
        ApiChecks.money(created, "subtotal", calculatedSubtotal.toPlainString());
        ApiChecks.money(created, "discount", expectedDiscount.toPlainString());
        ApiChecks.money(created, "total", expectedTotal.toPlainString());

        // Confirma que a resposta persistida e recuperada mantém os mesmos valores.
        Response retrieved = ApiClient.getOrder(created.jsonPath().getString("id"));
        ApiChecks.status(retrieved, 200);
        ApiChecks.money(retrieved, "subtotal", "7499.90");
        ApiChecks.money(retrieved, "discount", "749.99");
        ApiChecks.money(retrieved, "total", "6749.91");
    }

    private static void assertMoney(Object actual, BigDecimal expected) {
        assertNotNull(actual, "Valor monetário ausente");
        assertEquals(0, new BigDecimal(String.valueOf(actual)).compareTo(expected));
    }
}
