package org.forif_backend.application.team;

import lombok.RequiredArgsConstructor;
import org.forif_backend.common.exception.ErrorCode;
import org.forif_backend.common.exception.ForifException;
import org.forif_backend.domain.team.ClubDepartment;
import org.forif_backend.domain.team.ClubDepartmentRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ClubDepartmentService {

    private final ClubDepartmentRepository repository;

    public ClubDepartment getOrCreate(String rawName) {
        String name = normalizeName(rawName);
        if (name == null || name.isBlank() || name.length() > 50) {
            throw new ForifException(ErrorCode.INVALID_INPUT);
        }
        return repository.findByName(name).orElseGet(() -> {
            try {
                return repository.save(ClubDepartment.create(name));
            } catch (DataIntegrityViolationException e) {
                // 같은 이름의 동시 생성은 고유 제약이 막는다. 요청을 재시도하면 기존 ID를 사용한다.
                throw new ForifException(ErrorCode.DATA_INTEGRITY_VIOLATION);
            }
        });
    }

    public static String normalizeName(String rawName) {
        if (rawName == null) return null;
        String name = rawName.trim();
        if (name.length() > 1 && name.endsWith("팀")) {
            name = name.substring(0, name.length() - 1).trim();
        }
        return name;
    }
}
