package com.snayvik.kpi.sync;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class NoOpMondaySyncClient implements MondaySyncClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(NoOpMondaySyncClient.class);

    @Override
    public List<MondayTaskSnapshot> fetchBoardItems(String boardId) {
        LOGGER.debug("NoOpMondaySyncClient active; skipping monday full sync for board {}", boardId);
        return List.of();
    }
}
