package com.fitian.burntz.domain.classes.service;

import com.fitian.burntz.domain.box.entity.Box;
import com.fitian.burntz.domain.box.enums.MemberRole;
import com.fitian.burntz.domain.box.repository.BoxRepository;
import com.fitian.burntz.domain.classes.v1.dto.ClassesCreateRequest;
import com.fitian.burntz.domain.classes.v1.dto.ClassesIdentifierRequest;
import com.fitian.burntz.domain.classes.v1.dto.ClassesSearchRequest;
import com.fitian.burntz.domain.classes.entity.ClassParticipant;
import com.fitian.burntz.domain.classes.entity.Classes;
import com.fitian.burntz.domain.classes.repository.ClassParticipantRepository;
import com.fitian.burntz.domain.classes.repository.ClassesRepository;
import com.fitian.burntz.domain.member.entity.Member;
import com.fitian.burntz.domain.member.entity.MemberList;
import com.fitian.burntz.domain.member.repository.MemberListRepository;
import com.fitian.burntz.domain.member.repository.MemberRepository;
import com.fitian.burntz.global.common.entity.BaseTime;
import com.fitian.burntz.global.exception.ErrorCode;
import com.fitian.burntz.global.exception.ValidationException;
import com.fitian.burntz.global.security.core.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * @author : 김관중
 * @packageName : com.fitian.burntz.domain.classes.service
 * @fileName : ClassesService
 * @date : 2025-09-15
 * @description : 수업 서비스입니다.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class ClassesService {

    private final BoxRepository boxRepository;
    private final MemberRepository memberRepository;
    private final MemberListRepository memberListRepository;
    private final ClassesRepository classesRepository;
    private final ClassParticipantRepository participantRepository;

    public List<Classes> getClasses(ClassesSearchRequest request, CustomUserDetails userDetails) {

        //존재하는 회원인지 검증
        boolean exist = memberListRepository.existsByBoxBoxPkAndMemberMemberPkAndDeletedYN(request.getBoxPK(), userDetails.getMemberPk(), BaseTime.Yn.N);
        if(!exist) throw new ValidationException(ErrorCode.ACCESS_DENIED);

        return classesRepository.findByBoxBoxPkAndClassDateBetweenAndDeletedYN(request.getBoxPK(), request.getStartDate(), request.getEndDate(), BaseTime.Yn.N);
    }

    public void createClasses(List<ClassesCreateRequest> requestList, CustomUserDetails userDetails) {

        //회원 등급 검증
        MemberList memberList = memberListRepository.findRoleByMemberMemberPkAndBoxBoxPkAndDeletedYN(userDetails.getMemberPk(), requestList.get(0).getBoxPK(), BaseTime.Yn.N)
               .orElseThrow(() -> new ValidationException(ErrorCode.USER_NOT_FOUND));
       if(memberList.getRole() == MemberRole.GUEST || memberList.getRole() == MemberRole.MEMBER) throw new ValidationException(ErrorCode.ACCESS_DENIED);

       Box box = boxRepository.findById(requestList.get(0).getBoxPK())
               .orElseThrow(() -> new ValidationException(ErrorCode.BOX_NOT_FOUND));

       List<Classes> classesList = new ArrayList<>();

        for (ClassesCreateRequest classesCreateRequest : requestList) {
            Classes classes = Classes.builder()
                    .box(box)
                    .classTitle(classesCreateRequest.getClassTitle())
                    .classDate(classesCreateRequest.getClassDate())
                    .classMemo(classesCreateRequest.getClassMemo())
                    .startTime(classesCreateRequest.getStartTime())
                    .endTime(classesCreateRequest.getEndTime())
                    .classMemberCapacity(classesCreateRequest.getClassMemberCapacity())
                    .build();
            classesList.add(classes);
        }

        classesRepository.saveAll(classesList);
    }

    public void joinClass(ClassesIdentifierRequest request, CustomUserDetails userDetails) {
        //해당 박스에 존재하는 회원인지 검증
        boolean memberExist = memberListRepository.existsByBoxBoxPkAndMemberMemberPkAndDeletedYN(request.getBoxPK(), userDetails.getMemberPk(), BaseTime.Yn.N);
        if(!memberExist) throw new ValidationException(ErrorCode.ACCESS_DENIED);
        //해당 수업에 참여중인지 검증
        boolean isInClass = participantRepository.existsByClassesClassesPkAndMemberMemberPkAndDeletedYN(request.getClassesPK(), userDetails.getMemberPk(), BaseTime.Yn.N);
        if(isInClass) throw new ValidationException(ErrorCode.DUPLICATED_USER);

        Classes classes = classesRepository.findById(request.getClassesPK())
                .orElseThrow(() -> new ValidationException(ErrorCode.CLASS_NOT_FOUND));
        Member member = memberRepository.findById(userDetails.getMemberPk())
                .orElseThrow(() -> new ValidationException(ErrorCode.USER_NOT_FOUND));

        ClassParticipant cp = ClassParticipant.builder()
                .classes(classes)
                .member(member)
                .build();

        participantRepository.save(cp);
    }

    public void cancelClass(ClassesIdentifierRequest request, CustomUserDetails userDetails) {
        //해당 박스에 존재하는 회원인지 검증
        boolean memberExist = memberListRepository.existsByBoxBoxPkAndMemberMemberPkAndDeletedYN(request.getBoxPK(), userDetails.getMemberPk(), BaseTime.Yn.N);
        if(!memberExist) throw new ValidationException(ErrorCode.ACCESS_DENIED);
        //해당 수업에 참여중인지 검증
        ClassParticipant participant = participantRepository.findByClassesClassesPkAndMemberMemberPkAndDeletedYN(request.getClassesPK(), userDetails.getMemberPk(), BaseTime.Yn.N)
                .orElseThrow(() -> new ValidationException(ErrorCode.USER_NOT_FOUND));

        participant.markDeleted();
    }

    public List<ClassParticipant> getMembersByClassPk(ClassesIdentifierRequest request, CustomUserDetails userDetails) {
        List<ClassParticipant> list = new ArrayList<>();
        return list;
    }
}
