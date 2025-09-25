package com.fitian.burntz.domain.box.repository;

import com.fitian.burntz.domain.box.entity.SubscriptionEventLog;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubscriptionEventLogRepository extends JpaRepository<SubscriptionEventLog, Long> {
  List<SubscriptionEventLog> findAllByBoxPk(Long boxPk);
}
