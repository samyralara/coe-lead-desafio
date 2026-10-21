package br.com.qa.pedidos.support;

import br.com.qa.pedidos.config.ApiConfig;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import java.util.Map;

/** Client Rest-Assured sem estado global, seguro para parametrização. */
public final class ApiClient {
    private ApiClient() { }

    public static RequestSpecification orderRequest() {
        return base(ApiConfig.orderUrl());
    }

    public static RequestSpecification catalogRequest() {
        return base(ApiConfig.catalogUrl());
    }

    private static RequestSpecification base(String url) {
        return RestAssured.given()
                .baseUri(url)
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .log().ifValidationFails();
    }

    public static Response createOrder(Map<String, Object> order) {
        return orderRequest().body(order).post(ApiConfig.ORDER_PATH);
    }

    public static Response getOrder(String id) {
        return orderRequest().pathParam("id", id).get(ApiConfig.ORDER_PATH + "/{id}");
    }

    public static Response listOrders(int page, int size) {
        return orderRequest().queryParam("page", page).queryParam("size", size).get(ApiConfig.ORDER_PATH);
    }

    public static Response getProduct(String id) {
        return catalogRequest().pathParam("id", id).get(ApiConfig.PRODUCT_PATH + "/{id}");
    }

    public static Response prices(Object ids) {
        return catalogRequest().body(Map.of("productIds", ids)).post(ApiConfig.PRICES_PATH);
    }
}
