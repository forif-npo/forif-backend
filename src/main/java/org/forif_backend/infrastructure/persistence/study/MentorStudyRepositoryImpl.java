package org.forif_backend.infrastructure.persistence.study;

import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.forif_backend.domain.study.*;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class MentorStudyRepositoryImpl implements MentorStudyRepository {
    private final JPAQueryFactory queryFactory;

    private final QStudy study = QStudy.study;
    private final QStudyTag studyTag = QStudyTag.studyTag;

    @Override
    public List<Study> findStudiesByMentorId(Long mentorId) {
        return queryFactory
                .selectFrom(study).distinct()
                .leftJoin(study.tags, studyTag).fetchJoin()
                .where(
                        // 멘토 ID 조건 (주멘토 혹은 부멘토)
                        study.primaryMentor.id.eq(mentorId)
                                .or(study.secondaryMentor.id.eq(mentorId)),
                        // "내 스터디" 목록이라면 APPROVED 상태로 고정
                        study.studyStatus.eq(StudyStatus.APPROVED)
                )
                .orderBy(study.createdAt.desc())
                .fetch();
    }
}