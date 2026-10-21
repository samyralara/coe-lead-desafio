package br.com.qa.pedidos.tests;

import br.com.qa.pedidos.config.ApiConfig;
import br.com.qa.pedidos.support.ApiChecks;
import br.com.qa.pedidos.support.ApiClient;
import br.com.qa.pedidos.support.TestData;
import io.restassured.response.Response;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** INT-07: cria pedidos regulares usando APENAS os produtos já existentes do seed. */
public class ConcurrentOrdersAdditionalApiTest {

    @Test
    @DisplayName("INT-07 | Três pedidos concorrentes não misturam clientes, itens e totais")
    void int07_criacaoConcorrenteSemContaminacao() throws Exception {
        int count = 3;
        CountDownLatch allReady = new CountDownLatch(count);
        CountDownLatch release = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(count);
        try {
            List<Future<Result>> futures = new ArrayList<>();
            for (int n = 1; n <= count; n++) {
                int quantity = n;
                Map<String, Object> payload = TestData.single(ApiConfig.product1(), quantity);
                @SuppressWarnings("unchecked")
                Map<String, Object> client = (Map<String, Object>) payload.get("client");
                String expectedEmail = String.valueOf(client.get("email"));
                Callable<Result> task = () -> {
                    allReady.countDown();
                    if (!release.await(10, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("A criação concorrente não foi liberada");
                    }
                    return new Result(quantity, expectedEmail, ApiClient.createOrder(payload));
                };
                futures.add(pool.submit(task));
            }
            assertTrue(allReady.await(10, TimeUnit.SECONDS), "Threads não ficaram prontas");
            release.countDown();

            Set<String> createdIds = new HashSet<>();
            Set<String> emails = new HashSet<>();
            for (Future<Result> future : futures) {
                Result result = future.get(30, TimeUnit.SECONDS);
                Response created = result.response();
                ApiChecks.created(created);
                String id = created.jsonPath().getString("id");
                assertTrue(createdIds.add(id), "IDs não podem se repetir");
                assertTrue(emails.add(result.email()), "Cada pedido deve ter cliente distinto");
                verifyOrder(created, result.quantity(), result.email());
                Response retrieved = ApiClient.getOrder(id);
                ApiChecks.status(retrieved, 200);
                verifyOrder(retrieved, result.quantity(), result.email());
            }
            assertEquals(count, createdIds.size());
        } finally {
            release.countDown();
            pool.shutdownNow();
        }
    }

    private static void verifyOrder(Response response, int quantity, String email) {
        assertEquals(email, response.jsonPath().getString("client.email"));
        assertEquals(1, response.jsonPath().getList("items").size());
        assertEquals(ApiConfig.product1(), response.jsonPath().getString("items[0].productId"));
        assertEquals(quantity, response.jsonPath().getInt("items[0].quantity"));
        BigDecimal subtotal = new BigDecimal("3500.00").multiply(BigDecimal.valueOf(quantity));
        BigDecimal discount = subtotal.multiply(new BigDecimal("0.10")).setScale(2, RoundingMode.HALF_UP);
        ApiChecks.money(response, "items[0].lineTotal", subtotal.toPlainString());
        ApiChecks.money(response, "subtotal", subtotal.toPlainString());
        ApiChecks.money(response, "discount", discount.toPlainString());
        ApiChecks.money(response, "total", subtotal.subtract(discount).toPlainString());
    }

    private record Result(int quantity, String email, Response response) { }
}
