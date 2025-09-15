package com.fitian.burntz.domain.classes.service;

import com.fitian.burntz.domain.box.entity.Box;
import com.fitian.burntz.domain.box.enums.MemberRole;
import com.fitian.burntz.domain.box.repository.BoxRepository;
import com.fitian.burntz.domain.classes.dto.ClassesCreateRequest;
import com.fitian.burntz.domain.classes.dto.ClassesSearchRequest;
import com.fitian.burntz.domain.classes.entity.Classes;
import com.fitian.burntz.domain.classes.repository.ClassesRepository;
import com.fitian.burntz.domain.member.repository.MemberListRepository;
import com.fitian.burntz.global.common.entity.BaseTime;
import com.fitian.burntz.global.exception.ErrorCode;
import com.fitian.burntz.global.exception.ValidationException;
import com.fitian.burntz.global.security.core.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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
public class ClassesService {

    private final BoxRepository boxRepository;
    private final MemberListRepository memberListRepository;
    private final ClassesRepository classesRepository;

    public List<Classes> getClasses(ClassesSearchRequest request, CustomUserDetails userDetails) {

        boolean exist = memberListRepository.existsByBoxBoxPkAndMemberPkAndDeletedYN(request.getBoxPK(), userDetails.getMemberPk(), BaseTime.Yn.N);

        if(!exist) throw new ValidationException(ErrorCode.ACCESS_DENIED);

        return classesRepository.findByBoxBoxPkAndClassDateBetweenAndDeletedYN(request.getBoxPK(), request.getStartDate(), request.getEndDate(), BaseTime.Yn.N);
    }

    public void createClasses(List<ClassesCreateRequest> requestList, CustomUserDetails userDetails) {

        //회원 등급 검증
        MemberRole role = memberListRepository.findRoleByMemberMemberPkAndBoxBoxPkAndDeletedYN(userDetails.getMemberPk(), requestList.get(0).getBoxPK(), BaseTime.Yn.N)
               .orElseThrow(() -> new ValidationException(ErrorCode.USER_NOT_FOUND));
       if(role == MemberRole.GUEST || role == MemberRole.MEMBER) throw new ValidationException(ErrorCode.ACCESS_DENIED);

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
}
