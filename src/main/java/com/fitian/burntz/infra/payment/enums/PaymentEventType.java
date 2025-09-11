package com.fitian.burntz.infra.payment.enums;


import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PaymentEventType {
  TEST("TEST", "RevenueCat 대시보드를 통해 발행된 테스트 이벤트입니다."),
  INITIAL_PURCHASE("INITIAL_PURCHASE", "새로운 구독을 구매했습니다."),
  RENEWAL("RENEWAL", "기존 구독이 갱신되었거나, 만료된 사용자가 다시 구독을 신청했습니다."),
  CANCELLATION("CANCELLATION", "구독 또는 비갱신 구매가 취소 또는 환불되었습니다. 환불 시 구독의 자동 갱신 설정이 아직 활성화되어 있을 수 있습니다. 자세한 내용은 취소 사유를 참조하세요 . 구독 환불의 경우, 이 이벤트는 구독의 최신 구독 기간이 환불된 경우에만 발생하며, 이전 구독 기간에 대한 환불은 이 이벤트를 발생시키지 않습니다."),
  UNCANCELLATION("UNCANCELLATION", "만료되지 않은 취소된 구독이 다시 활성화되었습니다."),
  NON_RENEWING_PURCHASE("NON_RENEWING_PURCHASE", "고객이 자동 갱신되지 않는 구매를 했습니다."),
  SUBSCRIPTION_PAUSED("SUBSCRIPTION_PAUSED", "구독은 기간 종료 시 일시 중지되도록 설정되었습니다. 참고: 이벤트 수신 시 액세스를 취소하지 마시고 , 이벤트 수신 시( 만료 사유가 있는 경우 )"),
  EXPIRATION("EXPIRATION", "구독이 만료되어 액세스 권한을 삭제해야 합니다. 플랫폼 서버 알림을 구성한 경우, 만료 알림을 받는 즉시(몇 초에서 몇 분 이내) 이 이벤트가 발생합니다. 알림을 구성하지 않은 경우 약 1시간 정도 지연될 수 있습니다."),
  BILLING_ISSUE("BILLING_ISSUE", "CANCELLATION구독자에게 요금을 청구하는 중 문제가 발생했습니다. 이는 구독이 만료되었음을 의미하지 않습니다. 이벤트 + 를 수신하는 경우 무시해도 됩니다."),
  PRODUCT_CHANGE("PRODUCT_CHANGE", "구독자가 구독 상품을 변경했습니다. 이는 새 구독이 즉시 적용되는 것을 의미하지 않습니다"),
  TRANSFER("TRANSFER", "한 앱 사용자 ID에서 다른 앱 사용자 ID로 거래 및 권한 이전이 시작되었습니다. 참고: 두 고객 기록 모두에 이 이벤트가 표시되어 있지만, 이벤트 본문이 두 사용자 모두와 동일하기 때문에 웹훅은 대상 사용자에게만 전송됩니다."),
  SUBSCRIPTION_EXTENDED("SUBSCRIPTION_EXTENDED", "기존 구독이 연장되었습니다(현재 구독 기간의 만료일이 미래로 연기됨). 이 이벤트는 Apple App Store 또는 Google Play Store 구독이 스토어 API를 통해 연장될 때 발생합니다. Google Play Store에서는 Google이 알 수 없는 이유로 갱신 요금 청구를 24시간 미만으로 연기할 때도 이 이벤트가 발생할 수 있습니다. 이 경우 웹훅을 수신한 후 24시간 이내에 또는 웹훅 SUBSCRIPTION_EXTENDED중 하나 가 전송됩니다."),
  TEMPORARY_ENTITLEMENT_GRANT("TEMPORARY_ENTITLEMENT_GRANT", "RevenueCat이 해당 스토어에서 구매를 일시적으로 검증할 수 없어 고객에게 단기 권한을 부여했습니다. 이 이벤트는 예외적인 상황(예: 앱 스토어 부분 중단)에서 전송되며 고객이 구매를 했지만 권한을 얻지 못하는 것을 방지하는 데 사용됩니다. 권한의 만료일은 항상 최대 24시간 후입니다. 예외적인 상황이 해결되고 RevenueCat이 구매를 검증할 수 있게 되면 일반 INITIAL_PURCHASE이벤트가 전송됩니다. 구매를 검증할 수 없는 경우 EXPIRATION동일한 이벤트가 transaction_id전송됩니다. 참고: 이 이벤트 유형은 스토어 서버와의 연결이 제한된 경우에 전송되므로 일반 구매 이벤트보다 정보가 적습니다. 다음 필드는 반드시 존재합니다. app_user_id, purchased_at_ms, expiration_at_ms, event_timestamp_ms, product_id, entitlement_ids, store, transaction_id(참고: 는 후속 이벤트에서 transaction_id스토어의 현재 값과 다를 수 있습니다 . 스토어에 따라서는 후속 이벤트에서 가 현재 값 과 다를 수 있습니다"),
  REFUND_REVERSED("REFUND_REVERSED", "환불이 취소되었습니다."),
  INVOICE_ISSUANCE("INVOICE_ISSUANCE", "구매에 대한 새 송장이 발행되었습니다. 이 이벤트는 아직 결제되지 않은 구독 또는 비갱신 구매에 대한 새 송장이 생성될 때 전송됩니다. 이 이벤트는 웹 결제 구매에만 적용되며, 결제를 통한 신규 구매와 청구 엔진에서 예약한 구독 갱신 모두에 적용됩니다."),
  VIRTUAL_CURRENCY_TRANSACTION("VIRTUAL_CURRENCY_TRANSACTION", "구매 시 가상 화폐가 지급되거나 환불 시 해당 화폐가 제거되는 등 가상 화폐 거래가 발생했습니다. 가상 화폐를 지급하는 구독의 경우, 이 이벤트는 전체 구독 수명 주기(최초 구매, 갱신, 환불 등) 동안 화폐 잔액 조정이 필요할 때마다 전송됩니다. 이 이벤트는 금액, 화폐 유형, 변경 출처 등 가상 화폐 조정에 대한 세부 정보를 제공합니다.");


  private final String value;
  private final String description;



}
