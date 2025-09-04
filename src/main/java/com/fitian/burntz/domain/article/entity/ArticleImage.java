package com.fitian.burntz.domain.article.entity;

import com.fitian.burntz.global.common.entity.BaseTime;
import jakarta.persistence.*;
import lombok.*;

/**
 * @author : 선순주
 * @packageName : com.fitian.burntz.domain.article.entity
 * @fileName : ArticleImage
 * @date : 2025-09-04
 * @description : ArticleImage 엔티티
 */

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "article_image")
public class ArticleImage extends BaseTime {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "article_image_pk")
    private Long articleImagePk;

    @Column(name = "article_image_url", length = 255)
    private String articleImageUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "article_pk", nullable = false)
    private Article article;

}