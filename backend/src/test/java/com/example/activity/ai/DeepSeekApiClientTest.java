package com.example.activity.ai;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class DeepSeekApiClientTest {

    private MockRestServiceServer server;
    private DeepSeekApiClient client;

    @BeforeEach
    void setUp() {
        AiProperties properties = new AiProperties();
        properties.setEnabled(true);
        properties.setApiKey("test-api-key");
        properties.setModel("deepseek-chat");
        properties.setMaxTokens(900);

        RestClient.Builder builder = RestClient.builder().baseUrl("https://api.deepseek.com");
        server = MockRestServiceServer.bindTo(builder).build();
        client = new DeepSeekApiClient(properties, builder.build());
    }

    @Test
    void sendsStructuredChatCompletionRequestAndReturnsContent() {
        server.expect(once(), requestTo("https://api.deepseek.com/chat/completions"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer test-api-key"))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.model").value("deepseek-chat"))
                .andExpect(jsonPath("$.max_tokens").value(900))
                .andExpect(jsonPath("$.response_format.type").value("json_object"))
                .andExpect(jsonPath("$.messages[0].role").value("system"))
                .andExpect(jsonPath("$.messages[1].content").value("activity prompt"))
                .andRespond(withSuccess("""
                        {"choices":[{"message":{"content":"{\\"title\\":\\"测试活动\\"}"}}]}
                        """, MediaType.APPLICATION_JSON));

        assertEquals("{\"title\":\"测试活动\"}", client.generateActivityCopy("activity prompt"));
        server.verify();
    }

    @Test
    void mapsProviderRateLimitToTooManyRequests() {
        server.expect(once(), requestTo("https://api.deepseek.com/chat/completions"))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));

        AiServiceException exception = assertThrows(
                AiServiceException.class,
                () -> client.generateActivityCopy("activity prompt")
        );

        assertEquals(429, exception.getCode());
        server.verify();
    }

    @Test
    void rejectsProviderResponseWithoutMessageContent() {
        server.expect(once(), requestTo("https://api.deepseek.com/chat/completions"))
                .andRespond(withSuccess("{\"choices\":[]}", MediaType.APPLICATION_JSON));

        AiServiceException exception = assertThrows(
                AiServiceException.class,
                () -> client.generateActivityCopy("activity prompt")
        );

        assertEquals(502, exception.getCode());
        server.verify();
    }
}
