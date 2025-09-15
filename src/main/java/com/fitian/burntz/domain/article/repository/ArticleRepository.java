package com.fitian.burntz.domain.article.repository;

import com.fitian.burntz.domain.article.entity.Article;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * @author : 김관중
 * @packageName : com.fitian.burntz.domain.article.repository
 * @fileName : ArticleRepository
 * @date : 2025-09-12
 * @description : 게시글 리포지토리입니다.
 */
public interface ArticleRepository extends JpaRepository<Article, Long> {

}
