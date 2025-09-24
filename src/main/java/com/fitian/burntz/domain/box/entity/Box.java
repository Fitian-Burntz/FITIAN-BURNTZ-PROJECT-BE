package com.fitian.burntz.domain.box.entity;

import com.fitian.burntz.domain.box.dto.BoxDto;
import com.fitian.burntz.domain.box.dto.CreateBoxRequest;
import com.fitian.burntz.domain.box.enums.SubscribeYN;
import com.fitian.burntz.global.common.entity.BaseTime;
import jakarta.persistence.*;
import lombok.*;

/**
 * @author : 선순주
 * @packageName : com.fitian.burntz.domain.box.entity
 * @fileName : Box
 * @date : 2025-09-04
 * @description : Box 엔티티
 */

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "box",
        uniqueConstraints = @UniqueConstraint(name = "uk_box_code", columnNames = "box_code"),
        indexes = {
                @Index(name = "idx_box_owner_pk", columnList = "owner_pk")
        })
public class  Box extends BaseTime {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "box_pk")
    private Long boxPk;

    @Column(name = "owner_pk", nullable = false)
    private Long ownerPk;

    @Column(name = "box_name", nullable = false, length = 100)
    private String boxName;

    @Column(name = "box_code", nullable = false, length = 100)
    private String boxCode;

    @Column(name = "box_contact", length = 200)
    private String boxContact;

    @Column(name = "box_address",columnDefinition="text")
    private String boxAddress;

    @Column(name = "box_script",columnDefinition="text")
    private String boxScript;

    @Column(name = "place_id", length = 255)
    private String placeId;

    @Column(name = "box_fee_url", length = 255)
    private String boxFeeUrl;


    @Column(name = "box_timetable_url", length = 255)
    private String boxTimetableUrl;


    @Column(name = "box_insta", length = 255)
    private String boxInsta;


    @Enumerated(EnumType.STRING)
    @Column(name = "subscribe", length = 1)
    @Builder.Default
    private SubscribeYN subscribe = SubscribeYN.N; // Y/N


    /** Box 생성 적적 팩토리 매서드 **/
    public static Box create(Long ownerPk, CreateBoxRequest createBoxRequest) {
        return Box.builder()
                .boxName(createBoxRequest.getBoxName())
                .ownerPk(ownerPk)
                .boxCode(createBoxRequest.getBoxCode())
                .boxContact(createBoxRequest.getBoxContact())
                .boxAddress(createBoxRequest.getBoxAddress())
                .boxScript(createBoxRequest.getBoxScript())
                .placeId(createBoxRequest.getPlaceId())
                .boxFeeUrl(createBoxRequest.getBoxFeeUrl())
                .boxTimetableUrl(createBoxRequest.getBoxTimetableUrl())
                .boxInsta(createBoxRequest.getBoxInsta())
                .build();
    }
}