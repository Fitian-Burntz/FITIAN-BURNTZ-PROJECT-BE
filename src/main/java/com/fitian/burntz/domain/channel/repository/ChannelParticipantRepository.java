package com.fitian.burntz.domain.channel.repository;

import com.fitian.burntz.domain.box.entity.Box;
import com.fitian.burntz.domain.channel.entity.Channel;
import com.fitian.burntz.domain.channel.entity.ChannelParticipant;
import com.fitian.burntz.domain.member.entity.Member;
import com.fitian.burntz.global.common.entity.BaseTime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
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

    @Query("select case when (count(cp) > 0) then true else false end " +
            "from ChannelParticipant cp where cp.member.memberPk = :memberPk and cp.channel.channelPk = :channelPk and cp.deletedYN = :YN")
    boolean existByChannelPkAndMemberPkAndDeletedYN(
            @Param("memberPk") Long memberPk,
            @Param("channelPk") Long channelPk,
            @Param("YN") BaseTime.Yn yn
    );

    @Query("select cp.channel from ChannelParticipant cp where cp.member = :member and cp.channel.box = :box and cp.deletedYN = 'N'")
    List<Channel> findChannelsByMemberAndBox(@Param("member") Member member, @Param("box") Box box);

    @Query("select p.member.memberPk from ChannelParticipant p where p.channel = :channel and p.deletedYN = 'N'")
    List<Long> findMemberPksByChannel(@Param("channel") Channel channel);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE ChannelParticipant c " +
            "SET c.deletedYN = :yn, c.updatedAt = CURRENT_TIMESTAMP " +
            "WHERE c.channel.channelPk = :channelPk and c.member.memberPk = :memberPk")
    int markDeletedByPk(@Param("channelPk") Long channelPk, @Param("memberPk") Long memberPk, @Param("yn") BaseTime.Yn yn);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE ChannelParticipant c " +
            "SET c.deletedYN = :yn, c.updatedAt = CURRENT_TIMESTAMP " +
            "WHERE c.channel.channelPk = :channelPk")
    int markDeletedByChannelPk(@Param("channelPk") Long channelPk, @Param("yn") BaseTime.Yn yn);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE ChannelParticipant c " +
            "SET c.deletedYN = :yn, c.updatedAt = CURRENT_TIMESTAMP " +
            "WHERE c.channel.channelPk IN :channelPkList " +
            "  AND c.member.memberPk = :memberPk")
    int markDeletedByMemberPkAndChannelPkIn(@Param("memberPk") Long memberPk, @Param("channelPkList") Collection<Long> channelPkList, @Param("yn") BaseTime.Yn yn);

    boolean existsByMemberAndChannel_ChannelPkAndDeletedYN(Member member, Long channelPk, BaseTime.Yn deletedYN);
}
