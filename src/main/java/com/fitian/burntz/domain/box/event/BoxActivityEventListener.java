package com.fitian.burntz.domain.box.event;

import com.fitian.burntz.domain.box.entity.BoxActivity;
import com.fitian.burntz.domain.box.repository.BoxActivityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class BoxActivityEventListener {

    private final BoxActivityRepository boxActivityRepository;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handle(BoxActivityEvent event) {
        try {
            boxActivityRepository.save(BoxActivity.builder()
                    .boxPk(event.getBoxPk())
                    .type(event.getType())
                    .actorPk(event.getActorPk())
                    .actorName(event.getActorName())
                    .targetMemberPk(event.getTargetMemberPk())
                    .targetMemberName(event.getTargetMemberName())
                    .detail(event.getDetail())
                    .build());
        } catch (Exception e) {
            log.error("BoxActivity 저장 실패: type={}, boxPk={}, error={}", event.getType(), event.getBoxPk(), e.getMessage());
        }
    }
}
