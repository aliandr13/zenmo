package com.github.aliandr13.zenmo.integration;

import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * Builds {@link RestClient} for integration tests without Jetty's HTTP client, which treats
 * {@code 401} as an HTTP authentication challenge and fails if {@code WWW-Authenticate} is absent
 * (common for JSON APIs that return 401 from {@code @ControllerAdvice}).
 */
public final class IntegrationTestRestClient {

    private IntegrationTestRestClient() {
    }

    /**
     * RestClient backed by the JDK {@link java.net.http.HttpClient} (not Jetty's client).
     */
    public static RestClient create() {
        return RestClient.builder()
                .requestFactory(new JdkClientHttpRequestFactory())
                .build();
    }
}
