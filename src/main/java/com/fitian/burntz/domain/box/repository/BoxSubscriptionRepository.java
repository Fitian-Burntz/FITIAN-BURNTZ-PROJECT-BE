package com.fitian.burntz.domain.box.repository;

import com.fitian.burntz.domain.box.entity.BoxSubscription;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface BoxSubscriptionRepository extends JpaRepository<BoxSubscription, Long> {

  @Query("select bs from BoxSubscription bs join fetch bs.member where bs.member.memberPk = :ownerMemberId")
  Optional<BoxSubscription> findByOwnerMemberId(Long ownerMemberId);

  @Query("select bs from BoxSubscription bs join fetch bs.member where bs.box.boxPk = :boxPk")
  Optional<BoxSubscription> findByBoxPk(Long boxPk);

}