package org.forif_backend.infrastructure.persistence.staff;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import org.forif_backend.domain.staff.QStaffAccount;
import org.forif_backend.domain.staff.StaffAccount;
import org.forif_backend.domain.staff.StaffRole;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Repository
public class StaffAccountQueryRepository {

    private final JPAQueryFactory queryFactory;
    private final QStaffAccount staffAccount = QStaffAccount.staffAccount;

    public StaffAccountQueryRepository(EntityManager em) {
        this.queryFactory = new JPAQueryFactory(em);
    }

    // ==================== 운영진(ADMIN) ====================

    public List<StaffAccount> searchAdminsWithCursor(Integer cursor, int size, String search) {
        return queryFactory
                .selectFrom(staffAccount)
                .join(staffAccount.user).fetchJoin()
                .where(
                        staffAccount.role.eq(StaffRole.ADMIN),
                        cursorLt(cursor),
                        searchKeyword(search)
                )
                .orderBy(staffAccount.user.id.desc())
                .limit(size + 1)
                .fetch();
    }

    public long countAdmins(String search) {
        Long count = queryFactory
                .select(staffAccount.count())
                .from(staffAccount)
                .where(
                        staffAccount.role.eq(StaffRole.ADMIN),
                        searchKeyword(search)
                )
                .fetchOne();
        return count != null ? count : 0L;
    }

    public List<StaffAccount> findByAffiliation(String affiliation) {
        return queryFactory
                .selectFrom(staffAccount)
                .where(
                        staffAccount.role.eq(StaffRole.ADMIN),
                        staffAccount.affiliation.eq(affiliation)
                )
                .fetch();
    }

    public List<StaffAccount> searchAdminsWithOffset(int page, int size, String search) {
        return queryFactory
                .selectFrom(staffAccount)
                .join(staffAccount.user).fetchJoin()
                .where(
                        staffAccount.role.eq(StaffRole.ADMIN),
                        searchKeyword(search)
                )
                .orderBy(staffAccount.user.id.desc())
                .offset((long) page * size)
                .limit(size)
                .fetch();
    }

    // ==================== 배치 조회 ====================

    public Map<Long, StaffRole> findStaffRolesByUserIds(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }

        List<StaffAccount> staffAccounts = queryFactory
                .selectFrom(staffAccount)
                .where(
                        staffAccount.user.id.in(userIds),
                        staffAccount.role.eq(StaffRole.ADMIN)
                )
                .fetch();

        return staffAccounts.stream()
                .collect(Collectors.toMap(
                        StaffAccount::getUserId,
                        StaffAccount::getRole
                ));
    }

    // ==================== 공통 ====================

    private BooleanExpression cursorLt(Integer cursor) {
        if (cursor == null) {
            return null;
        }
        return staffAccount.user.id.lt(cursor.longValue());
    }

    private BooleanExpression searchKeyword(String search) {
        if (search == null || search.isBlank()) {
            return null;
        }
        return staffAccount.name.containsIgnoreCase(search)
                .or(staffAccount.affiliation.containsIgnoreCase(search));
    }
}
