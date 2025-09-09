package com.fitian.burntz.domain.channel.repository;

import com.fitian.burntz.domain.channel.entity.Channel;
import org.springframework.data.jpa.repository.JpaRepository;

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
}
