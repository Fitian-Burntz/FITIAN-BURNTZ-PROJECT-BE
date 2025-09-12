package com.fitian.burntz.domain.article.v1.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @author : 김관중
 * @packageName : com.fitian.burntz.domain.article.dto
 * @fileName : ArticleCreateRequest
 * @date : 2025-09-12
 * @description : 게시글 생성 DTO 입니다
 */

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArticleCreateRequest {

    @NotBlank(message = "memberPk must not be blank")
    private long memberPk;

    @NotBlank(message = "boxPk must not be blank")
    private long boxPk;

    @NotBlank(message = "articleTitle must not be blank")
    private String articleTitle;

    @NotBlank(message = "articleScript must not be blank")
    private String articleScript;

    @NotBlank(message = "category must not be blank")
    private String category;

    private List<String> articleImgUrls;

}
