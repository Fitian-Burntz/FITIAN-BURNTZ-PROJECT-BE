package com.fitian.burntz.domain.wod.v1.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fitian.burntz.domain.wod.entity.Wod;
import com.fitian.burntz.domain.wod.enums.WodType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;

/**
 * @author : 선순주
 * @packageName : com.fitian.burntz.domain.wod.v1.dto
 * @fileName : WodUpdateRequest
 * @date : 2025-09-16
 * @description : Wod 수정용 DTO
 */
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class WodUpdateRequest {
    @NotNull
    @Schema(description = "wod 제목")
    private String wodTitle;

    @NotNull
    @Schema(description = "wod 내용")
    private String wodScript;

    @NotNull
    @Schema(description = "wod 종류")
    private WodType wodType;

    //DTO -> Entity 적용(DTO가 엔티티의 도메인 메서드 호출)
    public void applyTo(Wod wod) {
        wod.update(this.wodTitle, this.wodScript, this.wodType);
    }

}