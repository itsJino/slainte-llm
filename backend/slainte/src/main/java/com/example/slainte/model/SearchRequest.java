package com.example.slainte.model;

public class SearchRequest {
    private String query;
    private int topK;

    // ✅ Default constructor (Needed for Spring Boot JSON conversion)
    public SearchRequest() {}

    // ✅ Constructor with parameters
    public SearchRequest(String query, int topK) {
        this.query = query;
        this.topK = topK;
    }

    // ✅ Getter & Setter Methods
    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public int getTopK() {
        return topK;
    }

    public void setTopK(int topK) {
        this.topK = topK;
    }
}
