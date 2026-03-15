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
import java.time.temporal.ChronoUnit;
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

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "box_pk")
  private Box box;

  private String productId; //구매한 상품 id

  @Enumerated(EnumType.STRING)
  private PaymentStore store;

  @Enumerated(EnumType.STRING)
  private SubscriptionStatus status;

  private LocalDateTime startedAt;

  private LocalDateTime expiredAt;

  private Double price;

  @Builder
  private BoxSubscription(Long boxSubscriptionPk, Member member, Box box, String productId,
      PaymentStore store, SubscriptionStatus status,
      LocalDateTime startedAt, LocalDateTime expiredAt,
      Double price) {
    this.boxSubscriptionPk = boxSubscriptionPk;
    this.member = member;
    this.box = box;
    this.productId = productId;
    this.store = store;
    this.status = status;
    this.startedAt = startedAt;
    this.expiredAt = expiredAt;
    this.price = price;
  }

  public static BoxSubscription of(Member member ,Box box, String productId,
      PaymentStore store, SubscriptionStatus status,
      LocalDateTime startedAt, LocalDateTime expiredAt, Double price) {
    return BoxSubscription.builder()
        .member(member)
        .box(box)
        .productId(productId)
        .store(store)
        .status(status)
        .startedAt(startedAt)
        .expiredAt(expiredAt)
        .price(price)
        .build();
  }

  public BoxSubscription replaceTo(BoxSubscription boxSubscription) {
    return BoxSubscription
        .builder()
        .boxSubscriptionPk(this.getBoxSubscriptionPk())
        .member(boxSubscription.getMember())
        .box(boxSubscription.getBox())
        .productId(boxSubscription.getProductId())
        .store(boxSubscription.getStore())
        .status(boxSubscription.getStatus())
        .startedAt(boxSubscription.getStartedAt())
        .expiredAt(boxSubscription.getExpiredAt())
        .price(boxSubscription.getPrice())
        .build();
  }

  public BoxSubscription updateStatus(SubscriptionStatus status) {
    this.status = status;
    return this;
  }

  public long getRemainingDays() {
    LocalDateTime now = LocalDateTime.now();
    if (now.isAfter(expiredAt)) {
      return 0;
    }

    return ChronoUnit.DAYS.between(now, expiredAt);
  }

  public void sync(
          String productId,
          PaymentStore store,
          SubscriptionStatus status,
          LocalDateTime startedAt,
          LocalDateTime expiredAt,
          Double price
  ) {
    this.productId = productId;
    this.store = store;
    this.status = status;
    this.startedAt = startedAt;
    this.expiredAt = expiredAt;
    this.price = price;
  }
}
