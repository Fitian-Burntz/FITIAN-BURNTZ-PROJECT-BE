package com.fitian.burntz.domain.box.entity;

import com.fitian.burntz.domain.box.enums.SubscriptionStatus;
import com.fitian.burntz.domain.member.entity.Member;
import com.fitian.burntz.global.common.entity.BaseTime;
import com.fitian.burntz.infra.payment.enums.PaymentStore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * @author : 홍준표
 * @packageName : com.fitian.burntz.domain.box.entity;
 * @fileName : BoxSubscription
 * @date : 2025-09-09
 * @description : Box 구독 엔티티 입니다.
 */

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BoxSubscription extends BaseTime {

  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "box_subscription_pk")
  private Long boxSubscriptionPk;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "owner_member_id")
  private Member member; //구매자 id

  private String productId; //구매한 상품 id

  @Enumerated(EnumType.STRING)
  private PaymentStore store;

  @Enumerated(EnumType.STRING)
  private SubscriptionStatus status;

  private LocalDateTime startedAt;

  private LocalDateTime expiredAt;

  private Double price;

  @Builder
  private BoxSubscription(Member member, String productId,
      PaymentStore store, SubscriptionStatus status,
      LocalDateTime startedAt, LocalDateTime expiredAt,
      Double price) {
    this.member = member;
    this.productId = productId;
    this.store = store;
    this.status = status;
    this.startedAt = startedAt;
    this.expiredAt = expiredAt;
    this.price = price;
  }

  public static BoxSubscription of(Member member, String productId,
      PaymentStore store, SubscriptionStatus status,
      LocalDateTime startedAt, LocalDateTime expiredAt, Double price) {
    return BoxSubscription.builder()
        .member(member)
        .productId(productId)
        .store(store)
        .status(status)
        .startedAt(startedAt)
        .expiredAt(expiredAt)
        .price(price)
        .build();
  }



}
