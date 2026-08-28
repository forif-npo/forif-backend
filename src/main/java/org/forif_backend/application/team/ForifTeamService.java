package org.forif_backend.application.team;

import lombok.RequiredArgsConstructor;
import org.forif_backend.application.file.FileViewUrls;
import org.forif_backend.application.file.TransactionalFileCleanup;
import org.forif_backend.application.file.port.out.FilePort;
import org.forif_backend.common.exception.ErrorCode;
import org.forif_backend.common.exception.ForifException;
import org.forif_backend.application.semester.SemesterService;
import org.forif_backend.application.semester.dto.SemesterInfo;
import org.forif_backend.domain.team.ForifTeam;
import org.forif_backend.domain.team.ForifTeamRepository;
import org.forif_backend.domain.user.User;
import org.forif_backend.domain.user.UserRepository;
import org.forif_backend.web.team.dto.ForifTeamResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ForifTeamService {

    private static final String PROFILE_IMAGE_DIRECTORY = "forif-team/profiles";
    private static final String FILE_CLEANUP_CONTEXT = "운영진 프로필 이미지";
    private static final long MAX_PROFILE_IMAGE_SIZE = 5 * 1024 * 1024;
    private static final Set<String> PROFILE_IMAGE_CONTENT_TYPES = Set.of(
            MediaType.IMAGE_JPEG_VALUE,
            MediaType.IMAGE_PNG_VALUE,
            "image/jpg"
    );

    private final ForifTeamRepository forifTeamRepository;
    private final UserRepository userRepository;
    private final SemesterService semesterService;
    private final FilePort filePort;

    @Transactional(readOnly = true)
    public List<ForifTeamResponse> getAllMembers() {
        return forifTeamRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ForifTeamResponse> getMembersByYearAndSemester(int actYear, int actSemester) {
        return forifTeamRepository.findByYearAndSemester(actYear, actSemester).stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * 운영진 명단에 추가 (회장단 전용 — 권한 검증은 호출부에서 수행).
     * 학기를 지정하지 않으면 현재 활동 학기에 추가한다.
     */
    @Transactional
    public ForifTeamResponse addMember(Long userId, Integer actYear, Integer actSemester,
                                       String clubDepartment, String userTitle) {
        User user = userRepository.findUserById(userId)
                .orElseThrow(() -> new ForifException(ErrorCode.USER_NOT_FOUND));

        SemesterInfo active = semesterService.getActive();
        int year = actYear != null ? actYear : active.actYear();
        int semester = actSemester != null ? actSemester : active.actSemester();

        if (forifTeamRepository.existsByActYearAndActSemesterAndUserId(year, semester, userId)) {
            throw new ForifException(ErrorCode.FORIF_TEAM_MEMBER_ALREADY_EXISTS);
        }

        ForifTeam forifTeam = ForifTeam.create(user, year, semester, clubDepartment);
        if (userTitle != null && !userTitle.isBlank()) {
            forifTeam.update(userTitle, null, null, null, null, null);
        }
        return toResponse(forifTeamRepository.save(forifTeam));
    }

    @Transactional
    public ForifTeamResponse updateMember(Long id, String userTitle, String clubDepartment,
                                          String introTag, String selfIntro, String profImgUrl, Integer graduateYear) {
        ForifTeam forifTeam = forifTeamRepository.findById(id)
                .orElseThrow(() -> new ForifException(ErrorCode.FORIF_TEAM_MEMBER_NOT_FOUND));
        String previousProfileImage = forifTeam.getProfImgUrl();
        forifTeam.update(userTitle, clubDepartment, introTag, selfIntro, profImgUrl, graduateYear);
        if (profImgUrl != null && !profImgUrl.equals(previousProfileImage)) {
            TransactionalFileCleanup.replaceAfterCompletion(
                    filePort, singletonKey(previousProfileImage), singletonKey(profImgUrl), FILE_CLEANUP_CONTEXT);
        }
        return toResponse(forifTeam);
    }

    @Transactional
    public ForifTeamResponse updateMemberProfileImage(Long id, MultipartFile profileImage) {
        validateProfileImage(profileImage);

        ForifTeam forifTeam = forifTeamRepository.findById(id)
                .orElseThrow(() -> new ForifException(ErrorCode.FORIF_TEAM_MEMBER_NOT_FOUND));
        String uploadedObjectKey = filePort.uploadFile(profileImage, PROFILE_IMAGE_DIRECTORY);
        boolean cleanupRegistered = TransactionalFileCleanup.replaceAfterCompletion(
                filePort, singletonKey(forifTeam.getProfImgUrl()), singletonKey(uploadedObjectKey), FILE_CLEANUP_CONTEXT);
        if (!cleanupRegistered) {
            TransactionalFileCleanup.deleteQuietly(filePort, singletonKey(uploadedObjectKey), FILE_CLEANUP_CONTEXT);
            throw new IllegalStateException("운영진 프로필 이미지 변경 트랜잭션이 활성화되지 않았습니다.");
        }

        forifTeam.update(null, null, null, null, uploadedObjectKey, null);
        return toResponse(forifTeam);
    }

    @Transactional
    public void deleteMember(Long id) {
        ForifTeam forifTeam = forifTeamRepository.findById(id)
                .orElseThrow(() -> new ForifException(ErrorCode.FORIF_TEAM_MEMBER_NOT_FOUND));
        forifTeamRepository.deleteById(id);
        TransactionalFileCleanup.deleteAfterCommit(filePort, forifTeam.getProfImgUrl(), FILE_CLEANUP_CONTEXT);
    }

    private void validateProfileImage(MultipartFile file) {
        if (file == null || file.isEmpty()
                || file.getSize() > MAX_PROFILE_IMAGE_SIZE
                || !PROFILE_IMAGE_CONTENT_TYPES.contains(file.getContentType())) {
            throw new ForifException(ErrorCode.INVALID_FILE_ATTACHMENT);
        }
    }

    private ForifTeamResponse toResponse(ForifTeam forifTeam) {
        return ForifTeamResponse.from(
                forifTeam,
                FileViewUrls.resolveViewUrl(filePort, forifTeam.getProfImgUrl())
        );
    }

    private static List<String> singletonKey(String objectKey) {
        return objectKey == null ? List.of() : List.of(objectKey);
    }
}
