package com.fitian.burntz.domain.article.v1.controller;

import com.fitian.burntz.domain.article.v1.dto.ArticleCreateRequest;
import com.fitian.burntz.domain.article.service.ArticleService;
import com.fitian.burntz.global.common.response.ApiResponse;
import com.fitian.burntz.global.security.core.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * @author : 김관중
 * @packageName : com.fitian.burntz.domain.article.controller
 * @fileName : ArticleController
 * @date : 2025-09-12
 * @description : 게시글 컨트롤러입니다.
 */

@RestController
@RequestMapping("/api/v1/article")
@RequiredArgsConstructor
public class ArticleController {

    private final ArticleService articleService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<Void> createArticle(
            @Valid @RequestPart ArticleCreateRequest request,
            @RequestPart(value = "ArticleImages", required = false) MultipartFile[] images,
            @AuthenticationPrincipal CustomUserDetails userDetails){

        return ApiResponse.success(null, "게시글 생성 완료.");
    }

}
