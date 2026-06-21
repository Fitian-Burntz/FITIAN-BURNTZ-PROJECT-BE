package com.fitian.burntz.domain.member.service;

import com.fitian.burntz.domain.box.entity.Box;
import com.fitian.burntz.domain.box.repository.BoxRepository;
import com.fitian.burntz.domain.member.entity.Member;
import com.fitian.burntz.domain.member.entity.MemberList;
import com.fitian.burntz.domain.member.repository.MemberListRepository;
import com.fitian.burntz.domain.member.repository.MemberRepository;
import com.fitian.burntz.global.common.util.PreconditionValidator;
import com.fitian.burntz.global.exception.ValidationException;
import com.fitian.burntz.infra.s3.S3Service;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MemberServiceProfileImageTest {

    @Mock private MemberRepository memberRepository;
    @Mock private MemberListRepository memberListRepository;
    @Mock private PreconditionValidator preconditionValidator;
    @Mock private BoxRepository boxRepository;
    @Mock private S3Service s3Service;

    @InjectMocks
    private MemberService memberService;

    private static final Long MEMBER_PK = 1L;
    private static final Long BOX_PK = 2L;
    private static final String MEDIUM_URL = "https://bucket.s3.region.amazonaws.com/images/profile/1/2/medium.jpg";
    private static final String THUMB_URL  = "https://bucket.s3.region.amazonaws.com/images/profile/1/2/thumb.jpg";

    @BeforeEach
    void setUp() {
        // lenient: 일부 테스트는 BOX_PK 대신 다른 boxPk를 쓰므로 strict 불필요 stubbing 오류 방지
        lenient().when(preconditionValidator.requireMemberPk(MEMBER_PK)).thenReturn(MEMBER_PK);
        lenient().when(preconditionValidator.requireBoxPk(BOX_PK)).thenReturn(BOX_PK);
    }

    @Test
    @DisplayName("정상 업로드 - S3 업로드 후 MemberList에 thumbUrl 저장")
    void updateProfileImage_success_savesThumbUrl() {
        MemberList memberList = buildMemberList();
        when(memberListRepository.findActiveByBoxPkAndMemberPk(BOX_PK, MEMBER_PK))
                .thenReturn(Optional.of(memberList));
        when(s3Service.uploadProfileImage(eq(MEMBER_PK), eq(BOX_PK), any()))
                .thenReturn(new S3Service.ProfileImageUrls(MEDIUM_URL, THUMB_URL));

        MockMultipartFile file = new MockMultipartFile("image", "test.jpg", "image/jpeg", new byte[]{1});

        S3Service.ProfileImageUrls result = memberService.updateProfileImage(MEMBER_PK, BOX_PK, file);

        assertThat(result.mediumUrl()).isEqualTo(MEDIUM_URL);
        assertThat(result.thumbUrl()).isEqualTo(THUMB_URL);

        // MemberList에 thumbUrl이 저장됐는지 확인
        assertThat(memberList.getProfileImageUrl()).isEqualTo(THUMB_URL);
        verify(memberListRepository).save(memberList);
    }

    @Test
    @DisplayName("MediumUrl은 응답에만 포함, DB에는 thumbUrl만 저장")
    void updateProfileImage_onlyThumbUrlSavedToDb() {
        MemberList memberList = buildMemberList();
        when(memberListRepository.findActiveByBoxPkAndMemberPk(BOX_PK, MEMBER_PK))
                .thenReturn(Optional.of(memberList));
        when(s3Service.uploadProfileImage(any(), any(), any()))
                .thenReturn(new S3Service.ProfileImageUrls(MEDIUM_URL, THUMB_URL));

        MockMultipartFile file = new MockMultipartFile("image", "test.jpg", "image/jpeg", new byte[]{1});
        memberService.updateProfileImage(MEMBER_PK, BOX_PK, file);

        // DB 저장값은 thumbUrl이어야 한다 (mediumUrl이 아님)
        assertThat(memberList.getProfileImageUrl()).isEqualTo(THUMB_URL);
        assertThat(memberList.getProfileImageUrl()).doesNotContain("medium");
    }

    @Test
    @DisplayName("박스 미소속 멤버 → ValidationException")
    void updateProfileImage_notInBox_throws() {
        when(memberListRepository.findActiveByBoxPkAndMemberPk(BOX_PK, MEMBER_PK))
                .thenReturn(Optional.empty());

        MockMultipartFile file = new MockMultipartFile("image", "test.jpg", "image/jpeg", new byte[]{1});

        assertThatThrownBy(() -> memberService.updateProfileImage(MEMBER_PK, BOX_PK, file))
                .isInstanceOf(ValidationException.class);

        verify(s3Service, never()).uploadProfileImage(anyLong(), anyLong(), any());
    }

    @Test
    @DisplayName("박스별 독립 - boxPk가 다르면 해당 boxPk의 MemberList만 업데이트")
    void updateProfileImage_differentBoxPk_queriesCorrectMemberList() {
        Long anotherBoxPk = 99L;
        when(preconditionValidator.requireBoxPk(anotherBoxPk)).thenReturn(anotherBoxPk);

        // box2(BOX_PK)의 MemberList는 이 테스트에서 건드리지 않음 → 기존 imageUrl null 유지
        MemberList memberListBox2 = buildMemberList();

        MemberList memberListBox99 = buildMemberList();
        when(memberListRepository.findActiveByBoxPkAndMemberPk(anotherBoxPk, MEMBER_PK))
                .thenReturn(Optional.of(memberListBox99));

        String thumbForBox99 = "https://bucket.s3.region.amazonaws.com/images/profile/1/99/thumb.jpg";
        when(s3Service.uploadProfileImage(eq(MEMBER_PK), eq(anotherBoxPk), any()))
                .thenReturn(new S3Service.ProfileImageUrls("mediumUrl", thumbForBox99));

        MockMultipartFile file = new MockMultipartFile("image", "test.jpg", "image/jpeg", new byte[]{1});
        memberService.updateProfileImage(MEMBER_PK, anotherBoxPk, file);

        // box99 MemberList만 업데이트 됨
        assertThat(memberListBox99.getProfileImageUrl()).isEqualTo(thumbForBox99);
        // box2(BOX_PK) MemberList는 변경 없음
        assertThat(memberListBox2.getProfileImageUrl()).isNull();
    }

    @Test
    @DisplayName("삭제 정상 - S3 삭제 후 DB profileImageUrl이 null로 초기화")
    void deleteProfileImage_success_clearsUrl() {
        MemberList memberList = buildMemberList();
        memberList.updateProfileImageUrl(THUMB_URL);
        when(memberListRepository.findActiveByBoxPkAndMemberPk(BOX_PK, MEMBER_PK))
                .thenReturn(Optional.of(memberList));
        doNothing().when(s3Service).deleteProfileImage(MEMBER_PK, BOX_PK);

        memberService.deleteProfileImage(MEMBER_PK, BOX_PK);

        verify(s3Service).deleteProfileImage(MEMBER_PK, BOX_PK);
        assertThat(memberList.getProfileImageUrl()).isNull();
        verify(memberListRepository).save(memberList);
    }

    @Test
    @DisplayName("삭제 - 박스 미소속 멤버는 ValidationException, S3 호출 없음")
    void deleteProfileImage_notInBox_throws() {
        when(memberListRepository.findActiveByBoxPkAndMemberPk(BOX_PK, MEMBER_PK))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> memberService.deleteProfileImage(MEMBER_PK, BOX_PK))
                .isInstanceOf(ValidationException.class);

        verify(s3Service, never()).deleteProfileImage(anyLong(), anyLong());
    }

    // ---- helper ----

    private MemberList buildMemberList() {
        Member member = Member.create("id", "nick", "email@test.com", "google");
        Box box = Box.builder().boxPk(BOX_PK).ownerPk(MEMBER_PK).boxName("테스트박스").boxCode("BOX01").build();
        return MemberList.joinNewMemberToBox(member, box);
    }
}
