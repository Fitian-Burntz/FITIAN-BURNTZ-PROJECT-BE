package com.fitian.burntz.domain.membership.repository;

import com.fitian.burntz.domain.membership.entity.BoxHoldingPolicy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BoxHoldingPolicyRepository extends JpaRepository<BoxHoldingPolicy, Long> {

    Optional<BoxHoldingPolicy> findByBoxBoxPk(Long boxPk);
}
