package com.example.remediaplayer;

public class VideoItem {
    private String filePath;
    private String title;
    private long duration;
    private long size;
    private long dateModified;

    public VideoItem(String filePath, String title, long duration, long size, long dateModified) {
        this.filePath = filePath;
        this.title = title;
        this.duration = duration;
        this.size = size;
        this.dateModified = dateModified;
    }

    public String getFilePath() { return filePath; }
    public String getTitle() { return title; }
    public long getDuration() { return duration; }
    public long getSize() { return size; }
    public long getDateModified() { return dateModified; }
}
