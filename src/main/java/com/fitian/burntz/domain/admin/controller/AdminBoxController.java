package com.fitian.burntz.domain.admin.controller;

import com.fitian.burntz.domain.admin.dto.AdminAccount;
import com.fitian.burntz.domain.admin.dto.response.AdminBoxDetailResponse;
import com.fitian.burntz.domain.box.entity.Box;
import com.fitian.burntz.domain.box.repository.BoxRepository;
import com.fitian.burntz.domain.member.entity.MemberList;
import com.fitian.burntz.domain.member.repository.MemberListRepository;
import com.fitian.burntz.domain.member.repository.MemberRepository;
import com.fitian.burntz.domain.membership.entity.Membership;
import com.fitian.burntz.domain.membership.repository.MembershipRepository;
import com.fitian.burntz.global.common.entity.BaseTime;
import com.fitian.burntz.global.common.response.ApiResponse;
import com.fitian.burntz.global.exception.ErrorCode;
import com.fitian.burntz.global.exception.NotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin")
public class AdminBoxController {

    private final AdminAccount adminAccount;
    private final BoxRepository boxRepository;
    private final MemberRepository memberRepository;
    private final MemberListRepository memberListRepository;
    private final MembershipRepository membershipRepository;

    @GetMapping("/boxes/{boxPk}/detail")
    public ApiResponse<AdminBoxDetailResponse> getBoxDetail(
            @PathVariable Long boxPk,
            HttpServletRequest request) {

        if (!adminAccount.validateAccount(request)) {
            log.info("[Admin] 관리자 인증 실패 - 박스 상세 조회 불가 (boxPk={})", boxPk);
            return ApiResponse.success(null);
        }

        Box box = boxRepository.findActiveById(boxPk)
                .orElseThrow(() -> new NotFoundException(ErrorCode.BOX_NOT_FOUND));

        AdminBoxDetailResponse.OwnerInfo ownerInfo = memberRepository.findActiveById(box.getOwnerPk())
                .map(owner -> AdminBoxDetailResponse.OwnerInfo.builder()
                        .memberPk(owner.getMemberPk())
                        .nickname(owner.getNickname())
                        .email(owner.getEmail())
                        .build())
                .orElse(null);

        List<MemberList> memberLists = memberListRepository.findAllByBoxAndDeletedYN(box, BaseTime.Yn.N);

        List<Long> memberPks = memberLists.stream()
                .map(ml -> ml.getMember().getMemberPk())
                .toList();

        Map<Long, Membership> membershipMap = memberPks.isEmpty()
                ? Map.of()
                : membershipRepository.findLatestMembershipPerMemberByBox(boxPk, memberPks).stream()
                        .collect(Collectors.toMap(m -> m.getMember().getMemberPk(), m -> m));

        List<AdminBoxDetailResponse.MemberInfo> memberInfos = memberLists.stream()
                .map(ml -> {
                    Membership ms = membershipMap.get(ml.getMember().getMemberPk());
                    return AdminBoxDetailResponse.MemberInfo.builder()
                            .memberPk(ml.getMember().getMemberPk())
                            .boxNickname(ml.getBoxNickname())
                            .role(ml.getRole().name())
                            .email(ml.getMember().getEmail())
                            .membershipName(ms != null ? ms.getMembershipName() : null)
                            .membershipStatus(ms != null ? ms.getStatus().name() : null)
                            .startDate(ms != null ? ms.getStartDate() : null)
                            .expirationDate(ms != null ? ms.getExpirationDate() : null)
                            .build();
                })
                .toList();

        AdminBoxDetailResponse response = AdminBoxDetailResponse.builder()
                .boxPk(box.getBoxPk())
                .boxName(box.getBoxName())
                .boxCode(box.getBoxCode())
                .boxAddress(box.getBoxAddress())
                .boxContact(box.getBoxContact())
                .subscribeStatus(box.getSubscribe().name())
                .owner(ownerInfo)
                .memberCount(memberInfos.size())
                .members(memberInfos)
                .build();

        return ApiResponse.success(response);
    }
}
