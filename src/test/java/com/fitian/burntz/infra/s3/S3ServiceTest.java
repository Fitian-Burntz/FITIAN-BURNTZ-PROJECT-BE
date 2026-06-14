package com.fitian.burntz.infra.s3;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class S3ServiceTest {

    @Mock
    private S3Client s3Client;

    private S3Service s3Service;

    private static final String BUCKET = "test-bucket";
    private static final String REGION = "ap-northeast-2";

    @BeforeEach
    void setUp() {
        s3Service = new S3Service(s3Client);
        ReflectionTestUtils.setField(s3Service, "bucket", BUCKET);
        ReflectionTestUtils.setField(s3Service, "region", REGION);
    }

    @Test
    @DisplayName("정상 이미지 업로드 - S3에 medium·thumb 두 번 업로드하고 URL 반환")
    void uploadProfileImage_success() throws Exception {
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());

        MockMultipartFile file = createTestJpeg();

        S3Service.ProfileImageUrls result = s3Service.uploadProfileImage(1L, 2L, file);

        verify(s3Client, times(2)).putObject(any(PutObjectRequest.class), any(RequestBody.class));

        String expectedBase = "https://" + BUCKET + ".s3." + REGION + ".amazonaws.com/";
        assertThat(result.mediumUrl()).isEqualTo(expectedBase + "images/profile/1/2/medium.jpg");
        assertThat(result.thumbUrl()).isEqualTo(expectedBase + "images/profile/1/2/thumb.jpg");
    }

    @Test
    @DisplayName("S3 키 구조 - memberPk·boxPk가 경로에 포함돼야 함")
    void uploadProfileImage_s3KeyContainsMemberAndBoxPk() throws Exception {
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());

        MockMultipartFile file = createTestJpeg();

        S3Service.ProfileImageUrls result = s3Service.uploadProfileImage(10L, 99L, file);

        assertThat(result.mediumUrl()).contains("/images/profile/10/99/medium.jpg");
        assertThat(result.thumbUrl()).contains("/images/profile/10/99/thumb.jpg");
    }

    @Test
    @DisplayName("파일 null → IllegalArgumentException")
    void uploadProfileImage_nullFile_throws() {
        assertThatThrownBy(() -> s3Service.uploadProfileImage(1L, 1L, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("이미지 파일이 없습니다");
    }

    @Test
    @DisplayName("빈 파일 → IllegalArgumentException")
    void uploadProfileImage_emptyFile_throws() {
        MockMultipartFile empty = new MockMultipartFile("image", "test.jpg", "image/jpeg", new byte[0]);

        assertThatThrownBy(() -> s3Service.uploadProfileImage(1L, 1L, empty))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("이미지 파일이 없습니다");
    }

    @Test
    @DisplayName("이미지가 아닌 content-type → IllegalArgumentException")
    void uploadProfileImage_nonImageContentType_throws() {
        MockMultipartFile pdf = new MockMultipartFile("image", "doc.pdf", "application/pdf", new byte[]{1, 2, 3});

        assertThatThrownBy(() -> s3Service.uploadProfileImage(1L, 1L, pdf))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("이미지 파일만 업로드 가능");
    }

    @Test
    @DisplayName("10MB 초과 파일 → IllegalArgumentException")
    void uploadProfileImage_fileTooLarge_throws() {
        byte[] oversized = new byte[10 * 1024 * 1024 + 1];
        MockMultipartFile large = new MockMultipartFile("image", "big.jpg", "image/jpeg", oversized);

        assertThatThrownBy(() -> s3Service.uploadProfileImage(1L, 1L, large))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("10MB");
    }

    // ---- helper ----

    private MockMultipartFile createTestJpeg() throws Exception {
        BufferedImage img = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "jpg", baos);
        return new MockMultipartFile("image", "test.jpg", "image/jpeg", baos.toByteArray());
    }
}
