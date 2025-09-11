package com.fitian.burntz.domain.channel.repository;

import com.fitian.burntz.domain.box.entity.Box;
import com.fitian.burntz.domain.channel.entity.Channel;
import com.fitian.burntz.domain.channel.entity.ChannelParticipant;
import com.fitian.burntz.domain.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * @author : 김관중
 * @packageName : com.fitian.burntz.domain.channel.repository
 * @fileName : ChannelParticipantRepository
 * @date : 2025-09-09
 * @description : 채널(채팅)참여자 리포지토리입니다.
 */
public interface ChannelParticipantRepository extends JpaRepository<ChannelParticipant, Long> {
    List<ChannelParticipant> findByChannel(Channel channel);

    @Query("select cp.channel from ChannelParticipant cp where cp.member = :member and cp.channel.box = :box")
    List<Channel> findChannelsByMemberAndBox(@Param("member") Member member, @Param("box") Box box);

    @Query("select p.memberPk from ChannelParticipant p where p.channel = :channel and p.deletedYn = false")
    List<Long> findMemberPksByChannel(@Param("channel") Channel channel);
}
