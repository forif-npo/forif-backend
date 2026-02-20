package org.forif_backend.infrastructure.persistence.staff;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import org.forif_backend.domain.staff.QStaffAccount;
import org.forif_backend.domain.staff.StaffAccount;
import org.forif_backend.domain.staff.StaffRole;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class StaffAccountQueryRepository {

    private final JPAQueryFactory queryFactory;
    private final QStaffAccount staffAccount = QStaffAccount.staffAccount;

    public StaffAccountQueryRepository(EntityManager em) {
        this.queryFactory = new JPAQueryFactory(em);
    }

    public List<StaffAccount> searchWithCursor(Long cursor, int size, String search) {
        return queryFactory
                .selectFrom(staffAccount)
                .join(staffAccount.user).fetchJoin()
                .where(
                        staffAccount.role.eq(StaffRole.MENTOR),
                        cursorLt(cursor),
                        searchKeyword(search)
                )
                .orderBy(staffAccount.id.desc())
                .limit(size + 1)
                .fetch();
    }

    public long count(String search) {
        Long count = queryFactory
                .select(staffAccount.count())
                .from(staffAccount)
                .where(staffAccount.role.eq(StaffRole.MENTOR), searchKeyword(search))
                .fetchOne();
        return count != null ? count : 0L;
    }

    private BooleanExpression cursorLt(Long cursor) {
        return cursor != null ? staffAccount.id.lt(cursor) : null;
    }

    private BooleanExpression searchKeyword(String search) {
        if (search == null || search.isBlank()) {
            return null;
        }
        return staffAccount.name.containsIgnoreCase(search)
                .or(staffAccount.affiliation.containsIgnoreCase(search));
    }
}
