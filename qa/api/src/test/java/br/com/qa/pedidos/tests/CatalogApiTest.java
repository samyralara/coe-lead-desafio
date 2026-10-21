package br.com.qa.pedidos.tests;

import br.com.qa.pedidos.config.ApiConfig;
import br.com.qa.pedidos.support.ApiChecks;
import br.com.qa.pedidos.support.ApiClient;
import br.com.qa.pedidos.support.TestData;
import io.restassured.response.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import static org.junit.jupiter.api.Assertions.*;

/** Casos CAT-01, CAT-03 e CAT-04. */
public class CatalogApiTest {

    @Test
    @Tag("smoke")
    @DisplayName("CAT-01 | Produto vigente, inexistente e, se disponível, inativo")
    void cat01_produtosPorId() {
        Response valid = ApiClient.getProduct(ApiConfig.product1());
        ApiChecks.status(valid, 200);
        assertEquals("ATIVO", valid.jsonPath().getString("status"));
        ApiChecks.money(valid, "currentPrice.value", "3500.00");
        assertEquals("BRL", valid.jsonPath().getString("currentPrice.currency"));

        ApiChecks.error(ApiClient.getProduct(TestData.missingProduct()), 404, "PRODUCT_NOT_FOUND");

        // Massa opcional; não há identificação do produto inativo nos arquivos anexados.
        ApiConfig.value("qa.inactiveProductId", "QA_INACTIVE_PRODUCT_ID").ifPresent(id -> {
            Response inactive = ApiClient.getProduct(id);
            ApiChecks.status(inactive, 200);
            assertEquals("INATIVO", inactive.jsonPath().getString("status"));
        });
    }

    @Test
    @Tag("smoke")
    @DisplayName("CAT-03 | Preço em lote: produtos encontrados e notFound")
    void cat03_loteMisto() {
        String missing = TestData.missingProduct();
        Response response = ApiClient.prices(List.of(ApiConfig.product1(), ApiConfig.product2(), missing));
        ApiChecks.status(response, 200);
        assertEquals(2, response.jsonPath().getList("prices").size());
        assertTrue(response.jsonPath().getList("prices.productId", String.class)
                .containsAll(List.of(ApiConfig.product1(), ApiConfig.product2())));
        assertTrue(response.jsonPath().getList("notFound", String.class).contains(missing));
    }

    private static Stream<Arguments> batches() {
        return Stream.of(
                Arguments.of("1 ID existente", 1, false, 200),
                Arguments.of("49 IDs inéditos", 49, false, 200),
                Arguments.of("50 IDs inéditos", 50, false, 200),
                Arguments.of("51 IDs", 51, false, 400),
                Arguments.of("IDs duplicados", 2, true, 400)
        );
    }

    @ParameterizedTest(name = "{0} => HTTP {3}")
    @MethodSource("batches")
    @DisplayName("CAT-04 | AVL máximo de 50 IDs e unicidade no batch")
    void cat04_limitesEUnicidadeDoLote(String label, int count, boolean duplicated, int expectedStatus) {
        List<String> ids = new ArrayList<>();
        if (duplicated) {
            ids.add(ApiConfig.product1());
            ids.add(ApiConfig.product1());
        } else if (count == 1) {
            ids.add(ApiConfig.product1());
        } else {
            for (int i = 0; i < count; i++) ids.add("QA-MISSING-" + UUID.randomUUID());
        }
        Response response = ApiClient.prices(ids);
        if (expectedStatus == 400) {
            ApiChecks.error(response, 400, "VALIDATION_ERROR");
        } else {
            ApiChecks.status(response, 200);
            assertEquals(count, response.jsonPath().getList("prices").size()
                    + response.jsonPath().getList("notFound").size());
        }
        // Lote de 0 IDs não é imposto aqui: contrato só estabelece máximo 50 e ausência de duplicatas.
    }
}
