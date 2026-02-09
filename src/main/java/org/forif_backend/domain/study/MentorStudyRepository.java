package org.forif_backend.domain.study;

import java.util.List;

public interface MentorStudyRepository {
    /**
     * 멘토 아이디로 스터디와 태그들을 함께 조회한다.
     *
     * @param mentorId 멘토 아이디
     * @return 스터디 리스트
     */
    List<Study> findStudiesByMentorId(Long mentorId);
}
