package com.fitian.burntz.infra.s3;

import lombok.RequiredArgsConstructor;
import net.coobird.thumbnailator.Thumbnails;
import net.coobird.thumbnailator.geometry.Positions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Service
@RequiredArgsConstructor
public class S3Service {

    private final S3Client s3Client;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    @Value("${cloud.aws.region.static}")
    private String region;

    public ProfileImageUrls uploadProfileImage(Long memberPk, MultipartFile file) {
        validateImageFile(file);

        String mediumKey = "images/profile/" + memberPk + "/medium.jpg";
        String thumbKey  = "images/profile/" + memberPk + "/thumb.jpg";

        try {
            upload(mediumKey, resizeMedium(file));
            upload(thumbKey,  resizeThumb(file));
        } catch (IOException e) {
            throw new RuntimeException("이미지 처리 중 오류가 발생했습니다.", e);
        }

        String baseUrl = "https://" + bucket + ".s3." + region + ".amazonaws.com/";
        return new ProfileImageUrls(baseUrl + mediumKey, baseUrl + thumbKey);
    }

    public void deleteProfileImage(Long memberPk) {
        deleteObject("images/profile/" + memberPk + "/medium.jpg");
        deleteObject("images/profile/" + memberPk + "/thumb.jpg");
    }

    private byte[] resizeMedium(MultipartFile file) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Thumbnails.of(file.getInputStream())
                .size(800, 800)
                .keepAspectRatio(true)
                .outputFormat("jpg")
                .toOutputStream(out);
        return out.toByteArray();
    }

    private byte[] resizeThumb(MultipartFile file) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Thumbnails.of(file.getInputStream())
                .size(200, 200)
                .crop(Positions.CENTER)
                .outputFormat("jpg")
                .toOutputStream(out);
        return out.toByteArray();
    }

    private void upload(String key, byte[] data) {
        s3Client.putObject(
                PutObjectRequest.builder()
                        .bucket(bucket)
                        .key(key)
                        .contentType("image/jpeg")
                        .build(),
                RequestBody.fromBytes(data)
        );
    }

    private void deleteObject(String key) {
        s3Client.deleteObject(
                DeleteObjectRequest.builder()
                        .bucket(bucket)
                        .key(key)
                        .build()
        );
    }

    private void validateImageFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("이미지 파일이 없습니다.");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("이미지 파일만 업로드 가능합니다.");
        }
        if (file.getSize() > 10 * 1024 * 1024) {
            throw new IllegalArgumentException("파일 크기는 10MB를 초과할 수 없습니다.");
        }
    }

    public record ProfileImageUrls(String mediumUrl, String thumbUrl) {}
}
