package com.fitian.burntz.infra.payment.service;

import com.fitian.burntz.domain.box.repository.BoxSubscriptionRepository;
import com.fitian.burntz.domain.box.repository.SubscriptionEventLogRepository;
import com.fitian.burntz.domain.member.repository.MemberRepository;
import com.fitian.burntz.infra.payment.v1.dto.WebhookPurchaseResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PaymentService {

  private final BoxSubscriptionRepository boxSubscriptionRepository;
  private final SubscriptionEventLogRepository subscriptionEventLogRepository;
  private final MemberRepository memberRepository;

  public void handlePuchaseWebhook(WebhookPurchaseResponse webhookPurchaseResponse) {



  }

}
