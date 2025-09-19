package com.fitian.burntz.infra.payment.service;

import com.fitian.burntz.domain.box.entity.Box;
import com.fitian.burntz.domain.box.entity.BoxSubscription;
import com.fitian.burntz.domain.box.entity.SubscriptionEventLog;
import com.fitian.burntz.domain.box.enums.SubscriptionStatus;
import com.fitian.burntz.domain.box.repository.BoxRepository;
import com.fitian.burntz.domain.box.repository.BoxSubscriptionRepository;
import com.fitian.burntz.domain.box.repository.SubscriptionEventLogRepository;
import com.fitian.burntz.domain.member.entity.Member;
import com.fitian.burntz.domain.member.repository.MemberRepository;
import com.fitian.burntz.global.exception.ErrorCode;
import com.fitian.burntz.global.exception.ValidationException;
import com.fitian.burntz.infra.payment.enums.PaymentStore;
import com.fitian.burntz.infra.payment.v1.dto.WebhookPurchaseResponse;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class PaymentService {

  private final BoxSubscriptionRepository boxSubscriptionRepository;
  private final BoxRepository boxRepository;
  private final SubscriptionEventLogRepository subscriptionEventLogRepository;
  private final MemberRepository memberRepository;

  @Transactional
  public void handlePuchaseWebhook(WebhookPurchaseResponse webhookPurchaseResponse) {
    log.info("\n" + "결제완료 데이터 수신" + "\n" + "주문자 ID : " + webhookPurchaseResponse.getEvent().getOwnerMemberId() + "\n" + "박스 pk : " + webhookPurchaseResponse.getEvent().getSubscriberAttributes().getBoxPk().getValue());

    // 1. 멤버가 존재하는지 확인
    String ownerMemberId = webhookPurchaseResponse.getEvent().getOwnerMemberId();
    Long ownerMemberIdToLong = Long.parseLong(ownerMemberId);
    Member member = memberRepository.findById(ownerMemberIdToLong)
        .orElseThrow(() -> new ValidationException(ErrorCode.USER_NOT_FOUND));

    // 2. 박스가 존재하는지 확인
    String boxPk = webhookPurchaseResponse.getEvent().getSubscriberAttributes().getBoxPk().getValue();
    Long boxPkToLong = Long.parseLong(boxPk);
    Box box = boxRepository.findById(boxPkToLong)
        .orElseThrow(() -> new ValidationException(ErrorCode.BOX_NOT_FOUND));

    // 3. box_subscription 테이블 업데이트 또는 삽입
    String productId = webhookPurchaseResponse.getEvent().getProductId();
    PaymentStore store = webhookPurchaseResponse.getEvent().getStore();
    SubscriptionStatus subscriptionStatus = SubscriptionStatus.ACTIVE;
    LocalDateTime startedAt = LocalDateTime.now();
    LocalDateTime expiredAt = LocalDateTime.now().plusMonths(1);
    Double price = webhookPurchaseResponse.getEvent().getPrice();

    BoxSubscription boxSubscription = BoxSubscription
        .of(member, box, productId, store, subscriptionStatus, startedAt, expiredAt, price);

    SubscriptionEventLog subscriptionEventLog = SubscriptionEventLog.from(webhookPurchaseResponse, member, box);

    // 4. 박스 구독 정보 저장
    if(boxSubscriptionRepository.findByBoxPk(boxPkToLong).isPresent()) {
      log.info("박스 구독 정보 저장 중 - 기존 구독 정보가 존재하여 업데이트를 진행합니다.");
      BoxSubscription oldBoxSubscription = boxSubscriptionRepository.findByBoxPk(boxPkToLong)
          .orElseThrow(() -> new ValidationException(ErrorCode.BOX_NOT_FOUND));
      BoxSubscription updatedBoxSubscription = oldBoxSubscription.replaceTo(boxSubscription);
      boxSubscriptionRepository.save(updatedBoxSubscription);
    } else {
      log.info("박스 구독 정보 저장 중 - 새로운 구독 정보를 저장합니다.");
      boxSubscriptionRepository.save(boxSubscription);
    }

    // 5. 박스 구독 로그 저장
    System.out.println("subscriptionEventLog.getSubscriptionEventLogPk() = " + subscriptionEventLog.getSubscriptionEventLogPk();
    subscriptionEventLogRepository.save(subscriptionEventLog);
  }

}
