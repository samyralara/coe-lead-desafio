package br.com.qa.pedidos.config;

import java.net.URI;
import java.util.Optional;
import org.junit.jupiter.api.Assumptions;

/** Configuração de URLs e massas; -Dqa.* prevalece sobre QA_* do ambiente. */
public final class ApiConfig {
    private ApiConfig() { }

    public static final String ORDER_PATH = "/api/v1/orders";
    public static final String PRODUCT_PATH = "/api/v1/products";
    public static final String PRICES_PATH = PRODUCT_PATH + "/prices";

    public static String orderUrl() {
        return url("qa.orderBaseUrl", "QA_ORDER_BASE_URL", "http://localhost:8080");
    }

    public static String catalogUrl() {
        return url("qa.catalogBaseUrl", "QA_CATALOG_BASE_URL", "http://localhost:8081");
    }

    private static String url(String property, String env, String fallback) {
        String url = value(property, env).orElse(fallback).replaceAll("/+$", "");
        URI uri = URI.create(url);
        if (uri.getHost() == null || !("http".equals(uri.getScheme()) || "https".equals(uri.getScheme()))) {
            throw new IllegalArgumentException("URL inválida para " + property + ": " + url);
        }
        return url;
    }

    public static String product1() {
        return value("qa.product1Id", "QA_PRODUCT_1_ID").orElse("PROD-001");
    }

    public static String product2() {
        return value("qa.product2Id", "QA_PRODUCT_2_ID").orElse("PROD-002");
    }

    public static Optional<String> value(String property, String env) {
        String sys = System.getProperty(property);
        if (sys != null && !sys.isBlank()) return Optional.of(sys.trim());
        String os = System.getenv(env);
        return (os == null || os.isBlank()) ? Optional.empty() : Optional.of(os.trim());
    }

    /** Evita falso negativo para uma massa não informada nos quatro documentos. */
    public static String requireFixture(String property, String env, String meaning) {
        Optional<String> configured = value(property, env);
        Assumptions.assumeTrue(configured.isPresent(),
                () -> "Massa de teste não configurada: " + meaning + " (" + env + " / -D" + property + ")");
        return configured.orElseThrow();
    }
}
