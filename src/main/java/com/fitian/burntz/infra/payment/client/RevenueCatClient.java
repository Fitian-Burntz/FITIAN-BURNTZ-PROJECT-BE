package com.fitian.burntz.infra.payment.client;

import com.fitian.burntz.infra.payment.v1.dto.RevenueCatSubscriberResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.net.URI;

/**
 * @author : 김관중
 * @packageName : com.fitian.burntz.infra.payment.client
 * @fileName : RevenueCatClient
 * @date : 2026-03-15
 * @description : 레베뉴캣 API 호출 클라이언트
 */

@Component
@RequiredArgsConstructor
public class RevenueCatClient {

    private final RestTemplate restTemplate;

    @Value("${revenuecat.secret.key}")
    private String revenueCatSecretKey;

    @Value("${revenuecat.base-url:https://api.revenuecat.com/v1}")
    private String revenueCatBaseUrl;

    public RevenueCatSubscriberResponse getSubscriber(String appUserId) {
        String url = revenueCatBaseUrl + "/subscribers/" + appUserId;

        RequestEntity<Void> request = RequestEntity
                .get(URI.create(url))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + revenueCatSecretKey)
                .build();

        ResponseEntity<RevenueCatSubscriberResponse> response =
                restTemplate.exchange(request, RevenueCatSubscriberResponse.class);

        return response.getBody();
    }
}
