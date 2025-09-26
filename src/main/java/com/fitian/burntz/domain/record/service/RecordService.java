package com.fitian.burntz.domain.record.service;

import com.fitian.burntz.domain.box.enums.MemberRole;
import com.fitian.burntz.domain.box.repository.BoxRepository;
import com.fitian.burntz.domain.classes.entity.Classes;
import com.fitian.burntz.domain.classes.repository.ClassesRepository;
import com.fitian.burntz.domain.member.entity.Member;
import com.fitian.burntz.domain.member.entity.MemberList;
import com.fitian.burntz.domain.member.repository.MemberListRepository;
import com.fitian.burntz.domain.record.entity.Record;
import com.fitian.burntz.domain.record.ranking.RankingScoreEncoder;
import com.fitian.burntz.domain.record.repository.RecordRepository;
import com.fitian.burntz.domain.record.v1.dto.RecordCreateRequest;
import com.fitian.burntz.domain.record.v1.dto.RecordResponse;
import com.fitian.burntz.domain.record.v1.dto.RecordUpdateRequest;
import com.fitian.burntz.domain.wod.entity.Wod;
import com.fitian.burntz.domain.wod.enums.WodType;
import com.fitian.burntz.domain.wod.repository.WodRespository;
import com.fitian.burntz.global.common.entity.BaseTime;
import com.fitian.burntz.global.exception.ErrorCode;
import com.fitian.burntz.global.exception.ValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import com.fitian.burntz.domain.record.service.RankingService.RankingRow;
import com.fitian.burntz.domain.record.service.RankingService.RankingSnapshot;
import com.fitian.burntz.domain.record.service.RankingQueryService.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
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
    private final MemberListRepository memberListRepository;
    private final ClassesRepository classesRepository;
    private final RecordRepository recordRepository;

    private final RankingService rankingService;
    private  final RankingQueryService rankingQueryService;
    private final RankingScoreEncoder encoder;
    /*
     * record 생성
     * */
    @Transactional
    public void createRecord(RecordCreateRequest req, LocalDate date, Long boxPk, Long memberPk) {
        //wod 조회를 wodPk가 아니라 box, date로 체크하기
        /* 체험하러 온 사람이 있을 수도 있다 -> 일일체험자는 memberListPk가 null이면서 nickname 필드에 값이 들어감.
         * box에 등록된 사람이라면 memberListPk가 값이 들어가고, nickname필드에 boxNickname이 들어감
         * 클래스 1번 당 운동기록 1개
         * */
        //1. 해당 box에 등록된 매니저, 오너만 pass 되도록 유효성 검증(로그인한 유저)
        requireManagerOrOwner(memberPk, boxPk);

        //2. wod 유효성 검증 : box+date로 조회
        Wod wod = requireActiveWod(boxPk, date);

        //4. classes 존재 및 소속 검증
        Classes classes =classesRepository.findByClassesPkAndBoxBoxPkAndDeletedYN(req.getClassesPk(), boxPk, BaseTime.Yn.N)
                .orElseThrow(()-> new ValidationException(ErrorCode.CLASS_NOT_FOUND));

        //5. memberListPk/nickname 규칙 검사 (둘다 있거나 둘다 없으면 에러)
        if (req.getMemberListPk() != null && req.getNickname() != null && !req.getNickname().isBlank()) {
            throw new ValidationException(ErrorCode.DUPLICATED_NICKNAME_MEMBERPK);
        }
        if (req.getMemberListPk() == null && (req.getNickname() == null || req.getNickname().isBlank())) {
            throw new ValidationException(ErrorCode.EMPTY_NICKNAME_MEMBERPK);
        }

        //6. 대상 멤버 검증(회원일 경우)
        MemberList targetMember = null;
        String nicknameFromMemberList = null;
        if (req.getMemberListPk() != null) {
            // memberListPK가 존재하면 해당 memberList 객체 저장
            MemberList memberList = memberListRepository
                    .findByMemberListPkAndBoxBoxPkAndDeletedYN(req.getMemberListPk(), boxPk, BaseTime.Yn.N)
                    .orElseThrow(() -> new ValidationException(ErrorCode.MEMBER_NOT_IN_BOX));

            targetMember = memberList;
            nicknameFromMemberList = memberList.getBoxNickname();
        }

        //7. 운동기록이 작성될 유저가 특정 클래스에 이미 운동기록이 작성되어 있는지 확인(클래스 1번당 운동기록 1개)->memberPk를 가진 필드만 확인
        if (targetMember != null) {
            boolean dub = recordRepository.existsByClassesClassesPkAndMemberListMemberListPkAndDeletedYN(req.getClassesPk(), targetMember.getMemberListPk(), BaseTime.Yn.N);
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

        // IMPORTANT: DB 커밋 이후에 Redis 반영을 하도록 등록 (트랜잭션 안전)
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    rankingService.upsert(record);
                    System.out.println("Ranking upsert failed after create");
                } catch (Exception e) {
                    log.warn("Ranking upsert failed after create (box={}, date={}, recordPk={}): {}",
                            boxPk, date, record.getRecordPk(), e.toString());

                }
            }
        });
    }


    /*
     * record 목록 조회(Redis 우선 -> db 폴백)
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
        WodType type =wod.getWodType();

        // redis 우선 조회
        List<RankingRow> rows = rankingService.getRanking(boxPk, date, () -> rankingQueryService.rebuildFromDb(boxPk, date, type));


        //rows -> record 목록으로 변환 (DB에서 한 번에 조회)
        List<Long> ids = rows.stream().map(RankingRow::getRecordPk).toList();
        if (ids.isEmpty()) return Collections.emptyList();

        List<Record> records = recordRepository.findAllByRecordPkInWithJoins(ids);
        Map<Long, Record> byId = records.stream().collect(Collectors.toMap(Record::getRecordPk, Function.identity()));

        List<RecordResponse> result = new ArrayList<>(rows.size());
        for (RankingRow r : rows) {
            Record rec = byId.get(r.getRecordPk());
            if (rec == null) continue;
            result.add(RecordResponse.fromWithRank(rec, r.getRank()));  //랭킹포함
        }
        return result;
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
        if (!record.getWod().getBox().getBoxPk().equals(boxPk)
                || !record.getWod().getWodDate().equals(date)) {
            throw new ValidationException(ErrorCode.INVALID_REQUEST);
        }

        //4. memberPk/nickname 규칙 검사 (둘다 있거나 둘다 없으면 에러)
        validateMemberOrNickname(req);

        // 이전 상태 스냅샷 생성 (삭제/제거에 사용)
        RankingSnapshot beforeSnapshot = RankingSnapshot.fromRecord(record);

        // 5. targetMemberList (null 허용) 및 nickname 결정 (null => 변경 없음)
        MemberList targetMemberList = null;
        String nicknameToSet = null;


        if (req.getMemberListPk() != null) {
            // mmemberPk가 있으면 memberList에서 조회해서 member와 box-별 닉네임을 얻음
            MemberList memberList = memberListRepository
                    .findByMemberListPkAndBoxBoxPkAndDeletedYN(req.getMemberListPk(), boxPk, BaseTime.Yn.N)
                    .orElseThrow(() -> new ValidationException(ErrorCode.MEMBER_NOT_IN_BOX));

            //자기자신제외(recordPk) 중복 운동기록 있는지 확인
            boolean dup = recordRepository.existsByClassesClassesPkAndMemberListMemberListPkAndDeletedYNAndRecordPkNot
                    (record.getClasses().getClassesPk(),req.getMemberListPk(),BaseTime.Yn.N,recordPk);

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

        // 트랜잭션 커밋 후에 레디스 동기화 (기존 스냅샷 제거 후 새 값 upsert)
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    // remove old snapshot (정확히 이전 문자열 제거)
                    rankingService.remove(beforeSnapshot);
                } catch (Exception e) {
                    log.warn("Ranking remove(before) failed for update recordPk={}: {}", recordPk, e.toString());
                }
                try {
                    rankingService.upsert(record);
                } catch (Exception e) {
                    log.warn("Ranking upsert(after update) failed for recordPk={}: {}", recordPk, e.toString());
                }
            }
        });
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

        // 삭제 전 snapshot(현재 항목이 레디스에 있는 문자열을 Build하는데 사용)
        RankingSnapshot snapshot = RankingSnapshot.fromRecord(record);

        //delete
        record.markDeleted();
        recordRepository.flush();

        // 커밋 이후에 레디스에서 제거
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    rankingService.remove(snapshot);
                } catch (Exception e) {
                    log.warn("Ranking remove failed after delete recordPk={}: {}", recordPk, e.toString());
                }
            }
        });
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
        boolean hasMemberList = req.getMemberListPk() != null;
        boolean hasNickname = req.getNickname() != null && !req.getNickname().isBlank();

        if (hasMemberList && hasNickname) {
            throw new ValidationException(ErrorCode.DUPLICATED_NICKNAME_MEMBERPK);
        }
        if (!hasMemberList && !hasNickname) {
            throw new ValidationException(ErrorCode.EMPTY_NICKNAME_MEMBERPK);
        }
    }

}