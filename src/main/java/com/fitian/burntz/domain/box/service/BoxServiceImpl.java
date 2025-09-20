package com.fitian.burntz.domain.box.service;

import com.fitian.burntz.domain.box.dto.BoxDto;
import com.fitian.burntz.domain.box.dto.CreateBoxRequest;
import com.fitian.burntz.domain.box.entity.Box;
import com.fitian.burntz.domain.box.repository.BoxRepository;
import com.fitian.burntz.global.common.entity.BaseTime;
import com.fitian.burntz.global.exception.ErrorCode;
import com.fitian.burntz.global.exception.ValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class BoxServiceImpl implements BoxService {

    private final BoxRepository boxRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<BoxDto> getAllActiveBoxes(Pageable pageable) {
        Page<Box> page = boxRepository.findAllByDeletedYN(BaseTime.Yn.N, pageable);
        return page.map(BoxDto::from);
    }

    @Override
    public BoxDto createBox(Long ownerPk, CreateBoxRequest createBoxRequest) {
        // UX용 사전 검사
        if (createBoxRequest.getBoxCode() != null && boxRepository.existsByBoxCode(createBoxRequest.getBoxCode())) {
            throw new ValidationException(ErrorCode.DUPLICATE_BOX_CODE);
        }

        // 엔티티 생성
        Box createdBox = Box.create(ownerPk, createBoxRequest);

        try {
            Box savedBox = boxRepository.save(createdBox);
            log.info("Box created: boxPk={} boxCode={} ownerPk={}", savedBox.getBoxPk(), savedBox.getBoxCode(), ownerPk);

            return BoxDto.from(savedBox);
        }
        catch (DataIntegrityViolationException e) {
            // 데이터 저장중 경쟁상황, 동시성으로 인한 unique 위반 등: 일관된 에러코드로 변환
            log.warn("Failed to save box (possible duplicate box_code): {}", createBoxRequest.getBoxCode(), e);
            throw new ValidationException(ErrorCode.DUPLICATE_BOX_CODE);
        }
    }
}
