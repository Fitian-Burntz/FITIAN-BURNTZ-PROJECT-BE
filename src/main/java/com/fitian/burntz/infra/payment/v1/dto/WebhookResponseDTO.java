package com.fitian.burntz.infra.payment.v1.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fitian.burntz.infra.payment.enums.PaymentEventType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class WebhookResponseDTO {

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
    private String appUserId;

    @JsonProperty("product_id")
    private String productId;

    @JsonProperty("purchased_at_ms")
    private Long purchasedAtMs;

    @JsonProperty("expiration_at_ms")
    private Long expirationAtMs;

    private Double price;

    private String currency;

    private String store;

    private String environment;
  }
}