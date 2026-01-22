package com.fitian.burntz.domain.wod.service;

import com.fitian.burntz.domain.alarm.service.PushService;
import com.fitian.burntz.domain.alarm.v1.dto.PushDto;
import com.fitian.burntz.domain.box.entity.Box;
import com.fitian.burntz.domain.box.enums.MemberRole;
import com.fitian.burntz.domain.box.repository.BoxRepository;
import com.fitian.burntz.domain.member.entity.MemberList;
import com.fitian.burntz.domain.member.repository.MemberListRepository;
import com.fitian.burntz.domain.record.service.RecordService;
import com.fitian.burntz.domain.wod.entity.Wod;
import com.fitian.burntz.domain.wod.repository.WodRespository;
import com.fitian.burntz.domain.wod.v1.dto.WodCreateRequest;
import com.fitian.burntz.domain.wod.v1.dto.WodResponse;
import com.fitian.burntz.domain.wod.v1.dto.WodUpdateRequest;
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

/**
 * @author : 선순주
 * @packageName : com.fitian.burntz.domain.wod.service
 * @fileName : WodService
 * @date : 2025-09-16
 * @description : Wod Service
 */

@Slf4j
@Service
@RequiredArgsConstructor
public class WodService {

    private final PushService pushService;
    private final RecordService recordService;
    private final WodRespository wodRespository;
    private final BoxRepository boxRepository;
    private final MemberListRepository memberListRepository;

    /*
    * wod 생성
    * */
    @Transactional
    public void createWod(WodCreateRequest request, Long boxPk, Long memberPk){

        //1. 박스 유효성 검증
        Box box = requireActiveBox(boxPk);

        //2. 해당 box에 등록된 매니저, 오너만 pass되도록 유효성 검증
        requireManagerOrOwner(memberPk, boxPk);

        //3. 해당 날짜의 wod가 이미 생성되어 있는지 확인 -> 생성되어있으면 에러
        if(wodRespository.existsByBoxAndWodDateAndDeletedYN(box, request.getWodDate(), BaseTime.Yn.N)){
            throw new ValidationException(ErrorCode.WOD_ALREADY_EXISTS);
        }

        //DTO -> ENTITY
        Wod wod = request.toEntity(box);

        //동시성 대비 -> DB의 유니크 제약 조건에러(DataIntegrityViolationException)를 WOD_ALREADY_EXISTS로 변환함
        try {
            wodRespository.save(wod);


            // 해당 box 내 멤버에게 와드 등록 푸시 발송
            PushDto dto = PushDto.builder()
                    .title(box.getBoxName())
                    .body(request.getWodDate()+" 와드가 등록되었습니다.")
                    .build();
            pushService.notifyUsers(getAllMemberPkInBox(box), dto);

        } catch (DataIntegrityViolationException e) {
            throw new ValidationException(ErrorCode.WOD_ALREADY_EXISTS);
        }
    }

    /*
    * wod 조회(해당 날짜의 wod 조회)
    * */
    @Transactional(readOnly = true)
    public WodResponse getWod(Long boxPk, Long memberPk, LocalDate date){
        //1. 박스 유효성 검증
        Box box = requireActiveBox(boxPk);

        //2. 해당 멤버가 box에 추가되어 있는지 확인
        boolean memberExist = memberListRepository.existsByBoxBoxPkAndMemberMemberPkAndDeletedYN(boxPk, memberPk, BaseTime.Yn.N);
        if(!memberExist) throw new ValidationException(ErrorCode.ACCESS_DENIED);

        //해당 날짜의 wod가 없으면 코드 200 + null 값 반환
        Wod wod = wodRespository.findByBoxAndWodDateAndDeletedYN(box, date, BaseTime.Yn.N)
                .orElse(null);
        return WodResponse.from(wod);
    }

    /*
    * Wod 수정
    * */
    @Transactional
    public void updateWod(Long boxPk, Long memberPk, LocalDate date, WodUpdateRequest request){
        //1. 박스 유효성 검증
        Box box = requireActiveBox(boxPk);

        //2. 해당 box에 등록된 매니저, 오너만 pass되도록 유효성 검증
        requireManagerOrOwner(memberPk, boxPk);

        //3. Wod 유효성 검증
        Wod wod = requireActiveWod(box, date);

        //4. 기존 Wod와 연계된 Record 전체 삭제 deleteYN 처리
        recordService.deleteAllRecords(memberPk, boxPk, wod.getWodPk());

        //update
        request.applyTo(wod);
    }
    
    /*
    * Wod 삭제
    * */
    @Transactional
    public void deleteWod(Long boxPk, Long memberPk, LocalDate date){
        //1. 박스 유효성 검증
        Box box = requireActiveBox(boxPk);

        //2. 해당 box에 등록된 매니저, 오너만 pass되도록 유효성 검증
        requireManagerOrOwner(memberPk, boxPk);

        //3. Wod 유효성 검증
        Wod wod = requireActiveWod(box, date);

        //4. 기존 Wod와 연계된 Record 전체 삭제 deleteYN 처리
        recordService.deleteAllRecords(memberPk, boxPk, wod.getWodPk());

        //delete
        wod.markDeleted();
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

    private List<Long> getAllMemberPkInBox(Box box) {
        List<MemberList> memberLists = memberListRepository.findAllByBoxAndDeletedYN(box, BaseTime.Yn.N);

        return memberLists.stream()
                .map(ml -> ml.getMember().getMemberPk())
                .toList();
    }
}