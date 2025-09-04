package com.fitian.burntz.global.common.entity;

import com.fasterxml.jackson.databind.ser.Serializers;
import jakarta.persistence.*;
import lombok.*;

/**
 * @author : 선순주
 * @packageName : com.fitian.burntz.global.common.entity
 * @fileName : Agreement
 * @date : 2025-09-04
 * @description : 약관 entity
 */

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "agreement")
public class Agreement extends BaseTime {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "agreement_pk")
    private Long agreementPk;

    @Column(name = "language", length = 10)
    private String language;

    @Column(name = "title", length = 50)
    private String title;

    @Column(name = "content",columnDefinition="text")
    private String content;
}