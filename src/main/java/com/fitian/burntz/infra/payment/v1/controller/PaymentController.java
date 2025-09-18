package com.fitian.burntz.infra.payment.v1.controller;

import com.fitian.burntz.infra.payment.service.PaymentService;
import com.fitian.burntz.infra.payment.v1.dto.WebhookPurchaseResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
/**
 * @author : 홍준표
 * @packageName : com.fitian.burntz.infra.payment.v1.controller
 * @fileName : PaymentController
 * @date : 2025-09-09
 * @description : revenuecat 결제 요청 이후 웹훅으로 결제 완료 데이터를 받는 컨트롤러
 */

@RestController
@Slf4j
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

  private final PaymentService paymentService;

  @PostMapping("/webhook/purchase")
  public ResponseEntity<?> handlePuchaseWebhook(@RequestBody WebhookPurchaseResponse webhookPurchaseResponse) {
    paymentService.handlePuchaseWebhook(webhookPurchaseResponse);
    return ResponseEntity.ok().build();
  }

//  @PostMapping("/webhook/cancel")
//  public ResponseEntity<?> handleCancelWebhook(@RequestBody WebhookPurchaseResponse webhookPurchaseResponse) {
//    System.out.println("상품코드 :  = " + webhookPurchaseResponse.getEvent().getProductId());
//    System.out.println("상품가격 :  = " + webhookPurchaseResponse.getEvent().getPrice() + "달러");
//    System.out.println("구매처 = " + webhookPurchaseResponse.getEvent().getStore());
//    System.out.println("구매자 아이디 = " + webhookPurchaseResponse.getEvent().getOwnerMemberId());
//    System.out.println("이벤트 타입 = " + webhookPurchaseResponse.getEvent().getType().getValue());
//    return ResponseEntity.ok().build();
//  }








}
