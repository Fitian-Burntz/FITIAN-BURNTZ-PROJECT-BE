package com.fitian.burntz.domain.wod.v1.dto;

import com.fitian.burntz.domain.wod.entity.Wod;
import com.fitian.burntz.domain.wod.enums.WodType;
import com.fitian.burntz.global.common.entity.BaseTime;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * @author : 선순주
 * @packageName : com.fitian.burntz.domain.wod.v1.dto
 * @fileName : WodResponse
 * @date : 2025-09-16
 * @description : Wod 조회 DTO
 */

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(name = "WodResponse", description = "Wod 조회 응답")
public class WodResponse {
    @Schema(description = "wod Pk", example = "3")
    private Long wodPk;
    @Schema(description = "box Pk", example = "2")
    private Long boxPk;

    @Schema(description = "wod 제목", example="HSPU+HPCL")
    private String wodTitle;

    @Schema(description = "wod 내용", example="Strength Every 2:00 x 6 set")
    private String wodScript;

    @Schema(description = "wod 종류", example="AMRAP")
    private WodType wodType;

    @Schema(description = "wod 날짜", example="2025-10-01")
    private LocalDate wodDate;

    public static WodResponse from(Wod w) {
        if (w == null) return null;
        LocalDateTime createdAt = w.getCreatedAt();

        return WodResponse.builder()
                .wodPk(w.getWodPk())
                .boxPk(w.getBox() != null ? w.getBox().getBoxPk() : null)
                .wodTitle(w.getWodTitle())
                .wodScript(w.getWodScript())
                .wodType(w.getWodType())
                .wodDate(w.getWodDate())
                .build();
    }
}