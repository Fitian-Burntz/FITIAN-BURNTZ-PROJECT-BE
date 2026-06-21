package com.fitian.burntz.domain.admin.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class AdminGrowthResponse {

    private List<NewBoxInfo> recentBoxes;
    private List<NewMemberJoinInfo> recentMemberJoins;

    @Getter
    @Builder
    public static class NewBoxInfo {
        private Long boxPk;
        private String boxName;
        private String boxCode;
        private String boxAddress;
        private String subscribeStatus;
        private LocalDateTime createdAt;
    }

    @Getter
    @Builder
    public static class NewMemberJoinInfo {
        private Long memberListPk;
        private Long boxPk;
        private String boxName;
        private Long memberPk;
        private String nickname;
        private String email;
        private String role;
        private LocalDateTime joinedAt;
    }
}
