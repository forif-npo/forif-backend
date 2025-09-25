package org.forif_backend.application.user;

import lombok.RequiredArgsConstructor;
import org.forif_backend.application.study.StudyService;
import org.forif_backend.common.exception.ErrorCode;
import org.forif_backend.common.exception.ForifException;
import org.forif_backend.common.util.DateUtils;
import org.forif_backend.domain.user.User;
import org.forif_backend.domain.user.UserApply;
import org.forif_backend.domain.user.UserRepository;
import org.forif_backend.web.user.dto.StudyApplyRequest;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final StudyService studyService;

    /**
     * 스터디 지원 메서드
     * @param user
     * @param request
     */
    public void applyStudy(User user, StudyApplyRequest request) {
        // 이번 학기에 지원한 스터디 있는지 확인
        if(userRepository.existUserApply(DateUtils.getCurrentYear(), DateUtils.getCurrentSemester())) {
            throw new ForifException(ErrorCode.USER_APPLY_ALREADY_EXISTS);
        }

        // 지원 스터디 존재 확인
        studyService.getStudy(request.primaryStudyId());
        Optional.ofNullable(request.secondaryStudyId()).ifPresent(studyService::getStudy);

        // 지원 정보 생성
        UserApply userApply = UserApply.applyStudy(request, user);

        // 지원
        userRepository.createUserApply(userApply);
    }
}
