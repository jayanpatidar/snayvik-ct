package com.snayvik.kpi.kpi;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;

public class DailySnapshotId implements Serializable {

    private LocalDate snapshotDate;
    private String prefix;

    public DailySnapshotId() {
    }

    public DailySnapshotId(LocalDate snapshotDate, String prefix) {
        this.snapshotDate = snapshotDate;
        this.prefix = prefix;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof DailySnapshotId other)) {
            return false;
        }
        return Objects.equals(snapshotDate, other.snapshotDate) && Objects.equals(prefix, other.prefix);
    }

    @Override
    public int hashCode() {
        return Objects.hash(snapshotDate, prefix);
    }
}
