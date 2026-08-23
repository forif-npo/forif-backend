package org.forif_backend.common.type;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class SortCriteriaTest {

    @Test
    void keepsOnlyTheFirstSortCondition() {
        List<SortCriteria> criteria = SortCriteria.parse(
                List.of("name:asc", "department:desc"),
                Set.of("name", "department")
        );

        assertThat(criteria).containsExactly(new SortCriteria("name", SortDirection.ASC));
    }
}
