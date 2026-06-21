package com.fitian.burntz.domain.member.entity;

import com.fitian.burntz.domain.box.entity.Box;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MemberListProfileImageTest {

    @Test
    @DisplayName("초기 profileImageUrl은 null")
    void initialProfileImageUrl_isNull() {
        MemberList ml = buildMemberList();

        assertThat(ml.getProfileImageUrl()).isNull();
    }

    @Test
    @DisplayName("updateProfileImageUrl - URL이 정상 저장됨")
    void updateProfileImageUrl_savesUrl() {
        MemberList ml = buildMemberList();
        String thumbUrl = "https://bucket.s3.ap-northeast-2.amazonaws.com/images/profile/1/2/thumb.jpg";

        ml.updateProfileImageUrl(thumbUrl);

        assertThat(ml.getProfileImageUrl()).isEqualTo(thumbUrl);
    }

    @Test
    @DisplayName("updateProfileImageUrl - null로 덮어쓰기 가능")
    void updateProfileImageUrl_withNull_setsNull() {
        MemberList ml = buildMemberList();
        ml.updateProfileImageUrl("some-url");

        ml.updateProfileImageUrl(null);

        assertThat(ml.getProfileImageUrl()).isNull();
    }

    @Test
    @DisplayName("updateProfileImageUrl - 여러 번 호출 시 마지막 값이 유지됨")
    void updateProfileImageUrl_multipleUpdates_keepsLast() {
        MemberList ml = buildMemberList();

        ml.updateProfileImageUrl("https://example.com/old.jpg");
        ml.updateProfileImageUrl("https://example.com/new.jpg");

        assertThat(ml.getProfileImageUrl()).isEqualTo("https://example.com/new.jpg");
    }

    @Test
    @DisplayName("joinNewMemberToBox로 생성된 MemberList는 profileImageUrl이 null")
    void joinNewMemberToBox_profileImageUrl_isNull() {
        Member member = Member.create("id", "nick", "e@test.com", "google");
        Box box = Box.builder().boxPk(1L).ownerPk(99L).boxName("box").boxCode("B01").build();

        MemberList ml = MemberList.joinNewMemberToBox(member, box);

        assertThat(ml.getProfileImageUrl()).isNull();
    }

    // ---- helper ----

    private MemberList buildMemberList() {
        Member member = Member.create("id", "nick", "e@test.com", "google");
        Box box = Box.builder().boxPk(2L).ownerPk(1L).boxName("박스").boxCode("B01").build();
        return MemberList.joinNewMemberToBox(member, box);
    }
}
