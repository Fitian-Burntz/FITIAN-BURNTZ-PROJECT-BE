package com.fitian.burntz.domain.classes.service;

import com.fitian.burntz.domain.box.entity.Box;
import com.fitian.burntz.domain.box.enums.ActivityType;
import com.fitian.burntz.domain.box.enums.MemberRole;
import com.fitian.burntz.domain.box.event.BoxActivityEvent;
import com.fitian.burntz.domain.box.repository.BoxRepository;
import com.fitian.burntz.domain.classes.v1.dto.*;
import com.fitian.burntz.domain.classes.entity.ClassParticipant;
import com.fitian.burntz.domain.classes.entity.Classes;
import com.fitian.burntz.domain.classes.repository.ClassParticipantRepository;
import com.fitian.burntz.domain.classes.repository.ClassesRepository;
import com.fitian.burntz.domain.member.entity.MemberList;
import com.fitian.burntz.domain.member.repository.MemberListRepository;
import com.fitian.burntz.domain.membership.enums.MembershipStatus;
import com.fitian.burntz.domain.membership.repository.MembershipHoldRepository;
import com.fitian.burntz.domain.membership.repository.MembershipRepository;
import com.fitian.burntz.domain.record.entity.Record;
import com.fitian.burntz.domain.record.repository.RecordRepository;
import com.fitian.burntz.global.common.entity.BaseTime;
import com.fitian.burntz.global.exception.ErrorCode;
import com.fitian.burntz.global.exception.ValidationException;
import com.fitian.burntz.global.security.core.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

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
    private final RecordRepository recordRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final MembershipRepository membershipRepository;
    private final MembershipHoldRepository membershipHoldRepository;

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

        MemberList memberList = memberListRepository.findRoleByMemberMemberPkAndBoxBoxPkAndDeletedYN(userDetails.getMemberPk(), request.getBoxPk(), BaseTime.Yn.N)
                .orElseThrow(() -> new ValidationException(ErrorCode.USER_NOT_FOUND));

        List<ClassesWithCountResponse> list = classesRepository.findWithParticipantCountByBoxAndDate(request.getBoxPk(), request.getStartDate(), request.getEndDate(), BaseTime.Yn.N, BaseTime.Yn.N);

        List<Long> classesPks = list.stream()
                .map(r -> r.getClasses().getClassesPk())
                .toList();

        List<Long> ClassInPks = participantRepository.findClassesPkByClassesPkInAndMemberListMemberListPkAndDeletedYN(classesPks, memberList.getMemberListPk(), BaseTime.Yn.N);

        List<ClassesResponse> responseList;
        responseList = list.stream()
                .map(r -> {
                    Classes c = r.getClasses();
                    boolean participated = c.getClassesPk() != null && ClassInPks.contains(c.getClassesPk());
                    return new ClassesResponse(
                            c.getClassesPk(),
                            c.getClassDate(),
                            c.getStartTime(),
                            c.getEndTime(),
                            c.getClassMemberCapacity(),
                            r.getParticipantCount(),
                            c.getClassTitle(),
                            c.getClassMemo(),
                            participated ? BaseTime.Yn.Y : BaseTime.Yn.N
                    );
                }).toList();

        return responseList;
    }

    public List<ClassesWithParticipant> getClassesWithRecords(ClassesWithRecordsSearchRequest request, CustomUserDetails userDetails) {

        List<Classes> classesList = classesRepository.findByBoxBoxPkAndClassDateAndDeletedYN(request.getBoxPk(), request.getDate(), BaseTime.Yn.N);

        List<ClassesWithParticipant> result = new ArrayList<>();

        for(Classes c : classesList) {

            List<ClassParticipant> pList = participantRepository.findByClassesClassesPkAndDeletedYN(c.getClassesPk(), BaseTime.Yn.N);

            List<Record> rList = recordRepository.findByClassesClassesPkAndDeletedYN(c.getClassesPk(), BaseTime.Yn.N);

            // rList -> memberListPkSet (null 제거)
            Set<Long> memberListPkSet = Optional.ofNullable(rList).orElseGet(Collections::emptyList).stream()
                    .map(rec -> Optional.ofNullable(rec)
                            .map(Record::getMemberList)
                            .map(MemberList::getMemberListPk)
                            .orElse(null))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());

            // pList -> prList (null-safe)
            List<ParticipantWithRecord> prList = Optional.ofNullable(pList).orElseGet(Collections::emptyList).stream()
                    .filter(Objects::nonNull) // p가 null이면 제거
                    .map(p -> {
                        // memberList 관련 안전 추출 (한 번만 접근)
                        MemberList ml = p.getMemberList(); // ml may be null
                        Long memberListPk = ml != null ? ml.getMemberListPk() : null;
                        boolean recordExists = memberListPk != null && memberListPkSet.contains(memberListPk);

                        // classes 안전 추출
                        Long classesPk = Optional.ofNullable(p.getClasses()).map(Classes::getClassesPk).orElse(null);
                        String boxNickname = ml != null ? ml.getBoxNickname() : null;

                        return ParticipantWithRecord.builder()
                                .classParticipantPk(p.getClassParticipantPk()) // p is non-null here
                                .classesPk(classesPk)
                                .memberListPk(memberListPk)
                                .boxNickname(boxNickname)
                                .recordExists(recordExists)
                                .build();
                    }).toList();

            ClassesWithParticipant cp = ClassesWithParticipant.builder()
                    .classesPk(c.getClassesPk())
                    .classDate(c.getClassDate())
                    .startTime(c.getStartTime())
                    .participantWithRecordList(prList)
                    .build();

            result.add(cp);
        }

        return result;
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

        eventPublisher.publishEvent(BoxActivityEvent.of(
                box.getBoxPk(), ActivityType.CLASS_CREATED,
                userDetails.getMemberPk(), list.getBoxNickname(),
                requestList.get(0).getClassDate().toString()
        ));
    }

    public void joinClass(ClassesIdentifierRequest request, CustomUserDetails userDetails) {
        //해당 박스에 존재하는 회원인지 검증
        MemberList memberList = memberListRepository.findRoleByMemberMemberPkAndBoxBoxPkAndDeletedYN(userDetails.getMemberPk(), request.getBoxPk(), BaseTime.Yn.N)
                .orElseThrow(() -> new ValidationException(ErrorCode.USER_NOT_FOUND));
        if (memberList.getRole() == MemberRole.GUEST) throw new ValidationException(ErrorCode.ACCESS_DENIED);

        // MANAGER/OWNER는 홀딩 상태와 무관하게 수업 신청 가능
        Classes targetClass = classesRepository.findById(request.getClassesPk())
                .orElseThrow(() -> new ValidationException(ErrorCode.CLASS_NOT_FOUND));

        boolean isManagerOrOwner = memberList.getRole() == MemberRole.MANAGER || memberList.getRole() == MemberRole.OWNER;
        if (!isManagerOrOwner) {
            membershipRepository.findAllMembershipByBoxPkAndMemberPk(request.getBoxPk(), userDetails.getMemberPk())
                    .stream()
                    .filter(m -> m.getStatus() == MembershipStatus.ACTIVE || m.getStatus() == MembershipStatus.HOLDING)
                    .findFirst()
                    .ifPresent(membership -> {
                        if (membership.getStatus() == MembershipStatus.HOLDING) {
                            throw new ValidationException(ErrorCode.MEMBERSHIP_HOLDING);
                        }
                        membershipHoldRepository.findActiveOrScheduledOnDate(
                                membership.getMembershipPk(), targetClass.getClassDate())
                                .ifPresent(hold -> {
                                    throw new ValidationException(ErrorCode.MEMBERSHIP_HOLDING);
                                });
                    });
        }

        //해당 수업에 참여중인지 검증
        boolean isInClass = participantRepository.existsByClassesClassesPkAndMemberListMemberMemberPkAndDeletedYN(request.getClassesPk(), userDetails.getMemberPk(), BaseTime.Yn.N);
        if(isInClass) throw new ValidationException(ErrorCode.DUPLICATED_USER);

        Classes classes = targetClass;

        if (classes.getClassMemberCapacity() != null) {
            int currentCount = participantRepository.countByClassesClassesPkAndDeletedYN(request.getClassesPk(), BaseTime.Yn.N);
            if (currentCount >= classes.getClassMemberCapacity()) {
                throw new ValidationException(ErrorCode.CLASS_CAPACITY_EXCEEDED);
            }
        }

        ClassParticipant cp = ClassParticipant.builder()
                .classes(classes)
                .memberList(memberList)
                .build();

        participantRepository.save(cp);

        String classDetail = classes.getClassDate() + (classes.getStartTime() != null ? " " + classes.getStartTime() : "");
        eventPublisher.publishEvent(BoxActivityEvent.of(
                request.getBoxPk(), ActivityType.CLASS_JOINED,
                userDetails.getMemberPk(), memberList.getBoxNickname(),
                classDetail
        ));
    }

    public void cancelClass(ClassesIdentifierRequest request, CustomUserDetails userDetails) {
        //해당 박스에 존재하는 회원인지 검증
        MemberList memberList = memberListRepository.findRoleByMemberMemberPkAndBoxBoxPkAndDeletedYN(userDetails.getMemberPk(), request.getBoxPk(), BaseTime.Yn.N)
                .orElseThrow(() -> new ValidationException(ErrorCode.ACCESS_DENIED));
        //해당 수업에 참여중인지 검증
        ClassParticipant participant = participantRepository.findByClassesClassesPkAndMemberListMemberMemberPkAndDeletedYN(request.getClassesPk(), userDetails.getMemberPk(), BaseTime.Yn.N)
                .orElseThrow(() -> new ValidationException(ErrorCode.USER_NOT_FOUND));

        participant.markDeleted();

        Classes classes = participant.getClasses();
        String classDetail = classes.getClassDate() + (classes.getStartTime() != null ? " " + classes.getStartTime() : "");
        eventPublisher.publishEvent(BoxActivityEvent.of(
                request.getBoxPk(), ActivityType.CLASS_CANCELLED,
                userDetails.getMemberPk(), memberList.getBoxNickname(),
                classDetail
        ));
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
                    .profileImageUrl(cp.getMemberList().getProfileImageUrl())
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