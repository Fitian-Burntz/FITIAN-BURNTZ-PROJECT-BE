package com.fitian.burntz.infra.payment.v1.controller;

import com.fitian.burntz.infra.payment.docs.PaymentDocs;
import com.fitian.burntz.infra.payment.service.PaymentService;
import com.fitian.burntz.infra.payment.v1.dto.WebhookPurchaseResponse;
import jakarta.servlet.http.HttpServletRequest;
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
public class PaymentController implements PaymentDocs {

  private final PaymentService paymentService;

  @Override
  @PostMapping("/webhook/purchase")
  public ResponseEntity<?> handlePuchaseWebhook(@RequestBody WebhookPurchaseResponse webhookPurchaseResponse,
      HttpServletRequest request) {
    paymentService.handlePuchaseWebhook(webhookPurchaseResponse, request);
    return ResponseEntity.ok().build();
  }

}
