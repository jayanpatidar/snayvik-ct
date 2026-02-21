package com.snayvik.kpi.access;

import java.io.Serializable;
import java.util.Objects;

public class UserSkillGroupId implements Serializable {

    private String userId;
    private Long skillGroupId;

    public UserSkillGroupId() {
    }

    public UserSkillGroupId(String userId, Long skillGroupId) {
        this.userId = userId;
        this.skillGroupId = skillGroupId;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof UserSkillGroupId other)) {
            return false;
        }
        return Objects.equals(userId, other.userId) && Objects.equals(skillGroupId, other.skillGroupId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, skillGroupId);
    }
}
