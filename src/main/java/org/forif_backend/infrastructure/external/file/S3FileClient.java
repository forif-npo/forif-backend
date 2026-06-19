package org.forif_backend.infrastructure.external.file;

import lombok.extern.slf4j.Slf4j;
import org.forif_backend.application.file.dto.FileInfo;
import org.forif_backend.application.file.port.out.FilePort;
import org.forif_backend.common.exception.ErrorCode;
import org.forif_backend.common.exception.ForifException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.io.IOException;
import java.time.Duration;
import java.util.UUID;

@Slf4j
@Component
@ConditionalOnProperty(name = "app.file.storage", havingValue = "s3")
public class S3FileClient implements FilePort {

    private final S3Presigner s3Presigner;
    private final S3Client s3Client;
    private final String bucketName;
    private final Duration presignedUrlDuration;

    public S3FileClient(S3Presigner s3Presigner,
                        S3Client s3Client,
                        @Value("${spring.cloud.aws.s3.bucket-name:}") String bucketName,
                        @Value("${spring.cloud.aws.presigned-url.expiration-minutes:60}") long expirationMinutes) {
        this.s3Presigner = s3Presigner;
        this.s3Client = s3Client;
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
            throw new ForifException(ErrorCode.INTERNAL_SERVER_ERROR);
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
            throw new ForifException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 파일을 S3에 직접 업로드합니다.
     *
     * @param file 업로드할 파일
     * @return S3에 저장된 객체 키
     */
    @Override
    public String uploadFile(MultipartFile file) {
        String objectKey = UUID.randomUUID() + "-" + file.getOriginalFilename();
        String contentType = file.getContentType();

        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(objectKey)
                    .contentType(contentType)
                    .build();

            s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

            log.info("파일 업로드 성공: {}", objectKey);
            return objectKey;

        } catch (IOException e) {
            log.error("파일 읽기 중 오류 발생", e);
            throw new ForifException(ErrorCode.INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            log.error("S3 파일 업로드 중 오류 발생", e);
            throw new ForifException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * S3에서 파일을 삭제합니다.
     *
     * @param objectKey 삭제할 S3 객체 키
     */
    @Override
    public void deleteFile(String objectKey) {
        try {
            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(objectKey)
                    .build();

            s3Client.deleteObject(deleteObjectRequest);

            log.info("파일 삭제 성공: {}", objectKey);

        } catch (Exception e) {
            log.error("S3 파일 삭제 중 오류 발생 (ObjectKey: {})", objectKey, e);
            throw new ForifException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }
}
