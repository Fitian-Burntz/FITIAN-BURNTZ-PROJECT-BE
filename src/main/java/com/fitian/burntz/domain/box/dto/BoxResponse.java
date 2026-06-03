package com.fitian.burntz.domain.box.dto;

import com.fitian.burntz.domain.box.enums.BoxSubscribeStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Objects;

@Schema(description = "box 조회 후 box 정보 컨트롤러 반환용 response DTO")
public record BoxResponse(
        Long boxPk,
        String boxName,
        String boxCode,
        String boxContact,
        String boxAddress,
        String boxScript,
        String placeId,
        String boxFeeUrl,
        String boxTimetableUrl,
        String boxInsta,
        BoxSubscribeStatus subscribe
) {

    public static BoxResponse from(BoxDto boxDto) {
        Objects.requireNonNull(boxDto, "boxDto required.");

        return new BoxResponse(
                boxDto.getBoxPk(),
                boxDto.getBoxName(),
                boxDto.getBoxCode(),
                boxDto.getBoxContact(),
                boxDto.getBoxAddress(),
                boxDto.getBoxScript(),
                boxDto.getPlaceId(),
                boxDto.getBoxFeeUrl(),
                boxDto.getBoxTimetableUrl(),
                boxDto.getBoxInsta(),
                boxDto.getSubscribe()
        );
    }
}