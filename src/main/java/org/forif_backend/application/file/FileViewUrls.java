package org.forif_backend.application.file;

import org.forif_backend.application.file.port.out.FilePort;

/**
 * 저장된 objectKey를 클라이언트가 볼 수 있는 조회 URL로 바꾸는 규칙.
 * 여러 도메인 서비스가 같은 규칙을 쓰므로 한곳에 모아 둔다.
 *
 * <p>FilePort의 default 메서드로 두지 않는 이유: 포트를 목으로 대체하는 테스트에서
 * default 메서드까지 가로채여 항상 null이 반환된다.
 */
public final class FileViewUrls {

    private FileViewUrls() {
    }

    /**
     * 과거 데이터에는 presigned URL 자체가 저장되어 있을 수 있어 절대 URL은 그대로 통과시킨다.
     *
     * @return 값이 없으면 null
     */
    public static String resolveViewUrl(FilePort filePort, String objectKey) {
        if (objectKey == null || objectKey.isBlank()) {
            return null;
        }
        if (objectKey.startsWith("http://") || objectKey.startsWith("https://")) {
            return objectKey;
        }
        return filePort.generatePresignedViewUrl(objectKey).presignedUrl();
    }
}
