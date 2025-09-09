package com.fitian.burntz.infra.payment.v1.controller;

import com.fitian.burntz.infra.payment.v1.dto.WebhookResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

  @PostMapping("/webhook")
  public ResponseEntity<?> handleWebhook(@RequestBody WebhookResponseDTO webhookResponseDTO) {
    System.out.println("상품코드 :  = " + webhookResponseDTO.getEvent().getProductId());
    System.out.println("상품가격 :  = " + webhookResponseDTO.getEvent().getPrice() + "달러");
    System.out.println("구매처 = " + webhookResponseDTO.getEvent().getStore());
    System.out.println("구매자 아이디 = " + webhookResponseDTO.getEvent().getAppUserId());
    System.out.println("이벤트 타입 = " + webhookResponseDTO.getEvent().getType().getValue());
    return ResponseEntity.ok().build();
  }






}
