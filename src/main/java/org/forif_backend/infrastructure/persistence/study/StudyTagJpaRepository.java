package org.forif_backend.infrastructure.persistence.study;

import org.forif_backend.domain.study.StudyTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface StudyTagJpaRepository extends JpaRepository<StudyTag, Long> {
    @Query("select st from StudyTag st where lower(st.name) in :names")
    List<StudyTag> findByNameInIgnoreCase(@Param("names") List<String> names);
}
