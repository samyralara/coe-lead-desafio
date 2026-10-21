package br.com.qa.pedidos.tests;

import br.com.qa.pedidos.config.ApiConfig;
import br.com.qa.pedidos.support.ApiChecks;
import br.com.qa.pedidos.support.ApiClient;
import io.restassured.response.Response;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** ORD-05: não cria massa; inspeciona as páginas já existentes. */
public class OrderListingAdditionalApiTest {

    @Test
    @DisplayName("ORD-05 | Listagem ordenada por createdAt decrescente, inclusive entre páginas")
    void ord05_ordenacaoPorCriacaoDecrescente() {
        Response firstPage = ApiClient.listOrders(0, 100);
        ApiChecks.status(firstPage, 200);
        List<Map<String, Object>> firstContent = firstPage.jsonPath().getList("content");
        assertNotNull(firstContent, "O campo content é obrigatório");
        int totalPages = firstPage.jsonPath().getInt("totalPages");

        List<Instant> timestamps = new ArrayList<>();
        appendTimestamps(firstContent, timestamps);
        if (totalPages > 1) {
            Response secondPage = ApiClient.listOrders(1, 100);
            ApiChecks.status(secondPage, 200);
            appendTimestamps(secondPage.jsonPath().getList("content"), timestamps);
        }

        Assumptions.assumeTrue(timestamps.size() >= 2,
                "ORD-05 não executável: ao menos 2 pedidos devem existir no ambiente; não criar massa nova");
        for (int i = 1; i < timestamps.size(); i++) {
            Instant older = timestamps.get(i);
            Instant newer = timestamps.get(i - 1);
            assertFalse(older.isAfter(newer), () ->
                    "A listagem deve ser decrescente por createdAt. Sequência=" + timestamps);
        }
    }

    private static void appendTimestamps(List<Map<String, Object>> content, List<Instant> timestamps) {
        assertNotNull(content, "content não pode ser null");
        for (Map<String, Object> order : content) {
            Object createdAt = order.get("createdAt");
            assertNotNull(createdAt, "createdAt é obrigatório para validar ordenação");
            timestamps.add(Instant.parse(String.valueOf(createdAt)));
        }
    }
}
