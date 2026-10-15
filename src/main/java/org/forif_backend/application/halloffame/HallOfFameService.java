package org.forif_backend.application.halloffame;

import lombok.RequiredArgsConstructor;
import org.forif_backend.application.file.FileViewUrls;
import org.forif_backend.application.file.TransactionalFileCleanup;
import org.forif_backend.application.file.port.out.FilePort;
import org.forif_backend.application.halloffame.dto.HallOfFameCommands.AchievementCommand;
import org.forif_backend.application.halloffame.dto.HallOfFameCommands.CreateCommand;
import org.forif_backend.application.halloffame.dto.HallOfFameCommands.UpdateCommand;
import org.forif_backend.application.halloffame.dto.HallOfFameEntryDetail;
import org.forif_backend.common.dto.response.CursorPageResponse;
import org.forif_backend.common.exception.ErrorCode;
import org.forif_backend.common.exception.ForifException;
import org.forif_backend.domain.halloffame.HallOfFameAchievement;
import org.forif_backend.domain.halloffame.HallOfFameEntry;
import org.forif_backend.domain.halloffame.HallOfFameRepository;
import org.forif_backend.domain.halloffame.HallOfFameStatus;
import org.forif_backend.domain.user.User;
import org.forif_backend.domain.user.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * 명예의 전당 전시 콘텐츠 관리와 공개 조회.
 *
 * 관리자가 직접 등록·게시하며 자동 점수·랭킹은 만들지 않는다.
 * 공개 조회는 게시된 항목만 반환하고, 초안·게시 중단은 ID나 slug를 알아도 보이지 않는다.
 */
@Service
@RequiredArgsConstructor
public class HallOfFameService {

    private static final String IMAGE_DIRECTORY = "hall-of-fame/images";
    private static final long MAX_IMAGE_SIZE = 5 * 1024 * 1024; // 5MB
    private static final String FILE_CLEANUP_CONTEXT = "명예의 전당 이미지";
    private static final String HTTPS_PREFIX = "https://";

    private final HallOfFameRepository hallOfFameRepository;
    private final UserRepository userRepository;
    private final FilePort filePort;

    // ==================== 관리 ====================

    @Transactional
    public HallOfFameEntryDetail create(CreateCommand command) {
        requireSlugAvailable(command.slug(), null);

        User member = findMember(command.memberUserId());
        HallOfFameEntry entry = HallOfFameEntry.create(
                member,
                resolveMemberName(command.memberName(), member),
                command.actYear(),
                command.actSemester(),
                command.slug(),
                command.headline(),
                command.introduction(),
                command.displayOrder());
        applyBadges(entry, command.badges());
        applyAchievements(entry, command.achievements());

        HallOfFameEntry saved = saveOrSlugConflict(entry);
        return detail(saved);
    }

    @Transactional
    public HallOfFameEntryDetail update(Integer entryId, UpdateCommand command) {
        HallOfFameEntry entry = findEntry(entryId);
        requireVersion(entry, command.version());
        requireSlugAvailable(command.slug(), entry.getSlug());

        User member = findMember(command.memberUserId());
        entry.updateContent(
                resolveMemberName(command.memberName(), member),
                command.actYear(),
                command.actSemester(),
                command.slug(),
                command.headline(),
                command.introduction(),
                command.displayOrder());
        applyBadges(entry, command.badges());
        applyAchievements(entry, command.achievements());

        saveOrSlugConflict(entry);
        return detail(entry);
    }

    @Transactional
    public HallOfFameEntryDetail publish(Integer entryId, Long version) {
        HallOfFameEntry entry = findEntry(entryId);
        requireVersion(entry, version);
        entry.publish(LocalDateTime.now());
        return detail(entry);
    }

    @Transactional
    public HallOfFameEntryDetail unpublish(Integer entryId, Long version) {
        HallOfFameEntry entry = findEntry(entryId);
        requireVersion(entry, version);
        entry.unpublish();
        return detail(entry);
    }

    /** 공개 전시 이미지. 기존 파일 저장 모듈을 그대로 쓰고 교체 시 이전 파일을 정리한다. */
    @Transactional
    public String uploadImage(Integer entryId, MultipartFile file) {
        HallOfFameEntry entry = findEntry(entryId);
        validateImageFile(file);

        String previousObjectKey = entry.getImageObjectKey();
        String objectKey = filePort.uploadFile(file, IMAGE_DIRECTORY);
        entry.updateImage(objectKey);
        TransactionalFileCleanup.deleteOnRollback(filePort, objectKey, FILE_CLEANUP_CONTEXT);
        if (!Objects.equals(previousObjectKey, objectKey)) {
            TransactionalFileCleanup.deleteAfterCommit(filePort, previousObjectKey, FILE_CLEANUP_CONTEXT);
        }

        return FileViewUrls.resolveViewUrl(filePort, objectKey);
    }

    @Transactional(readOnly = true)
    public HallOfFameEntryDetail getForAdmin(Integer entryId) {
        return detail(findDetailEntry(entryId));
    }

