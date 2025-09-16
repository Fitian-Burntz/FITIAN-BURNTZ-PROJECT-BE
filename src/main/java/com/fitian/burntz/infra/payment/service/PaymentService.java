package com.fitian.burntz.infra.payment.service;

import com.fitian.burntz.domain.box.repository.BoxSubscriptionRepository;
import com.fitian.burntz.domain.member.repository.MemberRepository;
import com.fitian.burntz.infra.payment.v1.dto.WebhookPurchaseResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PaymentService {

  private final BoxSubscriptionRepository boxSubscriptionRepository;
  private final MemberRepository memberRepository;

  public void handlePuchaseWebhook(WebhookPurchaseResponse webhookPurchaseResponse) {

    System.out.println("상품코드 :  = " + webhookPurchaseResponse.getEvent().getProductId());
    System.out.println("상품가격 :  = " + webhookPurchaseResponse.getEvent().getPrice() + "달러");
    System.out.println("구매처 = " + webhookPurchaseResponse.getEvent().getStore());
    System.out.println("구매자 아이디 = " + webhookPurchaseResponse.getEvent().getOwnerMemberId());
    System.out.println("이벤트 타입 = " + webhookPurchaseResponse.getEvent().getType().getValue());
    System.out.println("구매한 박스 pk = " + webhookPurchaseResponse.getEvent().getSubscriberAttributes().getBoxPk().getValue());

  }

}
