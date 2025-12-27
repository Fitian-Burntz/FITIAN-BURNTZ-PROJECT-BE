package com.fitian.burntz.domain.box.service;

import com.fitian.burntz.domain.box.dto.*;
import com.fitian.burntz.domain.box.entity.Box;
import com.fitian.burntz.domain.box.repository.BoxRepository;
import com.fitian.burntz.domain.channel.enums.ChannelType;
import com.fitian.burntz.domain.channel.service.ChannelService;
import com.fitian.burntz.domain.channel.v1.dto.ChannelCreateRequest;
import com.fitian.burntz.domain.member.entity.Member;
import com.fitian.burntz.domain.member.entity.MemberList;
import com.fitian.burntz.domain.member.repository.MemberListRepository;
import com.fitian.burntz.domain.member.repository.MemberRepository;
import com.fitian.burntz.domain.member.service.MemberListService;
import com.fitian.burntz.global.common.entity.BaseTime;
import com.fitian.burntz.global.common.util.PreconditionValidator;
import com.fitian.burntz.global.exception.ErrorCode;
import com.fitian.burntz.global.exception.ValidationException;
import com.fitian.burntz.global.security.core.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class BoxServiceImpl implements BoxService {

    private final BoxRepository boxRepository;
    private final ChannelService channelService;
    private final MemberRepository memberRepository;
    private final MemberListService memberListService;
    private final MemberListRepository memberListRepository;
    private final PreconditionValidator preconditionValidator;

    @Override
    @Transactional(readOnly = true)
    public BoxDto getBoxForPk(Long boxPk){
        Box box = boxRepository.findActiveById(boxPk)
                .orElseThrow(() -> new ValidationException(ErrorCode.BOX_NOT_FOUND));

        return BoxDto.from(box);

    }

    @Override
    @Transactional(readOnly = true)
    public BoxDto getBoxForBoxCode(String boxCode){
        Box box = boxRepository.findActiveByBoxCode(boxCode)
                .orElseThrow(() -> new ValidationException(ErrorCode.BOX_NOT_FOUND));

        return BoxDto.from(box);

    }

    @Override
    @Transactional(readOnly = true)
    public Page<BoxDto> getAllActiveBoxes(Pageable pageable) {
        Page<Box> page = boxRepository.findAllByDeletedYN(BaseTime.Yn.N, pageable);
        return page.map(BoxDto::from);
    }

    @Override
    public BoxDto createBox(CreateBoxRequest createBoxRequest, CustomUserDetails userDetails) {
        Long ownerPk = userDetails.getMemberPk();
        // owner 존재 확인
        Member owner = memberRepository.findActiveById(ownerPk)
                .orElseThrow(() -> new ValidationException(ErrorCode.USER_NOT_FOUND));

        // boxCode 중복 검사
        if (createBoxRequest.getBoxCode() != null && checkDuplicateBoxCode(createBoxRequest.getBoxCode())) {
            throw new ValidationException(ErrorCode.DUPLICATE_BOX_CODE);
        }

        // 새로운 box 엔티티 생성 (정적 팩토리 메서드)
        Box createdBox = Box.create(ownerPk, createBoxRequest);

        try {
            // 박스 저장 (영속화)
            Box savedBox = boxRepository.save(createdBox);
            log.info("Box created: boxPk={} boxCode={} ownerPk={}", savedBox.getBoxPk(), savedBox.getBoxCode(), ownerPk);

            try {
                memberListService.createMemberList(owner, savedBox.getBoxPk());
                log.info("Owner added to Box as MemberList: boxPk={} ownerPk={}", savedBox.getBoxPk(), ownerPk);
            } catch (ValidationException e) {
                // MemberList 생성 중 비즈니스 오류 발생 시 로그 후 전파(트랜잭션 전체 롤백)
                log.warn("Failed to create MemberList via service for boxPk={} ownerPk={}: {}", savedBox.getBoxPk(), ownerPk, e.getErrorCode());
                throw e;
            }

            //박스 생성과 함께 채널도 생성
            record ChannelSpec(String name, ChannelType type){}
            String boxCode = createdBox.getBoxCode();
            List<Long> defaultMember = List.of(1L);
            List<ChannelSpec> specs = List.of(
                    new ChannelSpec("NOTICE", ChannelType.NOTICE),
                    new ChannelSpec("GENERAL", ChannelType.GENERAL)
            );

            specs.stream()
                    .map(s -> ChannelCreateRequest.builder()
                            .boxCode(boxCode)
                            .channelId(boxCode+"_"+s.name())
                            .channelName(s.name())
                            .type(s.type())
                            .memberPks(defaultMember)
                            .build())
                    .forEach(req -> channelService.createChannel(req, userDetails));

            // box 엔티티 dto 로 변환하여 반환
            return BoxDto.from(savedBox);
        }
        catch (DataIntegrityViolationException e) {
            // 데이터 저장중 경쟁상황, 동시성으로 인한 unique 위반 등: 일관된 에러코드로 변환
            log.warn("Failed to save box (possible duplicate box_code): {}", createBoxRequest.getBoxCode(), e);
            throw new ValidationException(ErrorCode.DUPLICATE_BOX_CODE);
        }
    }

    @Override
    public JoinBoxDto joinMemberToBox (Long joinMemberPk, String belongBoxCode){
        // 멤버 DB 존재 여부 확인
        Member joinMember = memberRepository.findActiveById(joinMemberPk)
                .orElseThrow(() -> new ValidationException(ErrorCode.USER_NOT_FOUND));

        // BoxCode DB 존재 여부 확인
        Box belongBox = boxRepository.findActiveByBoxCode(belongBoxCode)
                .orElseThrow(() -> new ValidationException(ErrorCode.BOX_CODE_NOT_FOUND));

        // 이미 해당 박스에 멤버가 존재하는지 확인
        if (memberListRepository.existsActiveByBoxPkAndMemberPk(belongBox.getBoxPk(), joinMemberPk)) {
            throw new ValidationException(ErrorCode.DUPLICATE_MEMBER);
        }

        MemberList joinNewMember = MemberList.joinNewMemberToBox(joinMember, belongBox);

        try {
            // 멤버 리스트 저장 (영속화)
            MemberList savedJoinNewMember = memberListRepository.save(joinNewMember);
            log.info("MemberList created: boxPk={} joinMemberPk={} joinMemberRole:{}",
                    savedJoinNewMember.getBox().getBoxPk(), joinMemberPk, savedJoinNewMember.getRole());

            return JoinBoxDto.from(joinMemberPk, belongBoxCode);
        }
        catch (DataIntegrityViolationException e) {
            // 데이터 저장중 경쟁상황, 동시성으로 인한 unique 위반 등: 일관된 에러코드로 변환
            log.warn("Failed to save memberList (possible duplicate memberList): {}", joinNewMember.getMemberListPk(), e);
            throw new ValidationException(ErrorCode.DUPLICATE_BOX_CODE);
        }

    }

    @Override
    public UpdateBoxInfoDto updateBoxInfo(Long operatorPk, UpdateBoxInfoDto updateBoxInfoDto) {

        // 멤버 DB 존재 여부 검사
        memberRepository.findActiveById(operatorPk)
                .orElseThrow(() -> new ValidationException(ErrorCode.USER_NOT_FOUND));

        Long targetBoxPk = updateBoxInfoDto.getBoxPk();

        // box DB 존재 여부 검사
        Box targetBox = boxRepository.findActiveById(targetBoxPk)
                .orElseThrow(() -> new ValidationException(ErrorCode.BOX_NOT_FOUND));

        // 요청자가 update 하려는 box 의 OWNER 인지 권한 체크
        if (!Objects.equals(targetBox.getOwnerPk(), operatorPk)) {
            throw new ValidationException(ErrorCode.FORBIDDEN);
        }

        String updateBoxCode = updateBoxInfoDto.getBoxCode();

        // 기존 boxCode 와 변경하려는 boxCode 가 다르면서 boxCode가 이미 존재하는 값이라면 에러
        if(!Objects.equals(targetBox.getBoxCode(), updateBoxCode) && checkDuplicateBoxCode(updateBoxCode)) {
            throw new ValidationException(ErrorCode.DUPLICATE_BOX_CODE);
        }

        // 엔티티 업데이트 (in-transaction; dirty checking 가능)
        targetBox.updateInfo(updateBoxInfoDto);

        // 저장 및 유니크 레이스 방어: save()에서 발생하는 DataIntegrityViolation 을 잡아 의미있는 에러로 변환
        try {
            Box updatedBox = boxRepository.save(targetBox);
            return UpdateBoxInfoDto.entityToDto(updatedBox);
        } catch (DataIntegrityViolationException e) {
            // getMostSpecificCause() 자체는 null을 반환하지 않으므로 바로 호출.
            String causeMsg = e.getMostSpecificCause().getMessage();
            if (causeMsg == null) {
                causeMsg = e.getMessage();
            }

            log.warn("Failed to save Box (unique?). boxPk={}, attemptedBoxCode={}, ownerPk={}, cause={}",
                    targetBox.getBoxPk(), updateBoxCode, targetBox.getOwnerPk(), causeMsg);

            // 자세한 스택트레이스는 debug에서 확인
            log.debug("DataIntegrityViolationException while saving boxPk={}", targetBox.getBoxPk(), e);

            throw new ValidationException(ErrorCode.DUPLICATE_BOX_CODE);
        }
    }

    @Override
    public void removeBox(Long operatorPk, Long boxPk){

        Long targetBoxPk = preconditionValidator.requireBoxPk(boxPk);
        Box targetBox = boxRepository.findActiveById(targetBoxPk)
                .orElseThrow(() -> new ValidationException(ErrorCode.BOX_NOT_FOUND));

        // 권한 체크 (owner인지 확인)
        if (!Objects.equals(targetBox.getOwnerPk(), operatorPk)) {
            throw new ValidationException(ErrorCode.FORBIDDEN);
        }

        try {
            targetBox.markDeleted();
            boxRepository.save(targetBox);
            log.info("Box soft-deleted. boxPk={}", targetBoxPk);

        } catch (DataIntegrityViolationException dive) {
            // DB 제약조건 위반(UNIQUE / NOT NULL / FK 등)
            dive.getMostSpecificCause();
            log.warn("Failed to soft-delete box. boxPk={}, cause={}",
                    targetBoxPk,
                    dive.getMostSpecificCause().getMessage());
            log.debug("DataIntegrityViolationException stacktrace:", dive);
            throw new ValidationException(ErrorCode.VALIDATION_FAILED);

        } catch (OptimisticLockingFailureException olfe) {
            // 낙관적 락 충돌(@Version 필드 관련)
            log.warn("Optimistic lock during box delete. boxPk={}, cause={}", targetBoxPk, olfe.getMessage());
            log.debug("OptimisticLockingFailureException stacktrace:", olfe);
            throw new ValidationException(ErrorCode.VALIDATION_FAILED);

        } catch (Exception e) {
            // 그 외 예기치 못한 오류(네트워크/타임아웃/NPE 등)
            log.error("Unexpected error while deleting box. boxPk={}", targetBoxPk, e);
            throw new ValidationException(ErrorCode.VALIDATION_FAILED);
        }
    }



    /** 헬퍼 메서드 **/

    /** BoxCode 중복 체크 **/
    private boolean checkDuplicateBoxCode(String boxCode){

        return boxRepository.existsActiveByBoxCode(boxCode);
    }

}
