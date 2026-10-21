package br.com.qa.pedidos.tests;

import br.com.qa.pedidos.config.ApiConfig;
import br.com.qa.pedidos.support.ApiChecks;
import br.com.qa.pedidos.support.ApiClient;
import io.restassured.response.Response;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CAT-02, cobertura PARCIAL: valida a vigência dos preços já presentes no catálogo.
 * Não é possível testar t0-ε, t0, t1-ε, t1 sem controlar relógio ou disponibilizar seed temporal.
 */
public class CatalogValidityAdditionalApiTest {

    @Test
    @DisplayName("CAT-02 (parcial) | Preço vigente respeita início inclusivo e fim exclusivo")
    void cat02_precoVigenteComJanelaCoerente() {
        verifyCurrentWindow(ApiConfig.product1());
        verifyCurrentWindow(ApiConfig.product2());
    }

    private static void verifyCurrentWindow(String productId) {
        Instant requestedAt = Instant.now();
        Response response = ApiClient.getProduct(productId);
        Instant completedAt = Instant.now();
        ApiChecks.status(response, 200);
        assertEquals("ATIVO", response.jsonPath().getString("status"));
        assertNotNull(response.jsonPath().get("currentPrice"),
                "O seed de referência tem preço vigente para " + productId);

        String from = response.jsonPath().getString("currentPrice.validFrom");
        assertNotNull(from, "validFrom precisa estar presente");
        Instant validFrom = Instant.parse(from);
        // Tolerância curta para pequena diferença entre relógios cliente/servidor.
        assertFalse(validFrom.isAfter(completedAt.plus(2, ChronoUnit.SECONDS)),
                "O início da vigência não pode estar no futuro para um preço vigente");

        String until = response.jsonPath().getString("currentPrice.validTo");
        if (until != null) {
            Instant validTo = Instant.parse(until);
            assertTrue(validTo.isAfter(validFrom), "Janela de vigência deve ter fim após o início");
            assertTrue(validTo.isAfter(requestedAt.minus(2, ChronoUnit.SECONDS)),
                    "validTo é exclusivo: janela expirada não pode ser currentPrice");
        }
        assertEquals("BRL", response.jsonPath().getString("currentPrice.currency"));
    }
}
