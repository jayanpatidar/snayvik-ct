package com.snayvik.kpi.kpi;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DailySnapshotRepository extends JpaRepository<DailySnapshot, DailySnapshotId> {

    List<DailySnapshot> findTop30ByOrderBySnapshotDateDescPrefixAsc();

    List<DailySnapshot> findBySnapshotDateGreaterThanEqualOrderBySnapshotDateAscPrefixAsc(LocalDate fromDate);
}
