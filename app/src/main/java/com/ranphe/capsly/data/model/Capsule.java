package com.ranphe.capsly.data.model;

public final class Capsule {
    private final long id;
    private final String title;
    private final String format;
    private final long openingDate;
    private final boolean opened;

    public Capsule(long id, String title, String format, long openingDate, boolean opened) {
        this.id = id;
        this.title = title;
        this.format = format;
        this.openingDate = openingDate;
        this.opened = opened;
    }

    public long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getFormat() {
        return format;
    }

    public long getOpeningDate() {
        return openingDate;
    }

    public boolean isOpened() {
        return opened;
    }
}