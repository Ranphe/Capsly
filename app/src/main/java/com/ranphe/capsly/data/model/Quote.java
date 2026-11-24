package com.ranphe.capsly.data.model;

public final class Quote {
    private final long id;
    private final String text;

    public Quote(long id, String text) {
        this.id = id;
        this.text = text;
    }

    public long getId() {
        return id;
    }

    public String getText() {
        return text;
    }
}