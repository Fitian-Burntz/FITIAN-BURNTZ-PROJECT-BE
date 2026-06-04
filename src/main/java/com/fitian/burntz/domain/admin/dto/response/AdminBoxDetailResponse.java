package com.fitian.burntz.domain.admin.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
public class AdminBoxDetailResponse {

    private Long boxPk;
    private String boxName;
    private String boxCode;
    private String boxAddress;
    private String boxContact;
    private String subscribeStatus;
    private OwnerInfo owner;
    private int memberCount;
    private List<MemberInfo> members;

    @Getter
    @Builder
    public static class OwnerInfo {
        private Long memberPk;
        private String nickname;
        private String email;
    }

    @Getter
    @Builder
    public static class MemberInfo {
        private Long memberPk;
        private String boxNickname;
        private String role;
        private String email;
        private String membershipName;
        private String membershipStatus;
        private LocalDate startDate;
        private LocalDate expirationDate;
    }
}
