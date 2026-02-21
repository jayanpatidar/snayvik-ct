package com.snayvik.kpi.policy;

import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PolicyViolationRepository extends JpaRepository<PolicyViolation, Long> {

    boolean existsByTaskKeyAndTypeAndResolvedFalse(String taskKey, String type);

    List<PolicyViolation> findByResolvedOrderByCreatedAtDesc(boolean resolved);

    @Query("""
            select coalesce(v.userId, 'UNASSIGNED') as userId, count(v) as total
            from PolicyViolation v
            where v.type = :type and v.createdAt >= :since and v.resolved = false
            group by coalesce(v.userId, 'UNASSIGNED')
            """)
    List<UserViolationCount> countByTypeGroupedByUserSince(@Param("type") String type, @Param("since") Instant since);

    interface UserViolationCount {
        String getUserId();

        Long getTotal();
    }
}