    @Transactional(readOnly = true)
    public CursorPageResponse<HallOfFameEntryDetail> getAdminList(HallOfFameStatus status, int page, int size) {
        long totalElements = hallOfFameRepository.countForAdmin(status);
        List<HallOfFameEntryDetail> content = hallOfFameRepository.findAllForAdmin(status, page * size, size).stream()
                .map(this::detail)
                .toList();
        boolean hasNext = (long) (page + 1) * size < totalElements;
        return CursorPageResponse.ofOffset(content, hasNext, totalElements, page, size);
    }

    // ==================== 공개 조회 ====================

    @Transactional(readOnly = true)
    public CursorPageResponse<HallOfFameEntryDetail> getPublished(Integer actYear, Integer actSemester,
                                                                  int page, int size) {
        long totalElements = hallOfFameRepository.countPublished(actYear, actSemester);
        List<HallOfFameEntryDetail> content = hallOfFameRepository
                .findPublished(actYear, actSemester, page * size, size).stream()
                .map(this::detail)
                .toList();
        boolean hasNext = (long) (page + 1) * size < totalElements;
        return CursorPageResponse.ofOffset(content, hasNext, totalElements, page, size);
    }

    /** 게시된 항목만 조회한다. 초안·게시 중단은 slug를 알아도 404다. */
    @Transactional(readOnly = true)
    public HallOfFameEntryDetail getPublishedBySlug(String slug) {
        return hallOfFameRepository.findPublishedBySlug(slug)
                .map(this::detail)
                .orElseThrow(() -> new ForifException(ErrorCode.HALL_OF_FAME_ENTRY_NOT_FOUND));
    }

    // ==================== 내부 ====================

    private HallOfFameEntry findEntry(Integer entryId) {
        return hallOfFameRepository.findDetailById(entryId)
                .orElseThrow(() -> new ForifException(ErrorCode.HALL_OF_FAME_ENTRY_NOT_FOUND));
    }

    private HallOfFameEntry findDetailEntry(Integer entryId) {
        return findEntry(entryId);
    }

    private HallOfFameEntryDetail detail(HallOfFameEntry entry) {
        return HallOfFameEntryDetail.from(entry, FileViewUrls.resolveViewUrl(filePort, entry.getImageObjectKey()));
    }

    private User findMember(Long memberUserId) {
        if (memberUserId == null) {
            return null;
        }
        return userRepository.findById(memberUserId)
                .orElseThrow(() -> new ForifException(ErrorCode.USER_NOT_FOUND));
    }

    /** 표시 이름은 입력값을 우선하고, 비어 있으면 연결된 회원 이름을 복사한다. */
    private String resolveMemberName(String memberName, User member) {
        if (memberName != null && !memberName.isBlank()) {
            return memberName;
        }
        if (member != null) {
            return member.getUserName();
        }
        throw new ForifException(ErrorCode.INVALID_INPUT);
    }

    private void requireSlugAvailable(String slug, String currentSlug) {
        if (slug != null && slug.equals(currentSlug)) {
            return;
        }
        if (hallOfFameRepository.existsBySlug(slug)) {
            throw new ForifException(ErrorCode.HALL_OF_FAME_SLUG_ALREADY_EXISTS);
        }
    }

    private void requireVersion(HallOfFameEntry entry, Long expectedVersion) {
        if (expectedVersion != null && !expectedVersion.equals(entry.getVersion())) {
            throw new ForifException(ErrorCode.DATA_INTEGRITY_VIOLATION);
        }
    }

    private HallOfFameEntry saveOrSlugConflict(HallOfFameEntry entry) {
        HallOfFameEntry saved = hallOfFameRepository.save(entry);
        try {
            hallOfFameRepository.flush();
        } catch (DataIntegrityViolationException e) {
            // slug 중복 검사와 저장 사이의 경합 — UNIQUE 제약이 최종 방어선
            throw new ForifException(ErrorCode.HALL_OF_FAME_SLUG_ALREADY_EXISTS);
        }
        return saved;
    }

    private void applyBadges(HallOfFameEntry entry, List<String> badges) {
        entry.replaceBadges(badges == null ? List.of() : badges);
    }

    private void applyAchievements(HallOfFameEntry entry, List<AchievementCommand> achievements) {
        List<AchievementCommand> source = achievements == null ? List.of() : achievements;
        source.forEach(achievement -> {
            if (achievement.title() == null || achievement.title().isBlank()) {
                throw new ForifException(ErrorCode.INVALID_INPUT);
            }
            // 외부 링크는 https만 허용한다.
            if (achievement.linkUrl() != null && !achievement.linkUrl().isBlank()
                    && !achievement.linkUrl().startsWith(HTTPS_PREFIX)) {
                throw new ForifException(ErrorCode.INVALID_INPUT);
            }
        });
        entry.replaceAchievements(source.stream()
                .map(achievement -> HallOfFameAchievement.create(
                        achievement.title(),
                        achievement.description(),
                        achievement.linkUrl(),
                        achievement.displayOrder()))
                .toList());
    }

    private void validateImageFile(MultipartFile file) {
        if (file == null || file.isEmpty() || file.getSize() > MAX_IMAGE_SIZE) {
            throw new ForifException(ErrorCode.INVALID_FILE_ATTACHMENT);
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new ForifException(ErrorCode.INVALID_FILE_ATTACHMENT);
        }
    }
}
