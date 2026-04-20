package com.fitian.burntz.infra.payment.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PaymentStore {
  APP_STORE("APP_STORE", "앱스토어"),
  PLAY_STORE("PLAY_STORE", "플레이스토어"),
  OTHER("OTHER", "기타");

  private final String value;
  private final String description;

  public static PaymentStore from(String rcStore) {
    if (rcStore == null) return OTHER;
    return switch (rcStore.toLowerCase()) {
      case "app_store"  -> APP_STORE;
      case "play_store" -> PLAY_STORE;
      default           -> OTHER;
    };
  }
}
