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
 * DB-01, cobertura PARCIAL pela API: confirma leitura após escrita e rejeição all-or-nothing.
 * Sem credenciais JDBC não se pode garantir inexistência de linhas órfãs em order_items.
 */
public class OrderPersistenceObservableAdditionalApiTest {

    @Test
    @DisplayName("DB-01 (parcial) | Persistência observável por GET e rejeição atômica pela API")
    void db01_persistenciaObservavelPelaApi() {
        Response created = ApiClient.createOrder(TestData.nominal());
        ApiChecks.created(created);
        String id = created.jsonPath().getString("id");

        Response stored = ApiClient.getOrder(id);
        ApiChecks.status(stored, 200);
        assertEquals(id, stored.jsonPath().getString("id"));
        assertEquals(2, stored.jsonPath().getList("items").size());
        ApiChecks.money(stored, "items[0].unitPrice", "3500.00");
        ApiChecks.money(stored, "items[0].lineTotal", "7000.00");
        ApiChecks.money(stored, "items[1].lineTotal", "499.90");
        ApiChecks.money(stored, "total", "6749.91");

        // Uma transação com item válido + item ausente deve falhar integralmente.
        // Sem acesso de leitura ao PostgreSQL a ausência de órfãos não é demonstrável.
        long before = ApiChecks.totalOrders();
        Map<String, Object> invalid = TestData.order(List.of(
                TestData.item(ApiConfig.product1(), 1),
                TestData.item(TestData.missingProduct(), 1)));
        Response rejected = ApiClient.createOrder(invalid);
        ApiChecks.error(rejected, 422, "PRODUCT_NOT_FOUND");
        assertEquals(before, ApiChecks.totalOrders(),
                "A API não deve mostrar pedidos adicionais após a criação rejeitada (ambiente sem outras escritas)");
    }
}
