package br.com.qa.pedidos.tests;

import br.com.qa.pedidos.support.ApiChecks;
import br.com.qa.pedidos.support.ApiClient;
import br.com.qa.pedidos.support.TestData;
import io.restassured.response.Response;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import static org.junit.jupiter.api.Assertions.*;

/**
 * INT-08 / GAP-01 — conflito entre até 100 itens no pedido e lote máximo 50 no catálogo.
 * Teste de contrato PROPOSTO (red test até definição/correção da API).
 * Não cria/edita massa no catálogo: gera IDs distintos propositalmente inexistentes.
 * Isso valida tratamento e atomicidade observável, NÃO sucesso para 51-100 produtos válidos.
 */
public class OrderCatalogBatchLimitApiTest {

    static Stream<Arguments> limites() {
        return Stream.of(
                Arguments.of(50, 422, "PRODUCT_NOT_FOUND"), // dentro do lote
                Arguments.of(51, 422, "PRODUCT_NOT_FOUND"), // primeiro além do lote
                Arguments.of(100, 422, "PRODUCT_NOT_FOUND"), // máximo do pedido
                Arguments.of(101, 400, "VALIDATION_ERROR") // excede limite do pedido
        );
    }

    @Tag("contract-gap")
    @ParameterizedTest(name = "{0} itens distintos -> {1} / {2}")
    @MethodSource("limites")
    @DisplayName("INT-08 | AVL 50, 51, 100 e 101 itens distintos sem massa adicional")
    void int08_limitesPedidoVsLoteCatalogo(int quantidade, int httpEsperado, String codigoEsperado) {
        // Não executa consulta prévia nem modifica seed; a criação rejeitada não deve persistir.
        Response resposta = ApiClient.createOrder(TestData.order(TestData.distinctItems(quantidade)));
        ApiChecks.error(resposta, httpEsperado, codigoEsperado);
        assertNull(resposta.getHeader("Location"), "Operação rejeitada não pode devolver Location de pedido criado");
    }
}
