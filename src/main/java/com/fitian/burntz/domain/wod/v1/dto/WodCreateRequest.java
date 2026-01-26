package com.fitian.burntz.domain.wod.v1.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fitian.burntz.domain.box.entity.Box;
import com.fitian.burntz.domain.wod.entity.Wod;
import com.fitian.burntz.domain.wod.enums.WodType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;
/**
 * @author : 선순주
 * @packageName : com.fitian.burntz.domain.wod.v1.dto
 * @fileName : WodCreateRequest
 * @date : 2025-09-16
 * @description : Wod 생성용 DTO
 */

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(name = "WodCreateRequest", description = "Wod 생성 요청")
public class WodCreateRequest{

    @Schema(description = "wod 제목", example="HSPU+HPCL")
    private String wodTitle;

    @NotBlank(message = "wodScript must not be blank")
    @Schema(description = "wod 내용", example="Strength Every 2:00 x 6 set")
    private String wodScript;

    @NotNull(message = "wodType is required")
    @Schema(description = "wod 종류", example="AMRAP")
    private WodType wodType;

    @NotNull(message = "wodDate is required")
    @Schema(description = "wod 날짜", example="2025-10-01")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate wodDate;

    public Wod toEntity(Box box) {
        return Wod.builder()
                .box(box)
                .wodTitle(this.wodTitle)
                .wodScript(this.wodScript)
                .wodType(this.wodType)
                .wodDate(this.wodDate)
                .build();
    }
}