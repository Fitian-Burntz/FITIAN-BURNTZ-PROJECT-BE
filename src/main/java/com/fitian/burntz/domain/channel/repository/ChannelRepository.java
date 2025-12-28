package com.fitian.burntz.domain.channel.repository;

import com.fitian.burntz.domain.channel.entity.Channel;
import com.fitian.burntz.global.common.entity.BaseTime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 * @author : 김관중
 * @packageName : com.fitian.burntz.domain.channel.repository
 * @fileName : ChannelRepository
 * @date : 2025-09-08
 * @description : 채널(채팅) 리포지토리입니다.
 */
public interface ChannelRepository extends JpaRepository<Channel, Long> {
    Optional<Channel> findByChannelId(String channelId);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Channel c " +
            "SET c.deletedYN = :yn, c.updatedAt = CURRENT_TIMESTAMP " +
            "WHERE c.channelPk = :channelPk")
    int markDeletedByChannelPk(@Param("channelPk") Long channelPk, @Param("yn") BaseTime.Yn yn);
}
