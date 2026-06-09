package com.fitian.burntz.domain.admin.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
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

    @Getter
    @Builder
    public static class WodInfo {
        private Long wodPk;
        private String wodTitle;
        private String wodType;
        private LocalDate wodDate;
        private String wodScript;
    }

    @Getter
    @Builder
    public static class ClassInfo {
        private Long classesPk;
        private String classTitle;
        private LocalDate classDate;
        private String startTime;
        private String endTime;
        private Integer capacity;
        private Long participantCount;
        private String classMemo;
    }

    @Getter
    @Builder
    public static class RecordInfo {
        private Long recordPk;
        private String nickname;
        private String level;
        private Integer round;
        private Integer reps;
        private Float time;
        private String result;
        private String memo;
        private LocalDate wodDate;
        private String wodTitle;
        private String wodType;
    }

    @Getter
    @Builder
    public static class WodDayInfo {
        private WodInfo wod;
        private List<RecordInfo> records;
    }

    @Getter
    @Builder
    public static class MembershipWithHistoryInfo {
        private Long membershipPk;
        private String membershipName;
        private String status;
        private LocalDate startDate;
        private LocalDate expirationDate;
        private String memo;
        private List<HistoryEntry> histories;
    }

    @Getter
    @Builder
    public static class HistoryEntry {
        private Long historyPk;
        private String actionType;
        private String preValue;
        private String newValue;
        private String memo;
        private Integer period;
        private String createdByNickname;
        private LocalDateTime createdAt;
    }

    @Getter
    @Builder
    public static class ClassParticipantInfo {
        private Long memberPk;
        private String boxNickname;
        private String role;
    }

    @Getter
    @Builder
    public static class ChannelInfo {
        private Long channelPk;
        private String channelName;
        private String channelEmoji;
        private String channelType;
        private int participantCount;
    }
}
