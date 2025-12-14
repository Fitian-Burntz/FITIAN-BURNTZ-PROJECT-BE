package com.fitian.burntz.domain.classes.service;

import com.fitian.burntz.domain.box.entity.Box;
import com.fitian.burntz.domain.box.enums.MemberRole;
import com.fitian.burntz.domain.box.repository.BoxRepository;
import com.fitian.burntz.domain.classes.v1.dto.*;
import com.fitian.burntz.domain.classes.entity.ClassParticipant;
import com.fitian.burntz.domain.classes.entity.Classes;
import com.fitian.burntz.domain.classes.repository.ClassParticipantRepository;
import com.fitian.burntz.domain.classes.repository.ClassesRepository;
import com.fitian.burntz.domain.member.entity.MemberList;
import com.fitian.burntz.domain.member.repository.MemberListRepository;
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
    private final MemberListRepository memberListRepository;
    private final ClassesRepository classesRepository;
    private final ClassParticipantRepository participantRepository;

    //참여인원 카운트없이 가져오는 getClasses 현재 안씀.
    public List<ClassesResponse> getClasses(ClassesSearchRequest request, CustomUserDetails userDetails) {

        //존재하는 회원인지 검증
        boolean exist = memberListRepository.existsByBoxBoxPkAndMemberMemberPkAndDeletedYN(request.getBoxPk(), userDetails.getMemberPk(), BaseTime.Yn.N);
        if(!exist) throw new ValidationException(ErrorCode.ACCESS_DENIED);

        List<Classes> list = classesRepository.findByBoxBoxPkAndClassDateBetweenAndDeletedYN(request.getBoxPk(), request.getStartDate(), request.getEndDate(), BaseTime.Yn.N);
        List<ClassesResponse> responseList = new ArrayList<>();

        for(Classes c : list) {
            ClassesResponse response = ClassesResponse.builder()
                    .classesPk(c.getClassesPk())
                    .classDate(c.getClassDate())
                    .startTime(c.getStartTime())
                    .endTime(c.getEndTime())
                    .classMemberCapacity(c.getClassMemberCapacity())
                    .classTitle(c.getClassTitle())
                    .classMemo(c.getClassMemo())
                    .build();
            responseList.add(response);
        }
        return responseList;
    }

    public List<ClassesResponse> getClassesWithCount(ClassesSearchRequest request, CustomUserDetails userDetails) {
        //존재하는 회원인지 검증
        boolean exist = memberListRepository.existsByBoxBoxPkAndMemberMemberPkAndDeletedYN(request.getBoxPk(), userDetails.getMemberPk(), BaseTime.Yn.N);
        if(!exist) throw new ValidationException(ErrorCode.ACCESS_DENIED);

        List<ClassesWithCountResponse> list = classesRepository.findWithParticipantCountByBoxAndDate(request.getBoxPk(), request.getStartDate(), request.getEndDate(), BaseTime.Yn.N, BaseTime.Yn.N);
        List<ClassesResponse> responseList;
        responseList = list.stream()
                .map(r -> {
                    Classes c = r.getClasses();
                    return new ClassesResponse(
                            c.getClassesPk(),
                            c.getClassDate(),
                            c.getStartTime(),
                            c.getEndTime(),
                            c.getClassMemberCapacity(),
                            r.getParticipantCount(),
                            c.getClassTitle(),
                            c.getClassMemo()
                    );
                }).toList();

        return responseList;
    }

    public void createClasses(List<ClassesCreateRequest> requestList, CustomUserDetails userDetails) {

        //회원 등급 검증
        MemberList list = memberListRepository.findRoleByMemberMemberPkAndBoxBoxPkAndDeletedYN(userDetails.getMemberPk(), requestList.get(0).getBoxPk(), BaseTime.Yn.N)
                .orElseThrow(() -> new ValidationException(ErrorCode.USER_NOT_FOUND));
        if(list.getRole() == MemberRole.GUEST || list.getRole() == MemberRole.MEMBER) throw new ValidationException(ErrorCode.ACCESS_DENIED);

        Box box = boxRepository.findById(requestList.get(0).getBoxPk())
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
        MemberList memberList = memberListRepository.findRoleByMemberMemberPkAndBoxBoxPkAndDeletedYN(userDetails.getMemberPk(), request.getBoxPk(), BaseTime.Yn.N)
                .orElseThrow(() -> new ValidationException(ErrorCode.USER_NOT_FOUND));
        //해당 수업에 참여중인지 검증
        boolean isInClass = participantRepository.existsByClassesClassesPkAndMemberListMemberMemberPkAndDeletedYN(request.getClassesPk(), userDetails.getMemberPk(), BaseTime.Yn.N);
        if(isInClass) throw new ValidationException(ErrorCode.DUPLICATED_USER);

        Classes classes = classesRepository.findById(request.getClassesPk())
                .orElseThrow(() -> new ValidationException(ErrorCode.CLASS_NOT_FOUND));

        ClassParticipant cp = ClassParticipant.builder()
                .classes(classes)
                .memberList(memberList)
                .build();

        participantRepository.save(cp);
    }

    public void cancelClass(ClassesIdentifierRequest request, CustomUserDetails userDetails) {
        //해당 박스에 존재하는 회원인지 검증
        boolean memberExist = memberListRepository.existsByBoxBoxPkAndMemberMemberPkAndDeletedYN(request.getBoxPk(), userDetails.getMemberPk(), BaseTime.Yn.N);
        if(!memberExist) throw new ValidationException(ErrorCode.ACCESS_DENIED);
        //해당 수업에 참여중인지 검증
        ClassParticipant participant = participantRepository.findByClassesClassesPkAndMemberListMemberMemberPkAndDeletedYN(request.getClassesPk(), userDetails.getMemberPk(), BaseTime.Yn.N)
                .orElseThrow(() -> new ValidationException(ErrorCode.USER_NOT_FOUND));

        participant.markDeleted();
    }

    public List<ClassParticipantResponse> getMembersByClassPk(ClassesIdentifierRequest request, CustomUserDetails userDetails) {
        boolean exist = memberListRepository.existsByBoxBoxPkAndMemberMemberPkAndDeletedYN(request.getBoxPk(), userDetails.getMemberPk(), BaseTime.Yn.N);
        if(!exist) throw new ValidationException(ErrorCode.USER_NOT_FOUND);

        List<ClassParticipant> cpList = participantRepository.findByClassesClassesPkAndDeletedYN(request.getClassesPk(), BaseTime.Yn.N);
        List<ClassParticipantResponse> responseList = new ArrayList<>();

        for(ClassParticipant cp : cpList) {
            ClassParticipantResponse response = ClassParticipantResponse.builder()
                    .classParticipantPk(cp.getClassParticipantPk())
                    .classesPk(cp.getClasses().getClassesPk())
                    .memberPk(cp.getMemberList().getMember().getMemberPk())
                    .memberListPk(cp.getMemberList().getMemberListPk())
                    .boxNickname(cp.getMemberList().getBoxNickname())
                    .createdAt(cp.getCreatedAt())
                    .build();
            responseList.add(response);
        }
        return responseList;
    }

    public void updateClass(ClassesUpdateRequest request, CustomUserDetails userDetails) {
        //회원 등급 검증
        MemberList list = memberListRepository.findRoleByMemberMemberPkAndBoxBoxPkAndDeletedYN(userDetails.getMemberPk(), request.getBoxPk(), BaseTime.Yn.N)
                .orElseThrow(() -> new ValidationException(ErrorCode.USER_NOT_FOUND));
        if(list.getRole() == MemberRole.GUEST || list.getRole() == MemberRole.MEMBER) throw new ValidationException(ErrorCode.ACCESS_DENIED);


        Classes classes = classesRepository.findById(request.getClassesPk())
                .orElseThrow(() -> new ValidationException(ErrorCode.CLASS_NOT_FOUND));

        classes.updateFrom(request);
    }

    public void deleteClass(ClassesIdentifierRequest request, CustomUserDetails userDetails) {
        //회원 등급 검증
        MemberList list = memberListRepository.findRoleByMemberMemberPkAndBoxBoxPkAndDeletedYN(userDetails.getMemberPk(), request.getBoxPk(), BaseTime.Yn.N)
                .orElseThrow(() -> new ValidationException(ErrorCode.USER_NOT_FOUND));
        if(list.getRole() == MemberRole.GUEST || list.getRole() == MemberRole.MEMBER) throw new ValidationException(ErrorCode.ACCESS_DENIED);

        Classes classes = classesRepository.findById(request.getClassesPk())
                .orElseThrow(() -> new ValidationException(ErrorCode.CLASS_NOT_FOUND));

        classes.markDeleted();

        List<ClassParticipant> cpList = participantRepository.findByClassesClassesPkAndDeletedYN(request.getClassesPk(), BaseTime.Yn.N);
        for(ClassParticipant cp : cpList) {
            cp.markDeleted();
        }
    }

    public void deleteClassesByDate(ClassesDeleteRequest request, CustomUserDetails userDetails) {
        //회원 등급 검증
        MemberList list = memberListRepository.findRoleByMemberMemberPkAndBoxBoxPkAndDeletedYN(userDetails.getMemberPk(), request.getBoxPk(), BaseTime.Yn.N)
                .orElseThrow(() -> new ValidationException(ErrorCode.USER_NOT_FOUND));
        if (list.getRole() == MemberRole.GUEST || list.getRole() == MemberRole.MEMBER)
            throw new ValidationException(ErrorCode.ACCESS_DENIED);

        List<Classes> classesList = classesRepository.findByBoxBoxPkAndClassDateAndDeletedYN(request.getBoxPk(), request.getClassDate(), BaseTime.Yn.N);

        if (classesList.isEmpty()) {
            throw new ValidationException(ErrorCode.CLASS_NOT_FOUND);
        }

        classesList.forEach(Classes::markDeleted);

        List<Long> classesPks = classesList.stream()
                .map(Classes::getClassesPk).toList();

        List<ClassParticipant> participants = participantRepository.findByClassesClassesPkInAndDeletedYN(classesPks, BaseTime.Yn.N);

        participants.forEach(ClassParticipant::markDeleted);

        classesRepository.saveAll(classesList);
        participantRepository.saveAll(participants);
    }
}