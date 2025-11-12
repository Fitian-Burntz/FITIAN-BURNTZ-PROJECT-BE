package com.fitian.burntz.domain.member.dto;

import com.fitian.burntz.domain.box.entity.Box;
import com.fitian.burntz.domain.box.enums.MemberRole;
import com.fitian.burntz.domain.member.entity.MemberList;
import com.fitian.burntz.domain.membership.v1.dto.MembershipDto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.util.Objects;

/**
 * @author : 김남이
 * @packageName : com.fitian.burntz.domain.member.dto
 * @fileName : BoxWithMembershipDto
 * @date : 2025-09-29
 * @description : 사용자가 내가 속한 box 정보를 조회할 때 데이터를 전달하는 dto 입니다.
 */

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "내 박스 정보 조회 시 멤버십과 결합한 데이터 DTO")
public class BoxWithMembershipDto {

    private Long memberListPk;
    private Long boxPk;
    private String boxName;
    private String boxNickname;
    private MemberRole role;

    // nullable: 멤버십이 없을 수 있음
    private MembershipDto membership;


    public static BoxWithMembershipDto from(MemberList memberList, Box targetBox, MembershipDto membershipDto) {
        Objects.requireNonNull(memberList, "memberList required.");
        Objects.requireNonNull(targetBox, "targetBox required.");

        return BoxWithMembershipDto.builder()
                .memberListPk(memberList.getMemberListPk())
                .boxPk(targetBox.getBoxPk())
                .boxName(targetBox.getBoxName())
                .boxNickname(memberList.getBoxNickname())
                .role(memberList.getRole())
                .membership(membershipDto)
                .build();
    }
}
