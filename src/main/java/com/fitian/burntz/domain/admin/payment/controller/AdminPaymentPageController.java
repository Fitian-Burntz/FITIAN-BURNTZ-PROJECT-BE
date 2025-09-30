package com.fitian.burntz.domain.admin.payment.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Slf4j
@Controller
@RequestMapping("/api/v1/admin/payments")
@RequiredArgsConstructor
public class AdminPaymentPageController {

  @GetMapping
  public String paymentPage() {
    return "admin/payment/admin-payment";
  }



}
