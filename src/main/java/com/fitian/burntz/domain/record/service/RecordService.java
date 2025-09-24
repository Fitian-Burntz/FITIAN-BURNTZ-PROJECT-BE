package com.fitian.burntz.domain.record.service;

import com.fitian.burntz.domain.box.enums.MemberRole;
import com.fitian.burntz.domain.box.repository.BoxRepository;
import com.fitian.burntz.domain.classes.entity.Classes;
import com.fitian.burntz.domain.classes.repository.ClassesRepository;
import com.fitian.burntz.domain.member.entity.Member;
import com.fitian.burntz.domain.member.entity.MemberList;
import com.fitian.burntz.domain.member.repository.MemberListRepository;
import com.fitian.burntz.domain.record.entity.Record;
import com.fitian.burntz.domain.record.repository.RecordRepository;
import com.fitian.burntz.domain.record.v1.dto.RecordCreateRequest;
import com.fitian.burntz.domain.record.v1.dto.RecordResponse;
import com.fitian.burntz.domain.record.v1.dto.RecordUpdateRequest;
import com.fitian.burntz.domain.wod.entity.Wod;
import com.fitian.burntz.domain.wod.repository.WodRespository;
import com.fitian.burntz.global.common.entity.BaseTime;
import com.fitian.burntz.global.exception.ErrorCode;
import com.fitian.burntz.global.exception.ValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

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
    private final RecordRepository recordRepository;
    /*
     * record 생성
     * */
    @Transactional
    public void createRecord(RecordCreateRequest req, LocalDate date, Long boxPk, Long memberPk) {
        //wod 조회를 wodPk가 아니라 box, date로 체크하기
        /* 체험하러 온 사람이 있을 수도 있다 -> 일일체험자는 memberPk가 null이면서 name 필드에 값이 들어감.
         * box에 등록된 사람이라면 memberPk에 값이 들어가고, name필드에 null이 들어감.
         * 클래스 1번 당 운동기록 1개
         * */
        //1. 해당 box에 등록된 매니저, 오너만 pass 되도록 유효성 검증(로그인한 유저)
        requireManagerOrOwner(memberPk, boxPk);

        //2. wod 유효성 검증 : box+date로 조회
        Wod wod = requireActiveWod(boxPk, date);

        //4. classes 존재 및 소속 검증
        Classes classes =classesRepository.findByClassesPkAndBoxBoxPkAndDeletedYN(req.getClassesPk(), boxPk, BaseTime.Yn.N)
                .orElseThrow(()-> new ValidationException(ErrorCode.CLASS_NOT_FOUND));

        //5. memberPk/nickname 규칙 검사 (둘다 있거나 둘다 없으면 에러)
        if (req.getMemberPk() != null && req.getNickname() != null && !req.getNickname().isBlank()) {
            throw new ValidationException(ErrorCode.DUPLICATED_NICKNAME_MEMBERPK);
        }
        if (req.getMemberPk() == null && (req.getNickname() == null || req.getNickname().isBlank())) {
            throw new ValidationException(ErrorCode.EMPTY_NICKNAME_MEMBERPK);
        }

        //6. 대상 멤버 검증(회원일 경우)
        //memberList에서 member와 nickname을 가져옴.
        Member targetMember = null;
        String nicknameFromMemberList = null;
        if (req.getMemberPk() != null) {
            // memberList에서 조회해서 member와 box-별 닉네임을 얻음
            MemberList memberList = memberListRepository
                    .findByMemberMemberPkAndBoxBoxPkAndDeletedYN(req.getMemberPk(), boxPk, BaseTime.Yn.N)
                    .orElseThrow(() -> new ValidationException(ErrorCode.MEMBER_NOT_IN_BOX));

            targetMember = memberList.getMember();
            nicknameFromMemberList = memberList.getBoxNickname();
        }

        //7. 운동기록이 작성될 유저가 특정 클래스에 이미 운동기록이 작성되어 있는지 확인(클래스 1번당 운동기록 1개)->memberPk를 가진 필드만 확인
        if (req.getMemberPk() != null) {
            boolean dub = recordRepository.existsByClassesClassesPkAndMemberMemberPkAndDeletedYN(req.getClassesPk(), targetMember.getMemberPk(), BaseTime.Yn.N);
            if(dub){
                throw new ValidationException(ErrorCode.ALREADY_EXISTS_RECORD_FOR_CLASS);
            }
        }

        //엔티티 저장
        Record record = req.toEntity(wod, classes, targetMember, nicknameFromMemberList);

        //동시성 대비
        try {
            recordRepository.save(record);
        } catch (DataIntegrityViolationException ex) {
            // DB 유니크 제약 위반 등 동시성 문제로 인해 발생할 수 있음
            throw new ValidationException(ErrorCode.ALREADY_EXISTS_RECORD_FOR_CLASS);
        }
    }

    /*
     * record 목록 조회
     * */
    @Transactional(readOnly = true)
    public List<RecordResponse> getRecord(Long boxPk, Long memberPk, LocalDate date){

        //1. 해당 box에 소속된 멤버인지 검증
         // memberList 엔티티에서 해당 멤버가 box에 속하는지 확인하고 Member를 가져오는 패턴
        MemberList memberList = memberListRepository
            .findRoleByMemberMemberPkAndBoxBoxPkAndDeletedYN(memberPk, boxPk, BaseTime.Yn.N)
            .orElseThrow(() -> new ValidationException(ErrorCode.MEMBER_NOT_IN_BOX));

        Member member = memberList.getMember();
        if (member == null) { // 안전장치
            throw new ValidationException(ErrorCode.MEMBER_NOT_IN_BOX);
        }

        //2. wod 존재 및 소속 검증
        Wod wod = requireActiveWod(boxPk, date);

        //해당 날짜의 record 모두 조회
        List<Record> records = recordRepository.findAllByWodWithMemberAndClasses(wod, BaseTime.Yn.N);

        return records.stream()
                .map(RecordResponse::from)
                .collect(Collectors.toList());
    }

    /*
    * Record 수정
    * */
    @Transactional
    public void updateRecord(Long boxPk, Long memberPk, Long recordPk, LocalDate date, RecordUpdateRequest req) {
        //1. 해당 box에 등록된 매니저, 오너만 pass 되도록 유효성 검증(로그인한 유저)
        requireManagerOrOwner(memberPk, boxPk);

        // 2. 대상 레코드 조회 (Wod & Box fetch)
        Record record = recordRepository.findByIdWithWodAndBox(recordPk)
                .orElseThrow(() -> new ValidationException(ErrorCode.RECORD_NOT_FOUND));

        // 3. path의 box/date와 일치하는지 검증
        Long recordBoxPk = record.getWod().getBox().getBoxPk();
        LocalDate recordDate = record.getWod().getWodDate();
        if (!recordBoxPk.equals(boxPk) || !recordDate.equals(date)) {
            throw new ValidationException(ErrorCode.INVALID_REQUEST);
        }

        //4. memberPk/nickname 규칙 검사 (둘다 있거나 둘다 없으면 에러)
        validateMemberOrNickname(req);

        // 5. targetMemberList (null 허용) 및 nickname 결정 (null => 변경 없음)
        MemberList targetMemberList = null;
        String nicknameToSet = null;


        if (req.getMemberPk() != null) {
            // mmemberPk가 있으면 memberList에서 조회해서 member와 box-별 닉네임을 얻음
            MemberList memberList = memberListRepository
                    .findByMemberMemberPkAndBoxBoxPkAndDeletedYN(req.getMemberPk(), boxPk, BaseTime.Yn.N)
                    .orElseThrow(() -> new ValidationException(ErrorCode.MEMBER_NOT_IN_BOX));

            //자기자신제외(recordPk) 중복 운동기록 있는지 확인
            boolean dup = recordRepository.existsByClassesClassesPkAndMemberMemberPkAndDeletedYNAndRecordPkNot
                    (record.getClasses().getClassesPk(),req.getMemberPk(),BaseTime.Yn.N,recordPk);

            if(dup){
                throw new ValidationException(ErrorCode.ALREADY_EXISTS_RECORD_FOR_CLASS);
            }

            targetMemberList = memberList;
        } else {
            // memberPk == null => 비회원으로 전환 또는 유지
            if (req.getNickname() != null) {
                // 클라이언트가 명시적으로 nickname을 보내면 그 값으로 변경(빈 문자열도 허용)
                nicknameToSet = req.getNickname();
            }
            // nicknameToSet == null -> nickname 변경 없음
        }

        //동시성 대비
        try {
            req.applyTo(record, targetMemberList, nicknameToSet);
            recordRepository.flush();
        } catch (DataIntegrityViolationException ex) {
            // DB 유니크 제약 위반 등 동시성 문제로 인해 발생할 수 있음
            throw new ValidationException(ErrorCode.ALREADY_EXISTS_RECORD_FOR_CLASS);
        }
    }

    /*
    * Record 삭제
    * */
    @Transactional
    public void deleteRecord(Long boxPk, Long memberPk, Long recordPk, LocalDate date){
        //1. 해당 box에 등록된 매니저, 오너만 pass 되도록 유효성 검증(로그인한 유저)
        requireManagerOrOwner(memberPk, boxPk);

        //2. wod 유효성 검증 : box+date로 조회
        Wod wod = requireActiveWod(boxPk, date);

        Record record = recordRepository.findById(recordPk)
                .orElseThrow(() -> new ValidationException(ErrorCode.RECORD_NOT_FOUND));

        //delete
        record.markDeleted();
    }


    /*
     * 유효성 검증 헬퍼 메서드
     * */

    //해당 멤버가 박스에 속해있는지 & 해당 Box의 매니저와 오너인지 검증하는 헬퍼 메서드
    private void requireManagerOrOwner(Long memberPk, Long boxPk) {
        MemberList memberList = memberListRepository
                .findRoleByMemberMemberPkAndBoxBoxPkAndDeletedYN(memberPk, boxPk, BaseTime.Yn.N)
                .orElseThrow(() -> new ValidationException(ErrorCode.MEMBER_NOT_IN_BOX));

        if (memberList.getRole() == MemberRole.GUEST || memberList.getRole() == MemberRole.MEMBER) {
            throw new ValidationException(ErrorCode.ACCESS_DENIED);
        }
    }

    //Wod 유효성 검증(box+date로 체크)
    private Wod requireActiveWod(Long boxPk, LocalDate date){
        return wodRespository.findByBoxBoxPkAndWodDateAndDeletedYN(boxPk,date,BaseTime.Yn.N)
                .orElseThrow(() -> new ValidationException(ErrorCode.WOD_NOT_FOUND));
    }

    //memberPk/nickname 규칙 검사 (둘다 있거나 둘다 없으면 에러)
    private void validateMemberOrNickname(RecordUpdateRequest req) {
        boolean hasMember = req.getMemberPk() != null;
        boolean hasNickname = req.getNickname() != null && !req.getNickname().isBlank();

        if (hasMember && hasNickname) {
            throw new ValidationException(ErrorCode.DUPLICATED_NICKNAME_MEMBERPK);
        }
        if (!hasMember && !hasNickname) {
            throw new ValidationException(ErrorCode.EMPTY_NICKNAME_MEMBERPK);
        }
    }
}