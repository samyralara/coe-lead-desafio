package br.com.qa.pedidos.tests;

import br.com.qa.pedidos.config.ApiConfig;
import br.com.qa.pedidos.support.ApiChecks;
import br.com.qa.pedidos.support.ApiClient;
import io.restassured.response.Response;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** INT-06: valida X-Request-Id no Order Service e no Catalog Service. */
public class RequestTracingApiTest {

    @Test
    @Tag("smoke")
    @DisplayName("INT-06 | Request ID informado e gerado é ecoado no header e envelope")
    void int06_rastreabilidade() {
        String id = UUID.randomUUID().toString();
        Response ownRequestId = ApiClient.orderRequest()
                .header("X-Request-Id", id)
                .get(ApiConfig.ORDER_PATH + "/" + UUID.randomUUID());
        ApiChecks.error(ownRequestId, 404, "ORDER_NOT_FOUND");
        assertEquals(id, ownRequestId.getHeader("X-Request-Id"));
        assertEquals(id, ownRequestId.jsonPath().getString("requestId"));

        Response generated = ApiClient.orderRequest()
                .get(ApiConfig.ORDER_PATH + "/" + UUID.randomUUID());
        ApiChecks.error(generated, 404, "ORDER_NOT_FOUND");
        String generatedId = generated.getHeader("X-Request-Id");
        ApiChecks.validUuid(generatedId);
        assertEquals(generatedId, generated.jsonPath().getString("requestId"));

        String catalogId = UUID.randomUUID().toString();
        Response fromCatalog = ApiClient.catalogRequest()
                .header("X-Request-Id", catalogId)
                .get(ApiConfig.PRODUCT_PATH + "/QA-MISSING-" + UUID.randomUUID());
        ApiChecks.error(fromCatalog, 404, "PRODUCT_NOT_FOUND");
        assertEquals(catalogId, fromCatalog.getHeader("X-Request-Id"));
        assertEquals(catalogId, fromCatalog.jsonPath().getString("requestId"));
    }
}
