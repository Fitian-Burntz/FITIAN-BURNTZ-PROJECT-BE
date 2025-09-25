package com.fitian.burntz.infra.payment.docs;

import com.fitian.burntz.infra.payment.v1.dto.WebhookPurchaseResponse;
import org.springframework.http.ResponseEntity;

public interface PaymentDocs {

  public ResponseEntity<?> handlePuchaseWebhook(WebhookPurchaseResponse webhookPurchaseResponse);

}
