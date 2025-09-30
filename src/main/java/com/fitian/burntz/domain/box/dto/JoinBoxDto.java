package com.fitian.burntz.domain.box.dto;

import com.fitian.burntz.domain.box.enums.MemberRole;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "사용자 box 가입 시 DTO")
public class JoinBoxDto {

    private Long memberPk;
    private String boxCode;
    private MemberRole memberRole;


    public static JoinBoxDto from(Long joinMemberPk, String belongBoxPk) {
        return JoinBoxDto.builder()
                .memberPk(joinMemberPk)
                .boxCode(belongBoxPk)
                .memberRole(MemberRole.GUEST)
                .build();
    }
}
