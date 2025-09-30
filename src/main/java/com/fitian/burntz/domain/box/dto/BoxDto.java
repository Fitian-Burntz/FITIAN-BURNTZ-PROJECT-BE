package com.fitian.burntz.domain.box.dto;

import com.fitian.burntz.domain.box.entity.Box;
import com.fitian.burntz.domain.box.enums.SubscribeYN;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.util.Objects;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Schema(description = "box 조회 시 box 정보 교환 DTO")
public class BoxDto {

    private Long boxPk;
    private String boxName;
    private String boxCode;
    private String boxContact;
    private String boxAddress;
    private String boxScript;
    private String placeId;
    private String boxFeeUrl;
    private String boxTimetableUrl;
    private String boxInsta;
    private SubscribeYN subscribe; // Y/N

    public static BoxDto from(Box box) {
        Objects.requireNonNull(box, "box required.");

        return BoxDto.builder()
                .boxPk(box.getBoxPk())
                .boxName(box.getBoxName())
                .boxCode(box.getBoxCode())
                .boxContact(box.getBoxContact())
                .boxAddress(box.getBoxAddress())
                .boxScript(box.getBoxScript())
                .placeId(box.getPlaceId())
                .boxFeeUrl(box.getBoxFeeUrl())
                .boxTimetableUrl(box.getBoxTimetableUrl())
                .boxInsta(box.getBoxInsta())
                .subscribe(box.getSubscribe())
                .build();
    }
}
