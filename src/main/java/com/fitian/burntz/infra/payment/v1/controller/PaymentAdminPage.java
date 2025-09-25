package com.fitian.burntz.infra.payment.v1.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@Slf4j
public class PaymentAdminPage {

  @GetMapping("/api/v1/payments/admin")
  public String paymentAdminPage() {
    log.info("관리자 결제 페이지 요청됨");
    return "admin-payment";
  }
}
