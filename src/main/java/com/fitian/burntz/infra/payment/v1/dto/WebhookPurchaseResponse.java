package com.fitian.burntz.infra.payment.v1.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fitian.burntz.infra.payment.enums.PaymentEventType;
import com.fitian.burntz.infra.payment.enums.PaymentStore;
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
    private PaymentEventType type;

    @JsonProperty("app_user_id")
    private String ownerMemberId;

    @JsonProperty("product_id")
    private String productId;

    private PaymentStore store;

    @JsonProperty("purchased_at_ms")
    private Long startedAt;

    @JsonProperty("expiration_at_ms")
    private Long expiresAt;

    private Double price;
  }
}