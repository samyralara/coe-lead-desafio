package br.com.qa.pedidos.tests;

import br.com.qa.pedidos.config.ApiConfig;
import br.com.qa.pedidos.support.ApiChecks;
import br.com.qa.pedidos.support.ApiClient;
import br.com.qa.pedidos.support.TestData;
import io.restassured.response.Response;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import static org.junit.jupiter.api.Assertions.*;

/** Casos PED-01 a PED-10 da matriz otimizada (10 casos lógicos). */
public class OrderCreationApiTest {

    @Test
    @Tag("smoke")
    @DisplayName("PED-01 | Criar pedido válido e verificar cálculo, Location e consulta")
    void ped01_criarPedidoValido() {
        Response created = ApiClient.createOrder(TestData.nominal());
        ApiChecks.created(created);
        assertEquals(2, created.jsonPath().getList("items").size());
        assertEquals(ApiConfig.product1(), created.jsonPath().getString("items[0].productId"));
        assertEquals(2, created.jsonPath().getInt("items[0].quantity"));
        ApiChecks.money(created, "items[0].unitPrice", "3500.00");
        ApiChecks.money(created, "items[0].lineTotal", "7000.00");
        ApiChecks.money(created, "items[1].unitPrice", "499.90");
        ApiChecks.money(created, "items[1].lineTotal", "499.90");
        ApiChecks.money(created, "subtotal", "7499.90");
        ApiChecks.money(created, "discount", "749.99");
        ApiChecks.money(created, "total", "6749.91");
        String id = created.jsonPath().getString("id");
        Response read = ApiClient.getOrder(id);
        ApiChecks.status(read, 200);
        ApiChecks.money(read, "total", "6749.91");
        assertEquals(id, read.jsonPath().getString("id"));
    }

