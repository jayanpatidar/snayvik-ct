package com.snayvik.kpi.access;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserAccountRepository extends JpaRepository<UserAccount, String> {

    List<UserAccount> findByActiveTrueOrderByNameAsc();
}
