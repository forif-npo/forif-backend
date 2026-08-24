package org.forif_backend.application.file;

import lombok.extern.slf4j.Slf4j;
import org.forif_backend.application.file.port.out.FilePort;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Collection;
import java.util.List;

/**
 * 트랜잭션 결과에 맞춰 저장소 파일을 정리하는 규칙.
 * DB 반영과 파일 삭제는 원자적일 수 없으므로, 커밋이 확정된 뒤에만 지우고
 * 롤백 시에는 방금 올린 파일을 회수해 고아 파일이 남지 않게 한다.
 *
 * <p>트랜잭션이 없을 때의 처리는 호출부마다 달라야 해서 메서드로 구분한다.
 * FilePort의 default 메서드나 주입 컴포넌트로 두지 않는 이유는 {@link FileViewUrls}와 같다.
 */
@Slf4j
public final class TransactionalFileCleanup {

    private TransactionalFileCleanup() {
    }

    /**
     * 커밋되면 교체 전 파일을, 롤백되면 새로 올린 파일을 지운다.
     *
     * @return 트랜잭션이 없어 아무것도 등록하지 못했으면 false. 호출부가 자체 폴백을 정한다.
     */
    public static boolean replaceAfterCompletion(FilePort filePort, Collection<String> previousKeys,
                                                 Collection<String> uploadedKeys, String context) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return false;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                deleteQuietly(filePort, status == STATUS_COMMITTED ? previousKeys : uploadedKeys, context);
            }
        });
        return true;
    }

    /** 커밋된 뒤에만 지운다. 트랜잭션이 없으면 즉시 지운다. */
    public static void deleteAfterCommit(FilePort filePort, Collection<String> objectKeys, String context) {
        if (objectKeys == null || objectKeys.isEmpty()) {
            return;
        }
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            deleteQuietly(filePort, objectKeys, context);
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                deleteQuietly(filePort, objectKeys, context);
            }
        });
    }

    /** 커밋된 뒤에만 지운다. 트랜잭션이 없으면 즉시 지운다. */
    public static void deleteAfterCommit(FilePort filePort, String objectKey, String context) {
        deleteAfterCommit(filePort, List.of(objectKey == null ? "" : objectKey), context);
    }

    /** 롤백된 경우에만 회수한다. 트랜잭션이 없으면 아무것도 하지 않는다. */
    public static void deleteOnRollback(FilePort filePort, String objectKey, String context) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED) {
                    deleteQuietly(filePort, List.of(objectKey == null ? "" : objectKey), context);
                }
            }
        });
    }

    /**
     * 삭제 실패가 이미 성공한 요청을 되돌리지 않도록 예외를 삼킨다.
     * 값이 없거나 절대 URL(과거 데이터)인 키는 저장소 대상이 아니므로 건너뛴다.
     */
    public static void deleteQuietly(FilePort filePort, Collection<String> objectKeys, String context) {
        if (objectKeys == null) {
            return;
        }
        for (String objectKey : objectKeys) {
            if (objectKey == null || objectKey.isBlank()
                    || objectKey.startsWith("http://") || objectKey.startsWith("https://")) {
                continue;
            }
            try {
                filePort.deleteFile(objectKey);
            } catch (Exception e) {
                log.warn("{} 파일 삭제 실패: {}", context, objectKey, e);
            }
        }
    }
}
