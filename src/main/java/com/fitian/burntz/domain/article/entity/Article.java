package com.fitian.burntz.domain.article.entity;

import com.fitian.burntz.domain.box.entity.Box;
import com.fitian.burntz.domain.member.entity.Member;
import com.fitian.burntz.global.common.entity.BaseTime;
import jakarta.persistence.*;
import lombok.*;


/**
 * @author : 선순주
 * @packageName : com.fitian.burntz.domain.article.entity
 * @fileName : Article
 * @date : 2025-09-04
 * @description : article 엔티티
 */

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "article")
public class Article extends BaseTime {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "article_pk")
    private Long articlePk;

    @Column(name = "article_title", length = 100)
    private String articleTitle;

    @Column(name = "article_script",columnDefinition="text")
    private String articleScript;

    @Column(name = "category", length = 50)
    private String category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_pk", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "box_pk", nullable = false)
    private Box box;
}