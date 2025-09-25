package com.fitian.burntz.infra.payment.docs;

import com.fitian.burntz.global.common.response.ApiResponse;
import com.fitian.burntz.infra.payment.dto.response.PurchaseLogResponse;
import com.fitian.burntz.infra.payment.v1.dto.WebhookPurchaseResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;

public interface PaymentDocs {

  public ResponseEntity<?> handlePuchaseWebhook(WebhookPurchaseResponse webhookPurchaseResponse, HttpServletRequest request);
  public ApiResponse<List<PurchaseLogResponse>> getPurchaseLog(Long memberPk);
}
