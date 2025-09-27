package com.fitian.burntz.domain.box.repository;

import com.fitian.burntz.domain.box.entity.SubscriptionEventLog;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface SubscriptionEventLogRepository extends JpaRepository<SubscriptionEventLog, Long> {

  @Query("select s from SubscriptionEventLog s where s.box.boxPk = :boxPk order by s.box.boxPk")
  List<SubscriptionEventLog> findAllByBoxPk(Long boxPk);
}