    private static Stream<Arguments> invalidClients() {
        return Stream.of(
                Arguments.of("nome ausente", "name", null, true),
                Arguments.of("nome vazio", "name", "", false),
                Arguments.of("documento ausente", "document", null, true),
                Arguments.of("e-mail ausente", "email", null, true),
                Arguments.of("e-mail inválido", "email", "invalido", false),
                Arguments.of("cliente ausente", "client", null, true)
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidClients")
    @DisplayName("PED-02 | Partições inválidas de cliente")
    void ped02_camposClienteInvalidos(String label, String field, String value, boolean remove) {
        Map<String, Object> payload = TestData.single(ApiConfig.product1(), 1);
        if ("client".equals(field)) {
            payload.remove("client");
        } else {
            @SuppressWarnings("unchecked")
            Map<String, Object> client = (Map<String, Object>) payload.get("client");
            if (remove) client.remove(field); else client.put(field, value);
        }
        ApiChecks.error(ApiClient.createOrder(payload), 400, "VALIDATION_ERROR");
    }

    private static Stream<Arguments> itemCounts() {
        return Stream.of(Arguments.of(0, 400), Arguments.of(1, 201), Arguments.of(101, 400));
    }

    @ParameterizedTest(name = "items={0} => HTTP {1}")
    @MethodSource("itemCounts")
    @DisplayName("PED-03 | AVL quantidade de itens: 0 / 1 / 101")
    void ped03_quantidadeItens(int count, int status) {
        Map<String, Object> payload = count == 1
                ? TestData.single(ApiConfig.product1(), 1)
                : TestData.order(TestData.distinctItems(count));
        Response response = ApiClient.createOrder(payload);
        if (status == 201) ApiChecks.created(response);
        else ApiChecks.error(response, 400, "VALIDATION_ERROR");
        // 51 a 100 itens exigem decisão sobre limite do batch 50 no catálogo (GAP-01).
    }

    private static Stream<Arguments> quantities() {
        return Stream.of(-1, 0, 1, 2, 998, 999, 1000)
                .map(n -> Arguments.of(n, n >= 1 && n <= 999));
    }

    @ParameterizedTest(name = "quantity={0}, válida={1}")
    @MethodSource("quantities")
    @DisplayName("PED-04 | AVL quantidade por item -1/0/1/2/998/999/1000")
    void ped04_limitesQuantidade(int quantity, boolean valid) {
        Response response = ApiClient.createOrder(TestData.single(ApiConfig.product1(), quantity));
        if (!valid) {
            ApiChecks.error(response, 400, "VALIDATION_ERROR");
            return;
        }
        ApiChecks.created(response);
        assertEquals(quantity, response.jsonPath().getInt("items[0].quantity"));
        BigDecimal expectedLine = new BigDecimal("3500.00").multiply(BigDecimal.valueOf(quantity));
        assertEquals(0, ApiChecks.decimal(response, "items[0].lineTotal").compareTo(expectedLine));
    }

    private static Stream<Arguments> productIdLengths() {
        return Stream.of(
                Arguments.of("ausente", -1, 400),
                Arguments.of("vazio", 0, 400),
                Arguments.of("1 caractere", 1, 422),
                Arguments.of("49 caracteres", 49, 422),
                Arguments.of("50 caracteres", 50, 422),
                Arguments.of("51 caracteres", 51, 400)
        );
    }

    @ParameterizedTest(name = "{0} => HTTP {2}")
    @MethodSource("productIdLengths")
    @DisplayName("PED-05 | PE e AVL do productId (1 a 50 caracteres)")
    void ped05_limitesIdProduto(String label, int length, int expectedHttp) {
        Map<String, Object> payload = TestData.single(ApiConfig.product1(), 1);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items = (List<Map<String, Object>>) payload.get("items");
        Map<String, Object> entry = new LinkedHashMap<>(items.get(0));
        if (length == -1) entry.remove("productId");
        else entry.put("productId", "Z".repeat(length));
        payload.put("items", List.of(entry));
        String expectedCode = expectedHttp == 400 ? "VALIDATION_ERROR" : "PRODUCT_NOT_FOUND";
        ApiChecks.error(ApiClient.createOrder(payload), expectedHttp, expectedCode);
    }

    @Test
    @DisplayName("PED-06 | Produtos duplicados são rejeitados")
    void ped06_productIdDuplicado() {
        Map<String, Object> payload = TestData.order(List.of(
                TestData.item(ApiConfig.product1(), 1), TestData.item(ApiConfig.product1(), 2)));
        ApiChecks.error(ApiClient.createOrder(payload), 400, "VALIDATION_ERROR");
    }

    @Test
    @DisplayName("PED-07 | Payload acima de 1 MB deve ser rejeitado antes da desserialização")
    void ped07_payloadGrande() {
        // 1.100.000 bytes excede 1 MB decimal e 1 MiB binário; independe de interpretação de MB.
        String oversized = TestData.paddedOrderOfExactSize(1_100_000);
        Response rejected = ApiClient.orderRequest().body(oversized).post(ApiConfig.ORDER_PATH);
        ApiChecks.error(rejected, 413, "PAYLOAD_TOO_LARGE");

        // Limites L-1 e L requerem confirmação da configuração em bytes (não definida no contrato).
        ApiConfig.value("qa.maxPayloadBytes", "QA_MAX_PAYLOAD_BYTES").ifPresent(limit -> {
            int bytes = Integer.parseInt(limit);
            for (int size : List.of(bytes - 1, bytes)) {
                Response r = ApiClient.orderRequest()
                        .body(TestData.paddedOrderOfExactSize(size)).post(ApiConfig.ORDER_PATH);
                assertNotEquals(413, r.statusCode(),
                        "Payload <= L não pode ser rejeitado por tamanho. tamanho=" + size + " corpo=" + r.asString());
            }
            Response r = ApiClient.orderRequest()
                    .body(TestData.paddedOrderOfExactSize(bytes + 1)).post(ApiConfig.ORDER_PATH);
            ApiChecks.error(r, 413, "PAYLOAD_TOO_LARGE");
        });
    }

    @Test
    @Tag("smoke")
    @DisplayName("PED-08 | JSON malformado retorna 400 padronizado")
    void ped08_jsonMalformado() {
        Response response = ApiClient.orderRequest()
                .body("{\"client\":{\"name\":\"Sem fechamento\",\"items\":[")
                .post(ApiConfig.ORDER_PATH);
        ApiChecks.error(response, 400, "VALIDATION_ERROR");
    }

    private static Stream<Arguments> invalidProducts() {
        return Stream.of(
                Arguments.of("inexistente", "", "PRODUCT_NOT_FOUND"),
                Arguments.of("inativo", "qa.inactiveProductId", "PRODUCT_INACTIVE"),
                Arguments.of("sem preço vigente", "qa.noPriceProductId", "NO_CURRENT_PRICE")
        );
    }

    @ParameterizedTest(name = "{0} => {2}")
    @MethodSource("invalidProducts")
    @DisplayName("PED-09 | Produtos inexistentes, inativos e sem preço vigente")
    void ped09_produtoInvalido(String classification, String fixtureProperty, String code) {
        String id;
        if (fixtureProperty.isEmpty()) id = TestData.missingProduct();
        else if ("qa.inactiveProductId".equals(fixtureProperty)) {
            id = ApiConfig.requireFixture(fixtureProperty, "QA_INACTIVE_PRODUCT_ID", "produto INATIVO");
        } else {
            id = ApiConfig.requireFixture(fixtureProperty, "QA_NO_PRICE_PRODUCT_ID", "produto sem preço vigente");
        }
        ApiChecks.error(ApiClient.createOrder(TestData.single(id, 1)), 422, code);
    }

    @Test
    @DisplayName("PED-10 | Composição ativo + inexistente rejeita o pedido integral")
    void ped10_allOrNothing() {
        // A contagem é uma evidência pela API; executar somente em ambiente QA sem escritas paralelas.
        long before = ApiChecks.totalOrders();
        Map<String, Object> payload = TestData.order(List.of(
                TestData.item(ApiConfig.product1(), 1), TestData.item(TestData.missingProduct(), 1)));
        ApiChecks.error(ApiClient.createOrder(payload), 422, "PRODUCT_NOT_FOUND");
        assertEquals(before, ApiChecks.totalOrders(),
                "Não pode existir pedido adicional após uma criação rejeitada");
    }
}
