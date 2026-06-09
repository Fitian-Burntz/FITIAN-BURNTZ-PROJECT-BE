package com.fitian.burntz.domain.box.event;

import com.fitian.burntz.domain.box.enums.ActivityType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class BoxActivityEvent {
    private final Long boxPk;
    private final ActivityType type;
    private final Long actorPk;
    private final String actorName;
    private final Long targetMemberPk;
    private final String targetMemberName;
    private final String detail;

    public static BoxActivityEvent of(Long boxPk, ActivityType type, Long actorPk, String actorName, String detail) {
        return new BoxActivityEvent(boxPk, type, actorPk, actorName, null, null, detail);
    }

    public static BoxActivityEvent withTarget(Long boxPk, ActivityType type, Long actorPk, String actorName,
                                              Long targetMemberPk, String targetMemberName, String detail) {
        return new BoxActivityEvent(boxPk, type, actorPk, actorName, targetMemberPk, targetMemberName, detail);
    }
}
