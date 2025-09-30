package com.fitian.burntz.domain.box.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Schema(description = "box 정보 수정 시 요청폼")
public class UpdateBoxInfoRequest {

    @NotNull(message = "boxPk is required")
    private Long boxPk;

    @NotNull(message = "boxName is required")
    private String boxName;

    @NotNull(message = "boxCode is required")
    private String boxCode;

    private String boxContact;
    private String boxAddress;
    private String boxScript;
    private String placeId;
    private String boxFeeUrl;
    private String boxTimetableUrl;
    private String boxInsta;

}
