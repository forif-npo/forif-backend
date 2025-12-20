package org.forif_backend.infrastructure.persistence.user;

import lombok.RequiredArgsConstructor;
import org.forif_backend.domain.user.UserApply;
import org.forif_backend.domain.user.UserApplyRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class UserApplyRepositoryImpl implements UserApplyRepository {
    private final UserApplyJpaRepository userApplyJpaRepository;

    @Override
    public List<UserApply> findAllUserApplyByUserId(Long userId) {
        return userApplyJpaRepository.findByApplierId(userId);
    }
}