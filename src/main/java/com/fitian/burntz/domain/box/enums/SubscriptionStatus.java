package com.fitian.burntz.domain.box.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SubscriptionStatus {

  ACTIVE("ACTIVE", "활성"),
  EXPIRED("EXPIRED", "만료"),
  CANCELED("CANCELED", "취소"),
  REFUNDED("REFUNDED", "환불"),
  PENDING("PENDING", "보류");


  private final String value;
  private final String description;
}
