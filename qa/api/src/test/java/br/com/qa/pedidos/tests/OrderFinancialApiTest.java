package br.com.qa.pedidos.tests;

import br.com.qa.pedidos.config.ApiConfig;
import br.com.qa.pedidos.support.ApiChecks;
import br.com.qa.pedidos.support.ApiClient;
import br.com.qa.pedidos.support.TestData;
import io.restassured.response.Response;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.api.Test;

/** Requer fixtures com preço específico no MongoDB, conforme docs/MASSAS.md. */
public class OrderFinancialApiTest {

    private static Stream<Arguments> thresholdFixtures() {
        return Stream.of(
                Arguments.of("999,99", "qa.price99999Id", "QA_PRICE_999_99_ID", "999.99", "0.00", "999.99"),
                Arguments.of("1.000,00", "qa.price100000Id", "QA_PRICE_1000_00_ID", "1000.00", "100.00", "900.00"),
                Arguments.of("1.000,01", "qa.price100001Id", "QA_PRICE_1000_01_ID", "1000.01", "100.00", "900.01")
        );
    }

    @ParameterizedTest(name = "subtotal={0}")
    @MethodSource("thresholdFixtures")
    @DisplayName("FIN-01 | Desconto nos limites 999,99 / 1.000,00 / 1.000,01")
    void fin01_fronteiraDesconto(String label, String property, String env, String subtotal,
                                 String discount, String total) {
        String product = ApiConfig.requireFixture(property, env, "produto ativo custando " + subtotal);
        Response catalogProduct = ApiClient.getProduct(product);
        ApiChecks.status(catalogProduct, 200);
        ApiChecks.money(catalogProduct, "currentPrice.value", subtotal);

        Response created = ApiClient.createOrder(TestData.single(product, 1));
        ApiChecks.created(created);
        ApiChecks.money(created, "subtotal", subtotal);
        ApiChecks.money(created, "discount", discount);
        ApiChecks.money(created, "total", total);
    }

    @Test
    @DisplayName("FIN-02 | HALF_UP: 10% de 1.000,05 deve resultar em 100,01")
    void fin02_arredondamentoMeioCentavo() {
        String product = ApiConfig.requireFixture("qa.price100005Id", "QA_PRICE_1000_05_ID",
                "produto ativo com preço R$ 1.000,05");
        Response catalogProduct = ApiClient.getProduct(product);
        ApiChecks.status(catalogProduct, 200);
        ApiChecks.money(catalogProduct, "currentPrice.value", "1000.05");

        Response created = ApiClient.createOrder(TestData.single(product, 1));
        ApiChecks.created(created);
        ApiChecks.money(created, "subtotal", "1000.05");
        ApiChecks.money(created, "discount", "100.01");
        ApiChecks.money(created, "total", "900.04");
    }
}
