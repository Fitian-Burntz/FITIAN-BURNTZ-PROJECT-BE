package com.fitian.burntz.domain.box.repository;

import com.fitian.burntz.domain.box.entity.BoxSubscription;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BoxSubscriptionRepository extends JpaRepository<BoxSubscription, Long> {

  @Query("select bs from BoxSubscription bs join fetch bs.member where bs.member.memberPk = :ownerMemberId")
  Optional<BoxSubscription> findByOwnerMemberId(Long ownerMemberId);

  @Query("select bs from BoxSubscription bs join fetch bs.member where bs.box.boxPk = :boxPk order by bs.boxSubscriptionPk desc limit 1")
  Optional<BoxSubscription> findByBoxPk(Long boxPk);

  @Query("""
      select bs
      from BoxSubscription bs
      join fetch bs.member m
      join fetch bs.box b
      where m.memberPk = :ownerMemberId
        and b.boxPk = :boxPk
      """)
  Optional<BoxSubscription> findByOwnerMemberIdAndBoxPk(Long ownerMemberId, Long boxPk);

  @Modifying
  @Query("DELETE FROM BoxSubscription bs WHERE bs.box.boxPk = :boxPk")
  void deleteAllByBoxPk(@Param("boxPk") Long boxPk);
}