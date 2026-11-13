package com.ranphe.capsly.data.model;

public final class UnlockedCapsule {
    private final long id;
    private final String title;
    private final String message;
    private final String format;
    private final String filePath;
    private final long openingDate;
    private final long createdAt;

    public UnlockedCapsule(long id, String title, String message, String format, String filePath, long openingDate, long createdAt) {
        this.id = id;
        this.title = title;
        this.message = message;
        this.format = format;
        this.filePath = filePath;
        this.openingDate = openingDate;
        this.createdAt = createdAt;
    }

    public long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getMessage() {
        return message;
    }

    public String getFormat() {
        return format;
    }

    public String getFilePath() {
        return filePath;
    }

    public long getOpeningDate() {
        return openingDate;
    }

    public long getCreatedAt() {
        return createdAt;
    }
}