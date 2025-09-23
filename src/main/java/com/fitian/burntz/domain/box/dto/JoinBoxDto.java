package com.fitian.burntz.domain.box.dto;

import com.fitian.burntz.domain.box.enums.MemberRole;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
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
