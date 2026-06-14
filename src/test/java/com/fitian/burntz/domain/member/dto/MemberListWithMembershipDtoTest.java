package com.fitian.burntz.domain.member.dto;

import com.fitian.burntz.domain.box.entity.Box;
import com.fitian.burntz.domain.member.dto.memberList_dto.MemberListWithMembershipDto;
import com.fitian.burntz.domain.member.entity.Member;
import com.fitian.burntz.domain.member.entity.MemberList;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MemberListWithMembershipDtoTest {

    @Test
    @DisplayName("from() - MemberList에 profileImageUrl이 있으면 profileImageThumbUrl에 매핑")
    void from_withImage_mapsToThumbUrl() {
        MemberList ml = buildMemberListWithImage("https://example.com/thumb.jpg");

        MemberListWithMembershipDto dto = MemberListWithMembershipDto.from(ml, 1L, 2L, null);

        assertThat(dto.getProfileImageThumbUrl()).isEqualTo("https://example.com/thumb.jpg");
    }

    @Test
    @DisplayName("from() - 프로필 이미지 없는 멤버는 profileImageThumbUrl이 null")
    void from_noImage_thumbUrlIsNull() {
        MemberList ml = buildMemberListWithImage(null);

        MemberListWithMembershipDto dto = MemberListWithMembershipDto.from(ml, 1L, 2L, null);

        assertThat(dto.getProfileImageThumbUrl()).isNull();
    }

    @Test
    @DisplayName("from() - 이미지 업데이트 후 최신 URL이 반영됨")
    void from_afterImageUpdate_reflectsNewUrl() {
        MemberList ml = buildMemberListWithImage("https://example.com/old-thumb.jpg");
        ml.updateProfileImageUrl("https://example.com/new-thumb.jpg");

        MemberListWithMembershipDto dto = MemberListWithMembershipDto.from(ml, 1L, 2L, null);

        assertThat(dto.getProfileImageThumbUrl()).isEqualTo("https://example.com/new-thumb.jpg");
    }

    @Test
    @DisplayName("from() - memberList가 null이면 NullPointerException")
    void from_nullMemberList_throwsNullPointerException() {
        assertThatThrownBy(() -> MemberListWithMembershipDto.from(null, 1L, 2L, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("from() - memberPk가 null이면 NullPointerException")
    void from_nullMemberPk_throwsNullPointerException() {
        MemberList ml = buildMemberListWithImage(null);

        assertThatThrownBy(() -> MemberListWithMembershipDto.from(ml, null, 2L, null))
                .isInstanceOf(NullPointerException.class);
    }

    // ---- helper ----

    private MemberList buildMemberListWithImage(String imageUrl) {
        Member member = Member.create("id", "nick", "e@test.com", "google");
        Box box = Box.builder().boxPk(2L).ownerPk(1L).boxName("box").boxCode("B01").build();
        MemberList ml = MemberList.joinNewMemberToBox(member, box);
        if (imageUrl != null) {
            ml.updateProfileImageUrl(imageUrl);
        }
        return ml;
    }
}
