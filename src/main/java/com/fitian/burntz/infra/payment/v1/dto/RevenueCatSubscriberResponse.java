package com.fitian.burntz.infra.payment.v1.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

/**
 * @author : 김관중
 * @packageName : com.fitian.burntz.infra.payment.v1.dto
 * @fileName : RevenueCatSubscriberResponse
 * @date : 2026-03-15
 * @description : 레베뉴캣 조회용 클라이언트 dto
 */

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class RevenueCatSubscriberResponse {

    private Subscriber subscriber;

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Subscriber {

        private Map<String, Entitlement> entitlements;

        @JsonProperty("original_app_user_id")
        private String originalAppUserId;

        @JsonProperty("subscriber_attributes")
        private Map<String, SubscriberAttribute> subscriberAttributes;
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Entitlement {

        @JsonProperty("product_identifier")
        private String productIdentifier;

        @JsonProperty("purchase_date")
        private String purchaseDate;

        @JsonProperty("expires_date")
        private String expiresDate;
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SubscriberAttribute {
        private String value;

        @JsonProperty("updated_at_ms")
        private Long updatedAtMs;
    }

}
