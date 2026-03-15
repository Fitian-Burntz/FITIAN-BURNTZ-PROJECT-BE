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
import com.fitian.burntz.global.exception.NotFoundException;
import com.fitian.burntz.global.exception.ValidationException;
import com.fitian.burntz.global.security.jwt.JwtTokenProvider;
import com.fitian.burntz.infra.payment.client.RevenueCatClient;
import com.fitian.burntz.infra.payment.dto.response.PurchaseLogResponse;
import com.fitian.burntz.infra.payment.enums.PaymentEventType;
import com.fitian.burntz.infra.payment.enums.PaymentStore;
import com.fitian.burntz.infra.payment.v1.dto.PaymentSyncResponse;
import com.fitian.burntz.infra.payment.v1.dto.RevenueCatSubscriberResponse;
import com.fitian.burntz.infra.payment.v1.dto.WebhookPurchaseResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

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
  private final RevenueCatClient revenueCatClient;

  private final JwtTokenProvider jwtTokenProvider;

  /**
   * [결제 완료 웹훅 처리 메서드]
   * @param webhookPurchaseResponse 결제 완료 웹훅 데이터
   * 결제완료 처리 흐름..
   * 1. 토큰 검증
   * 2. 멤버가 존재하는지 확인
   * 3. 박스가 존재하는지 확인
   * 4. box_subscription 테이블 업데이트 또는 삽입
   * 5. 구독 정보 저장
   * 6. 구독 로그 저장
   * 7. 최종 BOX 구독상태 변경
   */
  @Transactional
  public void handlePuchaseWebhook(WebhookPurchaseResponse webhookPurchaseResponse, HttpServletRequest request) {
    log.info("\n" + "결제완료 데이터 수신" + "\n" + "주문자 ID : " + webhookPurchaseResponse.getEvent().getOwnerMemberId() + "\n" + "박스 pk : " + webhookPurchaseResponse.getEvent().getSubscriberAttributes().getBoxPk().getValue());

    // 1. 토큰 검증
//    log.info("[1. 토큰 검증]");
//    String token = extractToken(request);
//    if(!jwtTokenProvider.validateToken(token)) {
//      log.error("[1. 토큰 검증] - 결제완료 웹훅 처리중 토큰이 유효하지 않습니다.(" + "구매한 box pk : " + webhookPurchaseResponse.getEvent().getSubscriberAttributes().getBoxPk().getValue() + ")" + "(" + "구매자 pk : " + webhookPurchaseResponse.getEvent().getOwnerMemberId() + ")");
//      throw new ValidationException(ErrorCode.TOKEN_INVALID);
//    }


    // 2. 멤버가 존재하는지 확인
    log.info("[2. 멤버가 존재하는지 확인]");
    String ownerMemberId = webhookPurchaseResponse.getEvent().getOwnerMemberId();
    Long ownerMemberIdToLong = Long.parseLong(ownerMemberId);
    Member member = memberRepository.findById(ownerMemberIdToLong)
        .orElseThrow(() -> new ValidationException(ErrorCode.USER_NOT_FOUND));

    // 3. 박스가 존재하는지 확인
    log.info("[3. 박스가 존재하는지 확인]");
    String boxPk = webhookPurchaseResponse.getEvent().getSubscriberAttributes().getBoxPk().getValue();
    Long boxPkToLong = Long.parseLong(boxPk);
    Box box = boxRepository.findById(boxPkToLong)
        .orElseThrow(() -> new ValidationException(ErrorCode.BOX_NOT_FOUND));

    // 4. box_subscription 테이블 업데이트 또는 삽입
    log.info("[4. box_subscription 테이블 업데이트 또는 삽입]");
    String productId = webhookPurchaseResponse.getEvent().getProductId();
    PaymentStore store = webhookPurchaseResponse.getEvent().getStore();
    SubscriptionStatus subscriptionStatus = SubscriptionStatus.ACTIVE;
    LocalDateTime startedAt = LocalDateTime.now();
    LocalDateTime expiredAt = LocalDateTime.now().plusMonths(1);
    Double price = webhookPurchaseResponse.getEvent().getPrice();

    BoxSubscription boxSubscription = BoxSubscription
        .of(member, box, productId, store, subscriptionStatus, startedAt, expiredAt, price);

    SubscriptionEventLog subscriptionEventLog = SubscriptionEventLog.from(webhookPurchaseResponse, member, box);

    // 5. 박스 구독 정보 저장
    log.info("[5. 박스 구독 정보 저장]");
    if(boxSubscriptionRepository.findByBoxPk(boxPkToLong).isPresent()) {
      log.info("[5. 박스 구독 정보 저장] - 기존 구독 정보가 존재하여 업데이트를 진행합니다.(" + "구매한 box pk : " + boxPk + ")" + "(" + "구매자 pk : " + ownerMemberId + ")");
      BoxSubscription oldBoxSubscription = boxSubscriptionRepository.findByBoxPk(boxPkToLong)
          .orElseThrow(() -> new ValidationException(ErrorCode.BOX_NOT_FOUND));
      BoxSubscription updatedBoxSubscription = oldBoxSubscription.replaceTo(boxSubscription);
      boxSubscriptionRepository.save(updatedBoxSubscription);
    } else {
      log.info("[5. 박스 구독 정보 저장] - 새로운 구독 정보를 저장합니다.(" + "구매한 box pk : " + boxPk + ")" + "(" + "구매자 pk : " + ownerMemberId + ")");
      boxSubscriptionRepository.save(boxSubscription);
    }

    // 6. 박스 구독 로그 저장
    log.info("[6. 박스 구독 로그 저장]");
    log.info("[6. 박스 구독 로그 저장] - 구매 로그를 저장합니다.(" + "구매한 box pk : " + boxPk + ")" + "(" + "구매자 pk : " + ownerMemberId + ")");
    subscriptionEventLogRepository.save(subscriptionEventLog);

    // 7. 최종 BOX 구독상태 변경
    log.info("[7. 최종 BOX 구독상태 변경]");
    box.subscribe();

  }

  @Transactional
  public PaymentSyncResponse syncPayment(Long memberPk, Long boxPk) {
    log.info("[결제 동기화 시작] memberPk={}, boxPk={}", memberPk, boxPk);

    Member member = memberRepository.findById(memberPk)
            .orElseThrow(() -> new ValidationException(ErrorCode.USER_NOT_FOUND));

    Box box = boxRepository.findActiveBoxByIdWithLock(boxPk)
            .orElseThrow(() -> new ValidationException(ErrorCode.BOX_NOT_FOUND));

    validateBoxOwner(memberPk, box);

    String appUserId = String.valueOf(memberPk);

    RevenueCatSubscriberResponse rcResponse = revenueCatClient.getSubscriber(appUserId);
    if (rcResponse == null || rcResponse.getSubscriber() == null) {
      throw new ValidationException(ErrorCode.INVALID_REQUEST);
    }

    RevenueCatSubscriberResponse.Subscriber subscriber = rcResponse.getSubscriber();

    String rcBoxPk = extractBoxPk(subscriber);
    validateRequestedBox(boxPk, rcBoxPk);

    RevenueCatSubscriberResponse.Entitlement premiumEntitlement =
            extractPremiumEntitlement(subscriber.getEntitlements());

    boolean premiumActive = premiumEntitlement != null;

    LocalDateTime startedAt = null;
    LocalDateTime expiredAt = null;
    String productId = null;
    Double price = null;

    if (premiumActive) {
      startedAt = parseDateTime(premiumEntitlement.getPurchaseDate());
      expiredAt = parseDateTime(premiumEntitlement.getExpiresDate());
      productId = premiumEntitlement.getProductIdentifier();
    }

    BoxSubscription boxSubscription = boxSubscriptionRepository
            .findByOwnerMemberIdAndBoxPk(memberPk, boxPk)
            .orElseGet(() -> createEmptySubscription(member, box));

    boxSubscription = updateSubscription(
            boxSubscription,
            productId,
            PaymentStore.APP_STORE,
            premiumActive,
            startedAt,
            expiredAt,
            price
    );
    boxSubscriptionRepository.save(boxSubscription);

    updateBoxPremium(box, premiumActive);

    saveSyncLog(
            member,
            box,
            productId,
            PaymentStore.APP_STORE,
            price,
            premiumActive,
            null,
            String.valueOf(memberPk)
    );

    return PaymentSyncResponse.builder()
            .boxPk(boxPk)
            .premium(premiumActive)
            .productId(productId)
            .store(PaymentStore.APP_STORE.name())
            .startedAt(startedAt)
            .expiredAt(expiredAt)
            .syncedFrom("MANUAL_SYNC")
            .build();
  }

  private void validateBoxOwner(Long memberPk, Box box) {
    if (box.getOwnerPk() == null || !box.getOwnerPk().equals(memberPk)) {
      throw new ValidationException(ErrorCode.FORBIDDEN);
    }
  }

  private String extractBoxPk(RevenueCatSubscriberResponse.Subscriber subscriber) {
    Map<String, RevenueCatSubscriberResponse.SubscriberAttribute> attrs = subscriber.getSubscriberAttributes();
    if (attrs == null || !attrs.containsKey("boxPK") || attrs.get("boxPK") == null) {
      throw new ValidationException(ErrorCode.INVALID_REQUEST);
    }
    return attrs.get("boxPK").getValue();
  }

  private void validateRequestedBox(Long requestedBoxPk, String rcBoxPk) {
    if (rcBoxPk == null || !requestedBoxPk.equals(Long.parseLong(rcBoxPk))) {
      throw new ValidationException(ErrorCode.INVALID_REQUEST);
    }
  }

  private RevenueCatSubscriberResponse.Entitlement extractPremiumEntitlement(
          Map<String, RevenueCatSubscriberResponse.Entitlement> entitlements) {

    if (entitlements == null || entitlements.isEmpty()) {
      return null;
    }

    return entitlements.get("premium");
  }

  private LocalDateTime parseDateTime(String dateTime) {
    if (dateTime == null) {
      return null;
    }
    return OffsetDateTime.parse(dateTime)
            .atZoneSameInstant(ZoneId.systemDefault())
            .toLocalDateTime();
  }

  private BoxSubscription createEmptySubscription(Member member, Box box) {
    return BoxSubscription.of(
            member,
            box,
            null,
            null,
            SubscriptionStatus.EXPIRED,
            null,
            null,
            null
    );
  }

  private BoxSubscription updateSubscription(
          BoxSubscription subscription,
          String productId,
          PaymentStore store,
          boolean premiumActive,
          LocalDateTime startedAt,
          LocalDateTime expiredAt,
          Double price
  ) {
    subscription.sync(
            productId,
            store,
            premiumActive ? SubscriptionStatus.ACTIVE : SubscriptionStatus.EXPIRED,
            startedAt,
            expiredAt,
            price
    );

    return subscription;
  }

  private void updateBoxPremium(Box box, boolean premiumActive) {
    if (premiumActive) {
      box.subscribe();
    } else {
      box.unsubscribe();
    }
    boxRepository.save(box);
  }

  private void saveSyncLog(
          Member member,
          Box box,
          String productId,
          PaymentStore store,
          Double price,
          boolean premiumActive,
          String appId,
          String appUserId
  ) {

    SubscriptionEventLog logEntity = SubscriptionEventLog.of(
            member,
            box,
            productId,
            store,
            premiumActive ? SubscriptionStatus.ACTIVE : SubscriptionStatus.EXPIRED,
            null,           // cancelledAt
            null,           // refundedAt
            price,
            appId,
            appUserId,
            premiumActive ? PaymentEventType.INITIAL_PURCHASE : PaymentEventType.EXPIRATION,
            null            // payload (sync에서는 raw payload 없음)
    );

    subscriptionEventLogRepository.save(logEntity);
  }

  /**
   * HTTP 요청 헤더에서 JWT 토큰을 추출하는 메서드입니다.
   *
   * @param request HTTP 요청
   * @return 추출된 JWT 토큰
   */
  public String extractToken(HttpServletRequest request) {
    String authHeader = request.getHeader("Authorization");

    // Authorization 헤더가 없거나 "Bearer "로 시작하지 않는 경우 예외 처리
    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
      log.error("[1. 토큰 검증] Authorization 헤더가 없거나 Bearer 로 시작하지 않습니다.");
      throw new ValidationException(ErrorCode.TOKEN_EXTRACTION_FAILED);
    }

    String token = authHeader.substring(7);

    return token;
  }

  /**
   * 구매 로그를 조회하는 메서드입니다.
   *
   * @param bokPk 조회하고자 하는 박스 pk 입니다.
   * @return 정제된 구매 로그 리스트 입니다.
   */
  public List<PurchaseLogResponse> getPurchaseLog(Long bokPk) {

    Box box = boxRepository.findById(bokPk)
        .orElseThrow(() -> new NotFoundException(ErrorCode.BOX_NOT_FOUND));

    List<SubscriptionEventLog> subscriptionEventLogs = subscriptionEventLogRepository.findAllByBoxPk(bokPk);

    if(subscriptionEventLogs.isEmpty()) {
      return List.of();
    }

    List<PurchaseLogResponse> responses = subscriptionEventLogs
        .stream()
        .map(log -> {
          PurchaseLogResponse purchaseLogResponse = PurchaseLogResponse
              .builder()
              .purchasedAt(log.getCreatedAt())
              .productId(log.getProductId())
              .price(log.getPrice())
              .build();
          return purchaseLogResponse;
        }).toList();

    return responses;
  }

}
