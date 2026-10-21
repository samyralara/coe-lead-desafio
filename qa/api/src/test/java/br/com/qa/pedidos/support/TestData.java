package br.com.qa.pedidos.support;

import br.com.qa.pedidos.config.ApiConfig;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class TestData {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private TestData() { }

    public static Map<String, Object> client() {
        Map<String, Object> client = new LinkedHashMap<>();
        client.put("name", "Cliente QA");
        client.put("document", "12345678901");
        client.put("email", "qa+" + UUID.randomUUID().toString().substring(0, 8) + "@example.com");
        return client;
    }

    public static Map<String, Object> item(String productId, int quantity) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("productId", productId);
        result.put("quantity", quantity);
        return result;
    }

    public static Map<String, Object> order(List<Map<String, Object>> items) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("client", client());
        result.put("items", items);
        return result;
    }

    public static Map<String, Object> single(String productId, int quantity) {
        return order(List.of(item(productId, quantity)));
    }

    public static Map<String, Object> nominal() {
        return order(List.of(item(ApiConfig.product1(), 2), item(ApiConfig.product2(), 1)));
    }

    public static List<Map<String, Object>> distinctItems(int count) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            // Comprimento <= 50; para 101 itens o request deve falhar antes de consultar o catálogo.
            list.add(item(String.format("QA-NOT-SEEDED-%03d", i), 1));
        }
        return list;
    }

    public static String missingProduct() {
        return "QA-MISSING-" + UUID.randomUUID();
    }

    public static String json(Object object) {
        try {
            return MAPPER.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Não foi possível serializar payload de teste", e);
        }
    }

    /** Gera JSON válido com o tamanho exato, em bytes UTF-8 ASCII, expandindo apenas client.name. */
    @SuppressWarnings("unchecked")
    public static String paddedOrderOfExactSize(int targetBytes) {
        Map<String, Object> payload = single(ApiConfig.product1(), 1);
        Map<String, Object> person = (Map<String, Object>) payload.get("client");
        person.put("name", "");
        int baseSize = json(payload).getBytes(StandardCharsets.UTF_8).length;
        if (targetBytes < baseSize) throw new IllegalArgumentException("Corpo menor que payload mínimo");
        person.put("name", "A".repeat(targetBytes - baseSize));
        String json = json(payload);
        if (json.getBytes(StandardCharsets.UTF_8).length != targetBytes) {
            throw new IllegalStateException("Payload não tem tamanho exato em bytes");
        }
        return json;
    }
}
