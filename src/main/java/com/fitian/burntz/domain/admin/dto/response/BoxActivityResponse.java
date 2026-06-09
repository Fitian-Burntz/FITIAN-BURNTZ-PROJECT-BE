package com.fitian.burntz.domain.admin.dto.response;

import com.fitian.burntz.domain.box.entity.BoxActivity;
import com.fitian.burntz.domain.box.enums.ActivityType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class BoxActivityResponse {
    private Long activityPk;
    private ActivityType type;
    private Long actorPk;
    private String actorName;
    private Long targetMemberPk;
    private String targetMemberName;
    private String detail;
    private LocalDateTime createdAt;

    public static BoxActivityResponse from(BoxActivity activity) {
        return BoxActivityResponse.builder()
                .activityPk(activity.getActivityPk())
                .type(activity.getType())
                .actorPk(activity.getActorPk())
                .actorName(activity.getActorName())
                .targetMemberPk(activity.getTargetMemberPk())
                .targetMemberName(activity.getTargetMemberName())
                .detail(activity.getDetail())
                .createdAt(activity.getCreatedAt())
                .build();
    }
}
