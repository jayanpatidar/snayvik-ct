package com.snayvik.kpi.access;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserSkillGroupRepository extends JpaRepository<UserSkillGroup, UserSkillGroupId> {

    @Query("""
            select count(distinct usg.userId)
            from UserSkillGroup usg
            join SkillGroup sg on sg.id = usg.skillGroupId
            join UserAccount u on u.id = usg.userId
            where lower(sg.name) = 'admin' and u.active = true
            """)
    long countActiveAdmins();

    @Query("""
            select count(usg)
            from UserSkillGroup usg
            join SkillGroup sg on sg.id = usg.skillGroupId
            where usg.userId = :userId and lower(sg.name) = 'admin'
            """)
    long countAdminMembership(@Param("userId") String userId);

    List<UserSkillGroup> findByUserId(String userId);
}
