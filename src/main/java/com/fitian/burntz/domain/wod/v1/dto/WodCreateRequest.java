package com.fitian.burntz.domain.wod.v1.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fitian.burntz.domain.box.entity.Box;
import com.fitian.burntz.domain.wod.entity.Wod;
import com.fitian.burntz.domain.wod.enums.WodType;
import com.fitian.burntz.global.common.entity.BaseTime;
import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "Wod Create DTO")
public class WodCreateRequest{
    @NotNull
    @Schema(description = "wod 제목")
    private String wodTitle;
    @NotNull
    @Schema(description = "wod 내용")
    private String wodScript;
    @NotNull
    @Schema(description = "wod 종류")
    private WodType wodType;
    @NotNull
    @Schema(description = "wod 날짜")
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