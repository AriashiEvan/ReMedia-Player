package com.example.remediaplayer;

public class VideoItem {

    private long id;          // REQUIRED for delete/rename
    private String title;
    private String filePath;
    private long duration;
    private long size;
    private long dateModified;

    public VideoItem(long id, String title, String filePath, long duration, long size, long dateModified) {
        this.id = id;
        this.title = title;
        this.filePath = filePath;
        this.duration = duration;
        this.size = size;
        this.dateModified = dateModified;
    }

    public long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getFilePath() {
        return filePath;
    }

    public long getDuration() {
        return duration;
    }

    public long getSize() {
        return size;
    }

    public long getDateModified() {
        return dateModified;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }
}
