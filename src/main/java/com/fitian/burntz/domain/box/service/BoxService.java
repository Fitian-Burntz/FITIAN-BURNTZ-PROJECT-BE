package com.fitian.burntz.domain.box.service;

import com.fitian.burntz.domain.box.dto.BoxDto;
import com.fitian.burntz.domain.box.dto.CreateBoxRequest;
import com.fitian.burntz.domain.box.dto.JoinBoxDto;
import com.fitian.burntz.domain.box.dto.UpdateBoxInfoDto;
import com.fitian.burntz.global.security.core.CustomUserDetails;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface BoxService {

    /** boxPk 로 활성화 박스 단건 조회 **/
    BoxDto getBoxForPk(Long boxPk);

    /** boxCode 로 활성화 박스 단건 조회 **/
    BoxDto getBoxForBoxCode(String boxCode);

    /** 활성화 박스 리스트 전체 보기 (검색용) **/
    Page<BoxDto> getAllActiveBoxes(Pageable pageable);

    BoxDto createBox(CreateBoxRequest createBoxRequest, CustomUserDetails userDetails);

    /** box 에 가입 초기에는 GUEST **/
    JoinBoxDto joinMemberToBox (Long joinMemberPk, String belongBoxCode);

    /** box 정보 수정 **/
    UpdateBoxInfoDto updateBoxInfo(Long operatorPk, UpdateBoxInfoDto updateBoxInfoDto);

    /** box soft-delete **/
    public void removeBox(Long operatorPk, Long boxPk);

}
