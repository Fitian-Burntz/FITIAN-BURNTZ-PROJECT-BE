package com.fitian.burntz.global.config;

import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.core5.util.Timeout;
import org.apache.hc.core5.util.TimeValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Configuration
public class RestTemplateConfig {

    private static final Logger log = LoggerFactory.getLogger(RestTemplateConfig.class);

    @Value("${app.http.connect-timeout-ms:5000}")
    private int connectTimeoutMs;
    @Value("${app.http.socket-timeout-ms:10000}")
    private int socketTimeoutMs;
    @Value("${app.http.connection-request-timeout-ms:3000}")
    private int connectionRequestTimeoutMs;
    @Value("${app.http.max-total-connections:200}")
    private int maxTotal;
    @Value("${app.http.max-per-route:50}")
    private int maxPerRoute;
    @Value("${app.http.evict-idle-seconds:30}")
    private int evictIdleSeconds;

    @Bean
    public RestTemplate restTemplate() {
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(Timeout.ofMilliseconds(connectTimeoutMs))
                .setResponseTimeout(Timeout.ofMilliseconds(socketTimeoutMs))
                .setConnectionRequestTimeout(Timeout.ofMilliseconds(connectionRequestTimeoutMs))
                .build();

        PoolingHttpClientConnectionManager connManager = new PoolingHttpClientConnectionManager();
        connManager.setMaxTotal(maxTotal);
        connManager.setDefaultMaxPerRoute(maxPerRoute);

        CloseableHttpClient httpClient = HttpClients.custom()
                .setConnectionManager(connManager)
                .setDefaultRequestConfig(requestConfig)
                .evictExpiredConnections()
                .evictIdleConnections(TimeValue.ofSeconds(evictIdleSeconds))
                .build();

        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory(httpClient);
        factory.setConnectTimeout(connectTimeoutMs);
        factory.setReadTimeout(socketTimeoutMs);
        factory.setConnectionRequestTimeout(connectionRequestTimeoutMs);

        RestTemplate restTemplate = new RestTemplate(factory);

        List<ClientHttpRequestInterceptor> interceptors = new ArrayList<>();
        interceptors.add(new LoggingMaskingInterceptor());
        restTemplate.setInterceptors(interceptors);

        restTemplate.setErrorHandler(new DefaultResponseErrorHandler() {
            @Override
            public void handleError(ClientHttpResponse response) throws IOException {
                String body = "";
                try { body = StreamUtils.copyToString(response.getBody(), StandardCharsets.UTF_8); } catch (Exception ignored) {}
                log.warn("[RestTemplate] HTTP Error: status={} bodyPreview={}", response.getStatusCode(), preview(body, 300));
                super.handleError(response);
            }
        });

        return restTemplate;
    }

    private static String preview(String s, int max) {
        if (s == null) return "";
        s = s.replaceAll("\\s+", " ");
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }

    static class LoggingMaskingInterceptor implements ClientHttpRequestInterceptor {
        private static final Logger log = LoggerFactory.getLogger(LoggingMaskingInterceptor.class);
        @Override
        public ClientHttpResponse intercept(org.springframework.http.HttpRequest request, byte[] body,
                                            ClientHttpRequestExecution execution) throws IOException {
            long start = System.nanoTime();
            String maskedHeaders = maskHeaders(request.getHeaders().toString());
            log.info("[HTTP OUT] {} {} headers={} bodyLen={}", request.getMethod(), request.getURI(), maskedHeaders, body == null ? 0 : body.length);
            ClientHttpResponse response = execution.execute(request, body);
            long tookMs = (System.nanoTime() - start) / 1_000_000;
            String respBodyPreview = "";
            try { respBodyPreview = StreamUtils.copyToString(response.getBody(), StandardCharsets.UTF_8); } catch (Exception ignored) {}
            log.info("[HTTP IN] {} {} status={} tookMs={} respPreview={}",
                    request.getMethod(), request.getURI(), response.getStatusCode(), tookMs, preview(respBodyPreview, 300));
            return response;
        }
        private static String maskHeaders(String headers) {
            if (headers == null) return null;
            return headers.replaceAll("(Authorization:\\s*)[^,\\]]+", "$1[masked]");
        }
    }
}
