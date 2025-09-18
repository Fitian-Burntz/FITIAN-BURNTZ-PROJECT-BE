package com.fitian.burntz.domain.box.entity;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fitian.burntz.domain.box.enums.SubscriptionStatus;
import com.fitian.burntz.domain.member.entity.Member;
import com.fitian.burntz.global.common.entity.BaseTime;
import com.fitian.burntz.infra.payment.enums.PaymentEventType;
import com.fitian.burntz.infra.payment.enums.PaymentStore;
import com.fitian.burntz.infra.payment.v1.dto.WebhookPurchaseResponse;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * @author : 홍준표
 * @packageName : com.fitian.burntz.domain.box.entity;
 * @fileName : SubscriptionEventLog
 * @date : 2025-09-16
 * @description : Box 구독 로그 엔티티 입니다.
 */

@Getter
@Entity
@Slf4j
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SubscriptionEventLog extends BaseTime {

  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "subscription_event_log_pk")
  private Long subscriptionEventLogPk;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "member_pk")
  private Member member;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "box_pk")
  private Box box;

  @Column(length = 255)
  private String productId;

  @Enumerated(EnumType.STRING)
  private PaymentStore store;

  @Enumerated(EnumType.STRING)
  private SubscriptionStatus status;

  private LocalDateTime cancelledAt;

  private LocalDateTime refundedAt;

  private Double price;

  private String appId;

  private String appUserId;

  @Enumerated(EnumType.STRING)
  private PaymentEventType eventType;

  @Column(length = 100000)
  private String payload;


  @Builder
  private SubscriptionEventLog (Member member, Box box, String productId,
      PaymentStore store, SubscriptionStatus status,
      LocalDateTime cancelledAt, LocalDateTime refundedAt,
      Double price, String appId, String appUserId,
      PaymentEventType eventType, String payload) {
    this.member = member;
    this.box = box;
    this.productId = productId;
    this.store = store;
    this.status = status;
    this.cancelledAt = cancelledAt;
    this.refundedAt = refundedAt;
    this.price = price;
    this.appId = appId;
    this.appUserId = appUserId;
    this.eventType = eventType;
    this.payload = payload;
  }

  public static SubscriptionEventLog of(Member member, Box box, String productId,
      PaymentStore store, SubscriptionStatus status,
      LocalDateTime cancelledAt, LocalDateTime refundedAt,
      Double price, String appId, String appUserId,
      PaymentEventType eventType, String payload) {
    return SubscriptionEventLog.builder()
        .member(member)
        .box(box)
        .productId(productId)
        .store(store)
        .status(status)
        .cancelledAt(cancelledAt)
        .refundedAt(refundedAt)
        .price(price)
        .appId(appId)
        .appUserId(appUserId)
        .eventType(eventType)
        .payload(payload)
        .build();
  }

  public static SubscriptionEventLog from(WebhookPurchaseResponse webhookPurchaseResponse) {
    try {
      ObjectMapper mapper = new ObjectMapper();
      System.out.println("json data : " + mapper.writeValueAsString(webhookPurchaseResponse));
      return SubscriptionEventLog
          .builder()
          .member(null) // TODO: 실제 멤버 엔티티 조회 후 교체 필요
          .box(null) // TODO: 실제 박스 엔티티 조회 후 교체 필요
          .productId(webhookPurchaseResponse.getEvent().getProductId())
          .store(webhookPurchaseResponse.getEvent().getStore())
          .status(SubscriptionStatus.ACTIVE)
          .cancelledAt(null)
          .refundedAt(null)
          .price(webhookPurchaseResponse.getEvent().getPrice())
          .appId(webhookPurchaseResponse.getEvent().getAppId())
          .appUserId(webhookPurchaseResponse.getEvent().getOwnerMemberId())
          .eventType(webhookPurchaseResponse.getEvent().getType())
          .payload(mapper.writeValueAsString(webhookPurchaseResponse))
          .build();
    } catch (Exception e) {
      log.error("WebhookPurchaseResponse 직렬화 에러: ", e);
      return null;
    }
  }

}
