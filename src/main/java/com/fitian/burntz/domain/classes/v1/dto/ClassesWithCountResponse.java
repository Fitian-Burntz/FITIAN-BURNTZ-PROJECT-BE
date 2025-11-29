package com.fitian.burntz.domain.classes.v1.dto;

import com.fitian.burntz.domain.classes.entity.Classes;

/**
 * @author : 김관중
 * @packageName : com.fitian.burntz.domain.classes.v1.dto
 * @fileName : ClassesWithCountResponse
 * @date : 2025-11-27
 * @description : 클래스 참여자 수와 함께 클래스 리스트를 반환하는 DTO 입니다
 */
public class ClassesWithCountResponse {

    private final Classes classes;
    private final Long participantCount;

    public ClassesWithCountResponse(Classes classes, Long participantCount){
        this.classes = classes;
        this.participantCount = participantCount;
    }

    public Classes getClasses() { return classes;}
    public Long getParticipantCount() { return participantCount;}
}
