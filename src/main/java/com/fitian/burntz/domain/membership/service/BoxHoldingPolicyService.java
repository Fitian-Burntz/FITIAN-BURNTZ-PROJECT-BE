package com.fitian.burntz.domain.membership.service;

import com.fitian.burntz.domain.box.entity.Box;
import com.fitian.burntz.domain.box.enums.MemberRole;
import com.fitian.burntz.domain.box.repository.BoxRepository;
import com.fitian.burntz.domain.member.entity.MemberList;
import com.fitian.burntz.domain.member.repository.MemberListRepository;
import com.fitian.burntz.domain.membership.entity.BoxHoldingPolicy;
import com.fitian.burntz.domain.membership.repository.BoxHoldingPolicyRepository;
import com.fitian.burntz.domain.membership.v2.dto.BoxHoldingPolicyRequest;
import com.fitian.burntz.domain.membership.v2.dto.BoxHoldingPolicyResponse;
import com.fitian.burntz.global.common.entity.BaseTime;
import com.fitian.burntz.global.exception.ErrorCode;
import com.fitian.burntz.global.exception.ValidationException;
import com.fitian.burntz.global.security.core.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class BoxHoldingPolicyService {

    private final BoxRepository boxRepository;
    private final MemberListRepository memberListRepository;
    private final BoxHoldingPolicyRepository policyRepository;

    public BoxHoldingPolicyResponse upsertPolicy(Long boxPk, BoxHoldingPolicyRequest request, CustomUserDetails userDetails) {
        MemberList requester = memberListRepository.findRoleByMemberMemberPkAndBoxBoxPkAndDeletedYN(userDetails.getMemberPk(), boxPk, BaseTime.Yn.N)
                .orElseThrow(() -> new ValidationException(ErrorCode.USER_NOT_FOUND));

        if (requester.getRole() != MemberRole.MANAGER && requester.getRole() != MemberRole.OWNER) {
            throw new ValidationException(ErrorCode.ACCESS_DENIED);
        }

        BoxHoldingPolicy policy = policyRepository.findByBoxBoxPk(boxPk)
                .orElse(null);

        if (policy != null) {
            policy.update(request.getDefaultHoldDays());
            return BoxHoldingPolicyResponse.from(policy);
        }

        Box box = boxRepository.findByBoxPkAndDeletedYN(boxPk, BaseTime.Yn.N)
                .orElseThrow(() -> new ValidationException(ErrorCode.BOX_NOT_FOUND));

        BoxHoldingPolicy newPolicy = BoxHoldingPolicy.builder()
                .box(box)
                .defaultHoldDays(request.getDefaultHoldDays())
                .build();

        policyRepository.save(newPolicy);
        return BoxHoldingPolicyResponse.from(newPolicy);
    }

    @Transactional(readOnly = true)
    public BoxHoldingPolicyResponse getPolicy(Long boxPk, CustomUserDetails userDetails) {
        boolean exists = memberListRepository.existsByBoxBoxPkAndMemberMemberPkAndDeletedYN(boxPk, userDetails.getMemberPk(), BaseTime.Yn.N);
        if (!exists) throw new ValidationException(ErrorCode.ACCESS_DENIED);

        BoxHoldingPolicy policy = policyRepository.findByBoxBoxPk(boxPk)
                .orElseThrow(() -> new ValidationException(ErrorCode.HOLDING_POLICY_NOT_FOUND));

        return BoxHoldingPolicyResponse.from(policy);
    }
}
