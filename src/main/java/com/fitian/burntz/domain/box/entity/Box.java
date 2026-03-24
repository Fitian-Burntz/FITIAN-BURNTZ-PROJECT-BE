package com.fitian.burntz.domain.box.entity;

import com.fitian.burntz.domain.box.dto.CreateBoxRequest;
import com.fitian.burntz.domain.box.dto.UpdateBoxInfoDto;
import com.fitian.burntz.domain.box.enums.SubscribeYN;
import com.fitian.burntz.global.common.entity.BaseTime;
import jakarta.persistence.*;
import lombok.*;

import java.util.Objects;

import static com.fitian.burntz.global.common.util.StringUtil.trimToNull;

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


    /** Box 생성 정적 팩토리 매서드 **/
    public static Box create(Long ownerPk, CreateBoxRequest createBoxRequest) {
        return Box.builder()
                .boxName(trimToNull(createBoxRequest.getBoxName()))
                .ownerPk(ownerPk)
                .boxCode(trimToNull(createBoxRequest.getBoxCode()))
                .boxContact(trimToNull(createBoxRequest.getBoxContact()))
                .boxAddress(trimToNull(createBoxRequest.getBoxAddress()))
                .boxScript(trimToNull(createBoxRequest.getBoxScript()))
                .placeId(trimToNull(createBoxRequest.getPlaceId()))
                .boxFeeUrl(trimToNull(createBoxRequest.getBoxFeeUrl()))
                .boxTimetableUrl(trimToNull(createBoxRequest.getBoxTimetableUrl()))
                .boxInsta(trimToNull(createBoxRequest.getBoxInsta()))
                .build();
    }

    /** boxInfoUpdate **/
    // null 허용 값이 null 일 경우 기존 값 지움 처리
    public void updateInfo(UpdateBoxInfoDto updateBoxInfoDto) {
        this.boxName = updateBoxInfoDto.getBoxName();
        this.boxCode = updateBoxInfoDto.getBoxCode();
        this.boxContact = updateBoxInfoDto.getBoxContact();
        this.boxAddress = updateBoxInfoDto.getBoxAddress();
        this.boxScript = updateBoxInfoDto.getBoxScript();
        this.placeId = updateBoxInfoDto.getPlaceId();
        this.boxFeeUrl = updateBoxInfoDto.getBoxFeeUrl();
        this.boxTimetableUrl = updateBoxInfoDto.getBoxTimetableUrl();
        this.boxInsta = updateBoxInfoDto.getBoxInsta();
    }

    public void subscribe() {
        this.subscribe = SubscribeYN.Y;
    }

    public void subscribeCancel() {
        this.subscribe = SubscribeYN.C;
    }

    public void unsubscribe() {
        this.subscribe = SubscribeYN.N;
    }


    /** changeBoxOwnerPk **/
    public void changeBoxOwnerPk(Long ownerPk){
        Objects.requireNonNull(ownerPk, "ownerPk required");

        // 이미 같은 owner면 아무 작업도 하지 않음 -> 불필요한 dirty 체크/DB UPDATE 방지
        if (Objects.equals(this.ownerPk, ownerPk)) return;
        this.ownerPk = ownerPk;
    }

}