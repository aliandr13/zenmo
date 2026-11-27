package com.github.aliandr13.zenmo.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import static org.assertj.core.api.Assertions.assertThat;

class JsonAuthErrorHandlerTest {

    private final JsonMapper json = JsonMapper.builder().build();
    private final JsonAuthErrorHandler handler = new JsonAuthErrorHandler(json);

    @Test
    void commenceWrites401Json() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/accounts");
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.commence(request, response, new InsufficientAuthenticationException("no token"));

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentType()).startsWith("application/json");
        JsonNode body = json.readTree(response.getContentAsString());
        assertThat(body.get("status").asInt()).isEqualTo(401);
        assertThat(body.get("error").asString()).isEqualTo("Unauthorized");
        assertThat(body.get("message").asString()).isEqualTo("Unauthorized");
        assertThat(body.get("path").asString()).isEqualTo("/api/accounts");
    }

    @Test
    void handleWrites403Json() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("DELETE", "/api/accounts/1");
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.handle(request, response, new AccessDeniedException("nope"));

        assertThat(response.getStatus()).isEqualTo(403);
        JsonNode body = json.readTree(response.getContentAsString());
        assertThat(body.get("message").asString()).isEqualTo("Forbidden");
        assertThat(body.get("path").asString()).isEqualTo("/api/accounts/1");
    }
}
