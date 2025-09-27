package com.fitian.burntz.domain.box.dto;


import com.fitian.burntz.domain.box.entity.Box;
import lombok.*;

import static com.fitian.burntz.global.common.util.StringUtil.trimToNull;


@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UpdateBoxInfoDto {

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

    public static UpdateBoxInfoDto from(UpdateBoxInfoRequest updateBoxInfoRequest){
        return UpdateBoxInfoDto.builder()
                .boxPk(updateBoxInfoRequest.getBoxPk())
                .boxName(trimToNull(updateBoxInfoRequest.getBoxName()))
                .boxCode(trimToNull(updateBoxInfoRequest.getBoxCode()))
                .boxContact(trimToNull(updateBoxInfoRequest.getBoxContact()))
                .boxAddress(trimToNull(updateBoxInfoRequest.getBoxAddress()))
                .boxScript(trimToNull(updateBoxInfoRequest.getBoxScript()))
                .placeId(trimToNull(updateBoxInfoRequest.getPlaceId()))
                .boxFeeUrl(trimToNull(updateBoxInfoRequest.getBoxFeeUrl()))
                .boxTimetableUrl(trimToNull(updateBoxInfoRequest.getBoxTimetableUrl()))
                .boxInsta(trimToNull(updateBoxInfoRequest.getBoxInsta()))
                .build();
    }

    public static UpdateBoxInfoDto entityToDto(Box box){
        return UpdateBoxInfoDto.builder()
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
                .build();
    }

}
