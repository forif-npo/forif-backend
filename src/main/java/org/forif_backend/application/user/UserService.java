package org.forif_backend.application.user;

import lombok.RequiredArgsConstructor;
import org.forif_backend.application.study.StudyService;
import org.forif_backend.common.exception.ErrorCode;
import org.forif_backend.common.exception.ForifException;
import org.forif_backend.common.util.DateUtils;
import org.forif_backend.domain.study.StudyRepository;
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
    private final StudyRepository studyRepository;

}
