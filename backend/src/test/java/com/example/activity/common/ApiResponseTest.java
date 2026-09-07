package com.example.activity.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ApiResponseTest {

    @Test
    void createsSuccessResponse() {
        ApiResponse<String> response = ApiResponse.success("ready");

        assertEquals(200, response.code());
        assertEquals("success", response.message());
        assertEquals("ready", response.data());
    }

    @Test
    void createsFailureResponseWithoutPayload() {
        ApiResponse<Void> response = ApiResponse.failure(409, "conflict");

        assertEquals(409, response.code());
        assertEquals("conflict", response.message());
        assertNull(response.data());
    }
}
