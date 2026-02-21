package com.snayvik.kpi.sync;

import java.util.List;

public interface MondaySyncClient {

    List<MondayTaskSnapshot> fetchBoardItems(String boardId);
}
