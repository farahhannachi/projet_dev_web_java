package org.example.model;

import java.util.List;

public class ResponseQuestionPage {
    private final List<ResponseQuestion> items;
    private final int totalCount;

    public ResponseQuestionPage(List<ResponseQuestion> items, int totalCount) {
        this.items = items;
        this.totalCount = totalCount;
    }

    public List<ResponseQuestion> getItems() {
        return items;
    }

    public int getTotalCount() {
        return totalCount;
    }
}

