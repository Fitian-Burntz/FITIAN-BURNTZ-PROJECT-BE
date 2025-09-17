package com.fitian.burntz.domain.record.service;

import com.fitian.burntz.domain.box.entity.Box;
import com.fitian.burntz.domain.box.enums.MemberRole;
import com.fitian.burntz.domain.box.repository.BoxRepository;
import com.fitian.burntz.domain.classes.entity.Classes;
import com.fitian.burntz.domain.classes.repository.ClassesRepository;
import com.fitian.burntz.domain.member.entity.Member;
import com.fitian.burntz.domain.member.entity.MemberList;
import com.fitian.burntz.domain.member.repository.MemberListRepository;
import com.fitian.burntz.domain.member.repository.MemberRepository;
import com.fitian.burntz.domain.record.v1.dto.RecordCreateRequest;
import com.fitian.burntz.domain.wod.entity.Wod;
import com.fitian.burntz.domain.wod.repository.WodRespository;
import com.fitian.burntz.global.common.entity.BaseTime;
import com.fitian.burntz.global.exception.ErrorCode;
import com.fitian.burntz.global.exception.ValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * @author : 선순주
 * @packageName : com.fitian.burntz.domain.record.v1.service
 * @fileName : RecordService
 * @date : 2025-09-17
 * @description : RecordService
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RecordService {
    private final WodRespository wodRespository;
    private final BoxRepository boxRepository;
    private final MemberListRepository memberListRepository;
    private final ClassesRepository classesRepository;
    private final MemberRepository memberRepository;

    /*
    * record 생성
    * */
    @Transactional
    public void createRecord(RecordCreateRequest req, Long boxPk, Long wodPk, Long ClassesPk ,Long memberPk){
        /* 체험하러 온 사람이 있을 수도 있다 -> 일일체험자는 memberPk가 null이면서 name 필드에 값이 들어감.
        * box에 등록된 사람이라면 memberPk에 값이 들어가고, name필드에 null이 들어감.
        * 클래스 1번 당 운동기록 1개
        * */

        //1. 박스 유효성 검증
        Box box = requireActiveBox(boxPk);

        //2. wod 존재 및 소속 검증(wod가 box에 속하는지)
        Wod wod = requireWodInBox(wodPk, boxPk);

        //3. classes 존재 및 소속 검증
        Classes classes = requireClassesInBox(ClassesPk,boxPk);

        //4. 해당 box에 등록된 매니저, 오너만 pass 되도록 유효성 검증(로그인한 유저)
        requireManagerOrOwner(memberPk, boxPk);

        //5. memberPk/nickname 규칙 검사 (둘다 있거나 둘다 없으면 에러)
        if (req.getMemberPk() != null && req.getNickname() != null && !req.getNickname().isBlank()) {
            throw new ValidationException(ErrorCode.DUPLICATED_NICKNAME_MEMBERPK);
        }
        if (req.getMemberPk() == null && (req.getNickname() == null || req.getNickname().isBlank())) {
            throw new ValidationException(ErrorCode.EMPTY_NICKNAME_MEMBERPK);
        }

        //6. 대상 멤버 검증(회원일 경우)
        Member targetMember = null;
        if (req.getMemberPk() != null) {
            boolean exists = memberListRepository.existsByBoxBoxPkAndMemberMemberPkAndDeletedYN(boxPk, req.getMemberPk(), BaseTime.Yn.N);
            if (!exists) throw new ValidationException(ErrorCode.MEMBER_NOT_IN_BOX);

            targetMember = memberRepository.findById(req.getMemberPk())
                    .orElseThrow(() -> new ValidationException(ErrorCode.MEMBER_NOT_IN_BOX));
        }

        //7. 운동기록이 작성될 유저가 특정 클래스에 이미 운동기록이 작성되어 있는지 확인(클래스 1번당 운동기록 1개)


        //




    }

    /*
     * 유효성 검증 헬퍼 메서드
     * */
    //Box 유효성 검증
    private Box requireActiveBox(Long boxPk) {
        return boxRepository.findByBoxPkAndDeletedYN(boxPk, BaseTime.Yn.N)
                .orElseThrow(() -> new ValidationException(ErrorCode.BOX_NOT_FOUND));
    }

    //해당 멤버가 박스에 속해있는지 & 해당 Box의 매니저와 오너인지 검증하는 헬퍼 메서드
    private void requireManagerOrOwner(Long memberPk, Long boxPk) {
        MemberList memberList = memberListRepository
                .findRoleByMemberMemberPkAndBoxBoxPkAndDeletedYN(memberPk, boxPk, BaseTime.Yn.N)
                .orElseThrow(() -> new ValidationException(ErrorCode.MEMBER_NOT_IN_BOX));

        if (memberList.getRole() == MemberRole.GUEST || memberList.getRole() == MemberRole.MEMBER) {
            throw new ValidationException(ErrorCode.ACCESS_DENIED);
        }
    }

    //Wod 유효성 검증
    private Wod requireActiveWod(Box box, LocalDate date){
        return wodRespository.findByBoxAndWodDateAndDeletedYN(box,date,BaseTime.Yn.N)
                .orElseThrow(() -> new ValidationException(ErrorCode.WOD_NOT_FOUND));
    }

    //Wod 존재 및 소속 검증(wod가 box에 속하는지)
    private Wod requireWodInBox(Long wodPk, Long boxPk){
        return wodRespository.findByWodPkAndBoxBoxPkAndDeletedYN(wodPk, boxPk, BaseTime.Yn.N)
                .orElseThrow(()-> new ValidationException(ErrorCode.WOD_NOT_FOUND));
    }

    //classes 존재 및 소속 검증
    private Classes requireClassesInBox(Long ClassesPk, Long boxPk){
        return classesRepository.findByClassesPkAndBoxBoxPkAndDeletedYN(ClassesPk, boxPk, BaseTime.Yn.N)
                .orElseThrow(()-> new ValidationException(ErrorCode.CLASS_NOT_FOUND));
    }

}