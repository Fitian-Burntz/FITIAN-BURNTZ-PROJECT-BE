package com.fitian.burntz.domain.record.service;

import com.fitian.burntz.domain.box.enums.MemberRole;
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

import java.time.LocalDate;
import java.util.*;
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

        //8. 엔티티 저장
        Record record = req.toEntity(wod, classes, targetMember, nicknameFromMemberList);

        //동시성 대비
        try {
            recordRepository.save(record);
        } catch (DataIntegrityViolationException ex) {
            // DB 유니크 제약 위반 등 동시성 문제로 인해 발생할 수 있음
            throw new ValidationException(ErrorCode.ALREADY_EXISTS_RECORD_FOR_CLASS);
        }

        //DB 커밋 이후에 Redis 반영을 하도록 등록 (트랜잭션 안전)
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                rankingService.upsert(record);
            }
        });
    }

    /*
     * record 다건 생성
     * */
    @Transactional
    public void createRecords(List<RecordCreateRequest> requests, LocalDate date, Long boxPk, Long memberPk) {

        if (requests == null || requests.isEmpty()) {
            return;
        }

        //1. 해당 box에 등록된 매니저, 오너만 pass 되도록 유효성 검증(로그인한 유저)
        requireManagerOrOwner(memberPk, boxPk);

        //2. wod 유효성 검증 : box+date로 조회
        Wod wod = requireActiveWod(boxPk, date);

        Set<Long> classesPkSet = requests.stream()
                .map(RecordCreateRequest::getClassesPk)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Set<Long> memberListPkSet = requests.stream()
                .map(RecordCreateRequest::getMemberListPk)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Long, Classes> classesMap = new HashMap<>();
        if(!classesPkSet.isEmpty()){
            List<Classes> classesList = classesRepository.findAllByClassesPkInAndBoxBoxPkAndDeletedYN(classesPkSet, boxPk, BaseTime.Yn.N);
            for(Classes c : classesList) {
                classesMap.put(c.getClassesPk(), c);
            }
        }

        Map<Long, MemberList> memberListMap = new HashMap<>();
        if(!memberListPkSet.isEmpty()) {
            List<MemberList> memberLists = memberListRepository.findAllByMemberListPkInAndBoxBoxPkAndDeletedYN(memberListPkSet, boxPk, BaseTime.Yn.N);
            for(MemberList ml : memberLists) {
                memberListMap.put(ml.getMemberListPk(), ml);
            }
        }

        if (!classesPkSet.isEmpty() && !memberListPkSet.isEmpty()) {
            List<Record> existing = recordRepository.findAllByClassesClassesPkInAndMemberListMemberListPkInAndDeletedYN(
                    classesPkSet, memberListPkSet, BaseTime.Yn.N);

            // existing에서 (classesPk#memberListPk) 키를 만들어 집합으로 보관
            Set<String> existingKeys = existing.stream()
                    .filter(r -> r.getClasses() != null && r.getMemberList() != null)
                    .map(r -> r.getClasses().getClassesPk() + "#" + r.getMemberList().getMemberListPk())
                    .collect(Collectors.toSet());

            // 요청 목록을 순회하면서 memberListPk가 있는 경우에는 기존 존재 키와 비교
            for (RecordCreateRequest req : requests) {
                if (req.getMemberListPk() != null) {
                    Long cPk = req.getClassesPk();
                    Long mPk = req.getMemberListPk();
                    // classes 또는 memberList가 null이면 이후 본 로직에서 검증하므로 여기서는 key 비교만
                    String key = cPk + "#" + mPk;
                    if (existingKeys.contains(key)) {
                        throw new ValidationException(ErrorCode.ALREADY_EXISTS_RECORD_FOR_CLASS);
                    }
                }
            }
        }

        List<Record> toSave = new ArrayList<>(requests.size());
        for (RecordCreateRequest req : requests) {
            Classes classes = classesMap.get(req.getClassesPk());
            if(classes == null) {
                throw new ValidationException(ErrorCode.CLASS_NOT_FOUND);
            }

            MemberList memberList = null;
            String nicknameFromMember = null;
            if (req.getMemberListPk() != null) {
                memberList = memberListMap.get(req.getMemberListPk());
                if (memberList == null) {
                    throw new ValidationException(ErrorCode.USER_NOT_FOUND);
                }
                nicknameFromMember = memberList.getBoxNickname();
            }

            // 닉네임 필수 규칙: memberList가 없으면 nickname 필수
            if (memberList == null && (req.getNickname() == null || req.getNickname().isBlank())) {
                throw new ValidationException(ErrorCode.EMPTY_NICKNAME_MEMBERPK);
            }

            Record record = req.toEntity(wod, classes, memberList, nicknameFromMember);

            toSave.add(record);
        }

        List<Record> saved = recordRepository.saveAll(toSave);

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                for (Record r : saved) {
                    try {
                        rankingService.upsert(r);
                    } catch (Exception ex) {
                        log.error("ranking upsert failed for recordPk=" + r.getRecordPk(), ex);
                    }
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

        // 3. redis 우선 조회
        //캐시에 있으면 캐시된 데이터 바로 반환 -> 캐시에 없으면 람다함수 실행하여 DB에서 데이터 조회
        List<Long> recordIds  = rankingService.getRanking(boxPk, date,
                () -> getRankingFromDb(boxPk, date, type));

        if (recordIds.isEmpty()) return Collections.emptyList();

        //4. 추출한 recordPk 목록으로 한번에 조회
        List<Record> records = recordRepository.findAllByRecordPkInWithJoins(recordIds);
        //5. Map으로 변환
        Map<Long, Record> recordMap = records.stream()
                .collect(Collectors.toMap(Record::getRecordPk, Function.identity()));

        //6. row 순서대로 응답 생성
        List<RecordResponse> result = new ArrayList<>();
        for (int i = 0; i < recordIds.size(); i++) {
            Record rec = recordMap.get(recordIds.get(i));
            if (rec != null) {
                result.add(RecordResponse.fromWithRank(rec, i + 1)); // rank는 1부터
            }
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

        // 5. 수정 전 정보 저장 (Redis 제거용)
        String beforeLevel = record.getLevel();
        String beforeNick = getNickname(record);
        Long beforeMemberListPk = getMemberListPk(record);

        // 6. targetMemberList (null 허용) 및 nickname 결정 (null => 변경 없음)
        MemberList targetMemberList = null;
        String nicknameToSet = null;


        if (req.getMemberListPk() != null) {
            // mmemberPk가 있으면 memberList에서 조회해서 member와 box-별 닉네임을 얻음
            MemberList memberList = memberListRepository
                    .findByMemberListPkAndBoxBoxPkAndDeletedYN(req.getMemberListPk(), boxPk, BaseTime.Yn.N)
                    .orElseThrow(() -> new ValidationException(ErrorCode.MEMBER_NOT_IN_BOX));

            //자기 자신 제외(recordPk) 중복 운동기록 있는지 확인
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

        //엔티티 수정 & 동시성 대비
        try {
            req.applyTo(record, targetMemberList, nicknameToSet);
            recordRepository.flush();
        } catch (DataIntegrityViolationException ex) {
            // DB 유니크 제약 위반 등 동시성 문제로 인해 발생할 수 있음
            throw new ValidationException(ErrorCode.ALREADY_EXISTS_RECORD_FOR_CLASS);
        }

        // 8. 트랜잭션 커밋 후 Redis 동기화
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                // 이전 항목 제거
                rankingService.remove(boxPk, date, beforeLevel, beforeNick, recordPk, beforeMemberListPk);
                // 새 항목 추가
                rankingService.upsert(record);
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

        // 삭제 전 정보 저장 (Redis 제거용)
        String level = record.getLevel();
        String nickname = getNickname(record);
        Long memberListPk = getMemberListPk(record);

        //delete
        record.markDeleted();
        recordRepository.flush();

        // 6. 트랜잭션 커밋 후 Redis 제거
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                rankingService.remove(boxPk, date, level, nickname, recordPk, memberListPk);
            }
        });
    }

    @Transactional
    public int deleteAllRecords(Long memberPk, Long boxPk, Long wodPk){
        //1. 해당 box에 등록된 매니저, 오너만 pass 되도록 유효성 검증(로그인한 유저)
        requireManagerOrOwner(memberPk, boxPk);

        //2. wod 유효성 검증 : box+date로 조회
        Wod wod = wodRespository.findByWodPkAndBoxBoxPkAndDeletedYN(wodPk, boxPk, BaseTime.Yn.N)
                .orElseThrow(() -> new ValidationException(ErrorCode.WOD_NOT_FOUND));

        return recordRepository.deleteByWodWodPkAndDeletedYN(wodPk, BaseTime.Yn.N);

    }

    /**
     * DB 폴백 (Redis 미스 시)
     */
    private List<Long> getRankingFromDb(Long boxPk, LocalDate date, WodType type) {
        //운동 타입별 정렬
        List<Record> records = switch (type) {
            case ForTime -> recordRepository.findForTimeOrder(boxPk, date);
            case AMRAP -> recordRepository.findAmrapOrder(boxPk, date);
            case EMOM, SuccessFail -> recordRepository.findEmomOrSfOrder(boxPk, date);
            case EMOMMAX, MaxReps -> recordRepository.findMaxRepsOrder(boxPk, date);
            default -> List.of();
        };

        // Redis 재빌드
        rankingService.rebuild(boxPk, date, records);

        return records.stream().map(Record::getRecordPk).toList();
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

    private String getNickname(Record r) {
        return r.getNickname() != null ? r.getNickname()
                : (r.getMemberList() != null ? r.getMemberList().getBoxNickname() : "");
    }

    private Long getMemberListPk(Record r) {
        return r.getMemberList() != null ? r.getMemberList().getMemberListPk() : null;
    }

    @Transactional
    private void deleteTeamRecord(Long boxPk, LocalDate date , Record record){

        // 삭제 전 정보 저장 (Redis 제거용)
        String level = record.getLevel();
        String nickname = getNickname(record);
        Long memberListPk = getMemberListPk(record);

        //delete
        record.markDeleted();
        recordRepository.flush();

        // 6. 트랜잭션 커밋 후 Redis 제거
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                rankingService.remove(boxPk, date, level, nickname, record.getRecordPk(), memberListPk);
            }
        });
    }

}