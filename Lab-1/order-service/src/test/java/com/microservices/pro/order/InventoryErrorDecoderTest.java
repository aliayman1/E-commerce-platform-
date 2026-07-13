package com.microservices.pro.order;

import feign.FeignException;
import feign.Request;
import feign.RequestTemplate;
import feign.Response;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class InventoryErrorDecoderTest {

    private final InventoryErrorDecoder decoder = new InventoryErrorDecoder();

    @Test
    void decode_translates409ToInsufficientStockException() {
        Exception result = decoder.decode("InventoryClient#checkStock", responseWithStatus(409));

        assertInstanceOf(InsufficientStockException.class, result);
    }

    @Test
    void decode_translates404ToProductNotFoundException() {
        Exception result = decoder.decode("InventoryClient#checkStock", responseWithStatus(404));

        assertInstanceOf(ProductNotFoundException.class, result);
    }

    @Test
    void decode_translates503ToServiceUnavailableException() {
        Exception result = decoder.decode("InventoryClient#checkStock", responseWithStatus(503));

        assertInstanceOf(ServiceUnavailableException.class, result);
    }

    @Test
    void decode_translatesUnknownStatusToFeignInternalServerError() {
        Exception result = decoder.decode("InventoryClient#checkStock", responseWithStatus(500));

        assertInstanceOf(FeignException.InternalServerError.class, result);
    }

    private Response responseWithStatus(int status) {
        Request request = Request.create(Request.HttpMethod.GET, "/api/v1/inventory/check",
                Collections.emptyMap(), null, StandardCharsets.UTF_8, new RequestTemplate());
        return Response.builder()
                .status(status)
                .reason("test")
                .request(request)
                .headers(Collections.emptyMap())
                .build();
    }
}