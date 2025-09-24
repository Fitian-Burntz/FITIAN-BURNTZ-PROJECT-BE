package com.fitian.burntz.domain.box.service;

import com.fitian.burntz.domain.box.dto.BoxDto;
import com.fitian.burntz.domain.box.dto.CreateBoxRequest;
import com.fitian.burntz.domain.box.dto.JoinBoxDto;
import com.fitian.burntz.domain.box.entity.Box;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface BoxService {

    Page<BoxDto> getAllActiveBoxes(Pageable pageable);

    BoxDto createBox(Long ownerPk, CreateBoxRequest createBoxRequest);

    /** box 에 가입 초기에는 GUEST **/
    JoinBoxDto joinMemberToBox (Long joinMemberPk, String belongBoxCode);
}
