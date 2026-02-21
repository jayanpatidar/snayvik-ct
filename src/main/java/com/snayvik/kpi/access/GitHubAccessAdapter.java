package com.snayvik.kpi.access;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class GitHubAccessAdapter implements ExternalAccessAdapter {

    private static final Logger logger = LoggerFactory.getLogger(GitHubAccessAdapter.class);

    @Override
    public String systemName() {
        return "GITHUB";
    }

    @Override
    public void revokeAllAccess(UserAccount userAccount) {
        logger.info("Revoking GitHub access for user {}", userAccount.getEmail());
    }
}
