package com.fitian.burntz.domain.member.service;

import com.fitian.burntz.domain.member.dto.memberList_dto.UpdateMemberRoleDto;
import com.fitian.burntz.domain.member.repository.MemberListRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class MemberListServiceImpl implements MemberListService{

    private final MemberListRepository memberListRepository;

    @Override
    public UpdateMemberRoleDto updateMemberRole(UpdateMemberRoleDto updateMemberRoleDto) {
        return updateMemberRoleDto;
    }
}
