package com.ranphe.capsly.data.model;

public final class Capsule {
    private final long id;
    private final String format;
    private final long openingDate;

    public Capsule(long id, String format, long openingDate) {
        this.id = id;
        this.format = format;
        this.openingDate = openingDate;
    }

    public long getId() {
        return id;
    }

    public String getFormat() {
        return format;
    }

    public long getOpeningDate() {
        return openingDate;
    }
}