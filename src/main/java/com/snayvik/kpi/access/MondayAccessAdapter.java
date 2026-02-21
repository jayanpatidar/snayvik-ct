package com.snayvik.kpi.access;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class MondayAccessAdapter implements ExternalAccessAdapter {

    private static final Logger logger = LoggerFactory.getLogger(MondayAccessAdapter.class);

    @Override
    public String systemName() {
        return "MONDAY";
    }

    @Override
    public void revokeAllAccess(UserAccount userAccount) {
        logger.info("Revoking monday access for user {}", userAccount.getEmail());
    }
}
