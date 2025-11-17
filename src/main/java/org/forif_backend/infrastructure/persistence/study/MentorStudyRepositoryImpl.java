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

    private final QMentorStudy mentorStudy = QMentorStudy.mentorStudy;
    private final QStudy study = QStudy.study;
    private final QStudyTag studyTag = QStudyTag.studyTag;

    @Override
    public List<Study> findStudiesWithTagsByMentorId(Long mentorId) {
        return queryFactory
                .selectFrom(study)
                .distinct()
                .join(mentorStudy).on(mentorStudy.study.eq(study))
                .leftJoin(study.tags, studyTag).fetchJoin()
                .where(mentorStudy.mentor.id.eq(mentorId))
                .fetch();
    }
}