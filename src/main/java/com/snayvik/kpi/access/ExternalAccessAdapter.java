package com.snayvik.kpi.access;

public interface ExternalAccessAdapter {

    String systemName();

    void revokeAllAccess(UserAccount userAccount) throws Exception;
}
