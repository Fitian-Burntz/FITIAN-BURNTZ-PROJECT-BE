package com.fitian.burntz.infra.payment.v1.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fitian.burntz.infra.payment.enums.PaymentEventType;
import com.fitian.burntz.infra.payment.enums.PaymentStore;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class WebhookPurchaseResponse {

  @JsonProperty("api_version")
  private String apiVersion;

  private Event event;


  @Getter
  @Setter
  @AllArgsConstructor
  @NoArgsConstructor
  public static class Event {

    @JsonProperty("event_timestamp_ms")
    private Long eventTimestampMs;

    @JsonProperty("product_id")
    private String productId;

    @JsonProperty("period_type")
    private String periodType;

    @JsonProperty("purchased_at_ms")
    private Long purchasedAtMs;

    @JsonProperty("expiration_at_ms")
    private Long expirationAtMs;

    private String environment;

    @JsonProperty("entitlement_id")
    private String entitlementId;

    @JsonProperty("entitlement_ids")
    private List<String> entitlementIds;

    @JsonProperty("presented_offering_id")
    private String presentedOfferingId;

    @JsonProperty("transaction_id")
    private String transactionId;

    @JsonProperty("original_transaction_id")
    private String originalTransactionId;

    @JsonProperty("is_family_share")
    private Boolean isFamilyShare;

    @JsonProperty("country_code")
    private String countryCode;

    @JsonProperty("app_user_id")
    private String ownerMemberId;

    private List<String> aliases;

    @JsonProperty("original_app_user_id")
    private String originalAppUserId;

    private String currency;
    private Double price;

    @JsonProperty("price_in_purchased_currency")
    private Double priceInPurchasedCurrency;

    @JsonProperty("subscriber_attributes")
    private SubscriberAttributes subscriberAttributes;

    private PaymentStore store;

    @JsonProperty("takehome_percentage")
    private Double takehomePercentage;

    @JsonProperty("offer_code")
    private String offerCode;

    @JsonProperty("tax_percentage")
    private Double taxPercentage;

    @JsonProperty("commission_percentage")
    private Double commissionPercentage;

    private String metadata;

    @JsonProperty("renewal_number")
    private Integer renewalNumber;

    private PaymentEventType type;
    private String id;

    @JsonProperty("app_id")
    private String appId;
  }

  @Getter
  @Setter
  @AllArgsConstructor
  @NoArgsConstructor
  public static class SubscriberAttributes {
    private BoxPk boxPk;
  }

  @Getter
  @Setter
  @AllArgsConstructor
  @NoArgsConstructor
  public static class BoxPk {
    private String value;
  }
}