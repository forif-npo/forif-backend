package org.forif_backend.application.department;

import lombok.RequiredArgsConstructor;
import org.forif_backend.application.department.dto.DepartmentInfo;
import org.forif_backend.common.exception.ErrorCode;
import org.forif_backend.common.exception.ForifException;
import org.forif_backend.domain.department.Department;
import org.forif_backend.domain.department.DepartmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DepartmentService {
    private final DepartmentRepository departmentRepository;

    public List<DepartmentInfo> getAll() {
        return departmentRepository.findAll().stream().map(DepartmentInfo::from).toList();
    }

    public Department getRequired(Long departmentId) {
        if (departmentId == null) {
            throw new ForifException(ErrorCode.INVALID_INPUT);
        }
        return departmentRepository.findById(departmentId)
                .orElseThrow(() -> new ForifException(ErrorCode.DEPARTMENT_NOT_FOUND));
    }

    /**
     * 기존 웹 클라이언트가 보내는 학과명은 배포 전환 기간에만 허용한다.
     * 신규 클라이언트는 반드시 departmentId를 전송한다.
     */
    public Department getRequired(Long departmentId, String legacyDepartmentName) {
        if (departmentId != null) {
            return getRequired(departmentId);
        }
        if (legacyDepartmentName == null || legacyDepartmentName.isBlank()) {
            throw new ForifException(ErrorCode.INVALID_INPUT);
        }
        return departmentRepository.findByDepartmentName(legacyDepartmentName.trim())
                .orElseThrow(() -> new ForifException(ErrorCode.DEPARTMENT_NOT_FOUND));
    }
}
