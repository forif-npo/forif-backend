package org.forif_backend.infrastructure.external.file;

import lombok.extern.slf4j.Slf4j;
import org.forif_backend.application.file.dto.FileInfo;
import org.forif_backend.application.file.port.out.FilePort;
import org.forif_backend.common.exception.ErrorCode;
import org.forif_backend.common.exception.ForifException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;
import java.util.UUID;

@Slf4j
@Component
public class S3FileClient implements FilePort {

    private final S3Presigner s3Presigner;
    private final String bucketName;
    private final Duration presignedUrlDuration;

    public S3FileClient(S3Presigner s3Presigner,
                        @Value("${spring.cloud.aws.s3.bucket-name}") String bucketName,
                        @Value("${spring.cloud.aws.presigned-url.expiration-minutes}") long expirationMinutes) {
        this.s3Presigner = s3Presigner;
        this.bucketName = bucketName;
        this.presignedUrlDuration = Duration.ofMinutes(expirationMinutes);
    }

    /**
     * 파일 업로드를 위한 Presigned URL을 생성합니다.
     *
     * @param file 업로드할 파일
     * @return FileInfo
     */
    @Override
    public FileInfo generatePresignedUploadUrl(MultipartFile file) {
        // S3에 저장될 파일명 생성
        String objectKey = UUID.randomUUID() + "-" + file.getOriginalFilename();
        String contentType = file.getContentType();

        try {
            // 업로드(PUT) 요청 객체 생성
            PutObjectRequest objectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(objectKey)
                    .contentType(contentType)
                    .build();

            // Presigned URL 생성 요청 객체 생성 (유효 시간 설정 포함)
            PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                    .signatureDuration(presignedUrlDuration)
                    .putObjectRequest(objectRequest)
                    .build();

            // S3Presigner를 사용하여 URL 생성
            String presignedUrl = s3Presigner.presignPutObject(presignRequest).url().toString();

            // FileInfo DTO로 래핑하여 반환
            return FileInfo.builder()
                    .objectKey(objectKey)
                    .presignedUrl(presignedUrl)
                    .build();

        } catch (Exception e) {
            log.error("업로드용 Presigned URL 생성 중 오류 발생", e);
            throw new ForifException(ErrorCode.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    /**
     * 파일 조회를 위한 Presigned URL을 생성합니다.
     *
     * @param objectKey 조회할 S3 객체 키
     * @return FileInfo
     */
    @Override
    public FileInfo generatePresignedViewUrl(String objectKey) {
        try {
            // 조회(GET) 요청 객체 생성
            GetObjectRequest objectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(objectKey)
                    .build();

            // Presigned URL 생성 요청 객체 생성 (유효 시간 설정 포함)
            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(presignedUrlDuration)
                    .getObjectRequest(objectRequest)
                    .build();

            // S3Presigner를 사용하여 URL 생성
            String presignedUrl = s3Presigner.presignGetObject(presignRequest).url().toString();

            // FileInfo DTO로 래핑하여 반환
            return FileInfo.builder()
                    .objectKey(objectKey)
                    .presignedUrl(presignedUrl)
                    .build();

        } catch (Exception e) {
            log.error("조회용 Presigned URL 생성 중 오류 발생 (ObjectKey: {})", objectKey, e);
            throw new ForifException(ErrorCode.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }
}
