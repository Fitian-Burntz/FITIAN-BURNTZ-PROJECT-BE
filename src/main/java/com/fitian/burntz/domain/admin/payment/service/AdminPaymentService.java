package com.fitian.burntz.domain.admin.payment.service;

import com.fitian.burntz.domain.admin.dto.response.AdminBoxesResponse;
import com.fitian.burntz.domain.admin.dto.response.AdminPurchaseLogResponse;
import com.fitian.burntz.domain.box.entity.Box;
import com.fitian.burntz.domain.box.entity.BoxSubscription;
import com.fitian.burntz.domain.box.entity.SubscriptionEventLog;
import com.fitian.burntz.domain.box.enums.SubscriptionStatus;
import com.fitian.burntz.domain.box.repository.BoxRepository;
import com.fitian.burntz.domain.box.repository.BoxSubscriptionRepository;
import com.fitian.burntz.domain.box.repository.SubscriptionEventLogRepository;
import com.fitian.burntz.domain.member.entity.Member;
import com.fitian.burntz.domain.member.repository.MemberListRepository;
import com.fitian.burntz.global.exception.ErrorCode;
import com.fitian.burntz.global.exception.NotFoundException;
import com.fitian.burntz.global.exception.ValidationException;
import com.fitian.burntz.infra.payment.dto.response.PurchaseLogResponse;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class AdminPaymentService {

    private final BoxRepository boxRepository;
    private final BoxSubscriptionRepository boxSubscriptionRepository;
    private final SubscriptionEventLogRepository subscriptionEventLogRepository;
    private final MemberListRepository memberListRepository;

    @Transactional(readOnly = true)
    public List<AdminBoxesResponse> getBoxes() {
        List<Box> boxes = boxRepository.findAllOrderByBoxPkAsc();
        Map<Long, Long> memberCountMap = memberListRepository.countActiveMembersGroupByBoxPk()
                .stream().collect(Collectors.toMap(r -> (Long) r[0], r -> (Long) r[1]));
        return boxes.stream()
                .map(box -> AdminBoxesResponse.builder()
                        .boxPk(box.getBoxPk())
                        .boxName(box.getBoxName())
                        .memberCount(memberCountMap.getOrDefault(box.getBoxPk(), 0L).intValue())
                        .build())
                .toList();
    }

    @Transactional(readOnly = true)
    public AdminPurchaseLogResponse getPurchaseLog(Long boxPk) {
        BoxSubscription boxSubscription = boxSubscriptionRepository.findByBoxPk(boxPk)
                .orElseThrow(() -> new NotFoundException(ErrorCode.BOX_NOT_FOUND));

        List<PurchaseLogResponse> logs = subscriptionEventLogRepository.findAllByBoxPk(boxPk)
                .stream()
                .map(l -> PurchaseLogResponse.builder()
                        .purchasedAt(l.getCreatedAt())
                        .productId(l.getProductId())
                        .price(l.getPrice())
                        .build())
                .toList();

        return AdminPurchaseLogResponse.builder()
                .startedAt(boxSubscription.getStartedAt())
                .currentAt(LocalDateTime.now())
                .expiredAt(boxSubscription.getExpiredAt())
                .status(boxSubscription.getStatus())
                .logs(logs)
                .build();
    }

    @Transactional
    public void updatePurchaseStatus(Long boxPk, String status) {
        SubscriptionStatus nextStatus = parseStatus(status);

        BoxSubscription boxSubscription = boxSubscriptionRepository.findByBoxPk(boxPk)
                .orElseThrow(() -> new NotFoundException(ErrorCode.BOX_NOT_FOUND));

        Member member = boxSubscription.getMember();
        Box box = boxSubscription.getBox();

        boxSubscription.updateStatus(nextStatus);

        if (nextStatus == SubscriptionStatus.ACTIVE) {
            box.subscribe();
        } else {
            box.unsubscribe();
        }

        subscriptionEventLogRepository.save(SubscriptionEventLog.of(
                member, box,
                boxSubscription.getProductId() + "(관리자에 의해 " + status + " 처리됨)",
                boxSubscription.getStore(),
                nextStatus,
                null, null, 0.0, null,
                boxSubscription.getMember().getMemberId(),
                null, null
        ));

        log.info("[Admin] 박스 구독 상태 변경 boxPk={} status={}", boxPk, nextStatus);
    }

    private SubscriptionStatus parseStatus(String status) {
        return switch (status) {
            case "활성" -> SubscriptionStatus.ACTIVE;
            case "만료" -> SubscriptionStatus.EXPIRED;
            case "취소" -> SubscriptionStatus.CANCELED;
            case "환불" -> SubscriptionStatus.REFUNDED;
            case "보류" -> SubscriptionStatus.PENDING;
            default -> throw new ValidationException(ErrorCode.INVALID_INPUT_VALUE);
        };
    }
}
