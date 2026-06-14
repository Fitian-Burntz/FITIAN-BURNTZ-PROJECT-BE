package com.fitian.burntz.domain.record.v1.dto;

import com.fitian.burntz.domain.box.entity.Box;
import com.fitian.burntz.domain.member.entity.Member;
import com.fitian.burntz.domain.member.entity.MemberList;
import com.fitian.burntz.domain.record.entity.Record;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RecordResponseTest {

    @Test
    @DisplayName("from() - MemberList에 profileImageUrl이 있으면 응답에 포함")
    void from_memberListHasImage_includesProfileImageUrl() {
        MemberList ml = buildMemberListWithImage("https://example.com/thumb.jpg");
        Record record = buildRecord(ml);

        RecordResponse response = RecordResponse.from(record);

        assertThat(response.getProfileImageUrl()).isEqualTo("https://example.com/thumb.jpg");
    }

    @Test
    @DisplayName("from() - MemberList profileImageUrl이 null이면 응답도 null")
    void from_memberListNoImage_profileImageUrlIsNull() {
        MemberList ml = buildMemberListWithImage(null);
        Record record = buildRecord(ml);

        RecordResponse response = RecordResponse.from(record);

        assertThat(response.getProfileImageUrl()).isNull();
    }

    @Test
    @DisplayName("from() - memberList 자체가 null이면 profileImageUrl은 null")
    void from_nullMemberList_profileImageUrlIsNull() {
        Record record = Record.builder()
                .nickname("tester")
                .build();

        RecordResponse response = RecordResponse.from(record);

        assertThat(response.getProfileImageUrl()).isNull();
    }

    @Test
    @DisplayName("from() - null Record 입력 시 null 반환")
    void from_nullRecord_returnsNull() {
        assertThat(RecordResponse.from(null)).isNull();
    }

    @Test
    @DisplayName("fromWithRank() - rank 값이 응답에 반영됨")
    void fromWithRank_rankIsSet() {
        MemberList ml = buildMemberListWithImage("https://example.com/thumb.jpg");
        Record record = buildRecord(ml);

        RecordResponse response = RecordResponse.fromWithRank(record, 3);

        assertThat(response.getRank()).isEqualTo(3);
        assertThat(response.getProfileImageUrl()).isEqualTo("https://example.com/thumb.jpg");
    }

    // ---- helpers ----

    private MemberList buildMemberListWithImage(String imageUrl) {
        Member member = Member.create("id", "nick", "e@test.com", "google");
        Box box = Box.builder().boxPk(1L).ownerPk(1L).boxName("box").boxCode("B01").build();
        MemberList ml = MemberList.joinNewMemberToBox(member, box);
        if (imageUrl != null) {
            ml.updateProfileImageUrl(imageUrl);
        }
        return ml;
    }

    private Record buildRecord(MemberList memberList) {
        return Record.builder()
                .memberList(memberList)
                .nickname("tester")
                .build();
    }
}
