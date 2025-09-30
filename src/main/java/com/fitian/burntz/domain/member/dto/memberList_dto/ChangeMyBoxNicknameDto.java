package com.fitian.burntz.domain.member.dto.memberList_dto;

import com.fitian.burntz.domain.member.entity.MemberList;
import lombok.*;

import java.time.LocalDateTime;

/**
 * @author : 김남이
 * @packageName : com.fitian.burntz.domain.member.dto.memberList_dto
 * @fileName : ChangeMyBoxNicknameDto
 * @date : 2025-09-30
 * @description : 사용자가 box 별 nickname 을 변경하려고 할 때 데이터를 전달하는 dto 입니다.
 */

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ChangeMyBoxNicknameDto {
    private Long memberListPk;
    private Long boxPk;
    private String boxNickname;
    private LocalDateTime updatedAt;

    public static ChangeMyBoxNicknameDto from(MemberList memberList, Long boxPk){
        return ChangeMyBoxNicknameDto.builder()
                .memberListPk(memberList.getMemberListPk())
                .boxPk(boxPk)
                .boxNickname(memberList.getBoxNickname())
                .updatedAt(memberList.getUpdatedAt())
                .build();
    }
}
