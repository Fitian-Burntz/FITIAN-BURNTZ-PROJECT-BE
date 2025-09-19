package com.fitian.burntz.domain.box.service;

import com.fitian.burntz.domain.box.dto.BoxDto;
import com.fitian.burntz.domain.box.entity.Box;
import com.fitian.burntz.domain.box.repository.BoxRepository;
import com.fitian.burntz.global.common.entity.BaseTime;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BoxServiceImpl implements BoxService {

    private final BoxRepository boxRepository;

    @Override
    public Page<BoxDto> getAllActiveBoxes(Pageable pageable) {
        Page<Box> page = boxRepository.findAllByDeletedYN(BaseTime.Yn.N, pageable);
        return page.map(BoxDto::from);
    }
}
