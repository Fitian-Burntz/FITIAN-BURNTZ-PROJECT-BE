package com.fitian.burntz.domain.article.service;

import com.fitian.burntz.domain.article.repository.ArticleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author : 김관중
 * @packageName : com.fitian.burntz.domain.article.service
 * @fileName : ArticleService
 * @date : 2025-09-12
 * @description : 게시글 서비스 입니다
 */

@Service
@RequiredArgsConstructor
@Transactional
public class ArticleService {
    private final ArticleRepository articleRepository;


}
