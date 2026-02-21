package com.snayvik.kpi.access;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

@Entity
@Table(name = "user_skill_groups")
@IdClass(UserSkillGroupId.class)
public class UserSkillGroup {

    @Id
    @Column(name = "user_id")
    private String userId;

    @Id
    @Column(name = "skill_group_id")
    private Long skillGroupId;

    public String getUserId() {
        return userId;
    }

    public Long getSkillGroupId() {
        return skillGroupId;
    }
}
