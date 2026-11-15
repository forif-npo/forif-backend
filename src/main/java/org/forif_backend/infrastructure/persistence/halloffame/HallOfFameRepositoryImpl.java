package org.forif_backend.infrastructure.persistence.halloffame;

import lombok.RequiredArgsConstructor;
import org.forif_backend.domain.halloffame.HallOfFameEntry;
import org.forif_backend.domain.halloffame.HallOfFameRepository;
import org.forif_backend.domain.halloffame.HallOfFameStatus;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class HallOfFameRepositoryImpl implements HallOfFameRepository {

    private final HallOfFameJpaRepository hallOfFameJpaRepository;

    @Override
    public HallOfFameEntry save(HallOfFameEntry entry) {
        return hallOfFameJpaRepository.save(entry);
    }

    @Override
    public void flush() {
        hallOfFameJpaRepository.flush();
    }

    @Override
    public Optional<HallOfFameEntry> findById(Integer entryId) {
        return hallOfFameJpaRepository.findById(entryId);
    }

    @Override
    public Optional<HallOfFameEntry> findDetailById(Integer entryId) {
        return hallOfFameJpaRepository.findDetailById(entryId);
    }

    @Override
    public Optional<HallOfFameEntry> findPublishedBySlug(String slug) {
        return hallOfFameJpaRepository.findPublishedBySlug(slug);
    }

    @Override
    public boolean existsBySlug(String slug) {
        return hallOfFameJpaRepository.existsBySlug(slug);
    }

    @Override
    public List<HallOfFameEntry> findAllForAdmin(HallOfFameStatus status, int offset, int limit) {
        return hallOfFameJpaRepository.findAllForAdmin(status, pageOf(offset, limit));
    }

    @Override
    public long countForAdmin(HallOfFameStatus status) {
        return hallOfFameJpaRepository.countForAdmin(status);
    }

    @Override
    public List<HallOfFameEntry> findPublished(Integer actYear, Integer actSemester, int offset, int limit) {
        return hallOfFameJpaRepository.findPublished(actYear, actSemester, pageOf(offset, limit));
    }

    @Override
    public long countPublished(Integer actYear, Integer actSemester) {
        return hallOfFameJpaRepository.countPublished(actYear, actSemester);
    }

    private PageRequest pageOf(int offset, int limit) {
        int size = Math.max(limit, 1);
        return PageRequest.of(offset / size, size);
    }
}
