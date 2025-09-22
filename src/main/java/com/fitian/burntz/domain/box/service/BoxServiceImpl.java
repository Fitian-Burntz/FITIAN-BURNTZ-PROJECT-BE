package com.fitian.burntz.domain.box.service;

import com.fitian.burntz.domain.box.dto.BoxDto;
import com.fitian.burntz.domain.box.dto.CreateBoxRequest;
import com.fitian.burntz.domain.box.entity.Box;
import com.fitian.burntz.domain.box.repository.BoxRepository;
import com.fitian.burntz.domain.member.entity.Member;
import com.fitian.burntz.domain.member.entity.MemberList;
import com.fitian.burntz.domain.member.repository.MemberListRepository;
import com.fitian.burntz.domain.member.repository.MemberRepository;
import com.fitian.burntz.domain.member.service.MemberListService;
import com.fitian.burntz.domain.member.service.MemberService;
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
    private final MemberRepository memberRepository;
    private final MemberListService memberListService;

    @Override
    @Transactional(readOnly = true)
    public Page<BoxDto> getAllActiveBoxes(Pageable pageable) {
        Page<Box> page = boxRepository.findAllByDeletedYN(BaseTime.Yn.N, pageable);
        return page.map(BoxDto::from);
    }

    @Override
    public BoxDto createBox(Long ownerPk, CreateBoxRequest createBoxRequest) {
        // 0) owner 존재 확인
        Member owner = memberRepository.findById(ownerPk)
                .orElseThrow(() -> new ValidationException(ErrorCode.USER_NOT_FOUND));

        // boxCode 중복 검사
        if (createBoxRequest.getBoxCode() != null && boxRepository.existsByBoxCode(createBoxRequest.getBoxCode())) {
            throw new ValidationException(ErrorCode.DUPLICATE_BOX_CODE);
        }

        // 새로운 box 엔티티 생성 (정적 팩토리 메서드)
        Box createdBox = Box.create(ownerPk, createBoxRequest);

        try {
            // 박스 저장 (영속화)
            Box savedBox = boxRepository.save(createdBox);
            log.info("Box created: boxPk={} boxCode={} ownerPk={}", savedBox.getBoxPk(), savedBox.getBoxCode(), ownerPk);

            try {
                memberListService.createMemberList(owner, savedBox);
                log.info("Owner added to Box as MemberList: boxPk={} ownerPk={}", savedBox.getBoxPk(), ownerPk);
            } catch (ValidationException e) {
                // MemberList 생성 중 비즈니스 오류 발생 시 로그 후 전파(트랜잭션 전체 롤백)
                log.warn("Failed to create MemberList via service for boxPk={} ownerPk={}: {}", savedBox.getBoxPk(), ownerPk, e.getErrorCode());
                throw e;
            }

            // box 엔티티 dto 로 변환하여 반환
            return BoxDto.from(savedBox);
        }
        catch (DataIntegrityViolationException e) {
            // 데이터 저장중 경쟁상황, 동시성으로 인한 unique 위반 등: 일관된 에러코드로 변환
            log.warn("Failed to save box (possible duplicate box_code): {}", createBoxRequest.getBoxCode(), e);
            throw new ValidationException(ErrorCode.DUPLICATE_BOX_CODE);
        }
    }
}
