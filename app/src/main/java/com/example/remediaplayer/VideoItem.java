package com.example.remediaplayer;

public class VideoItem {

    private long id;

    private String title;
    private String path;
    private long size;
    private long duration;
    private long dateModified;
    private String thumbnail;
    private boolean isLocal;

    private String tempNewName;
    public void setTempNewName(String name) { this.tempNewName = name; }
    public String getTempNewName() { return tempNewName; }

    private VideoItem() {}

    public static VideoItem fromLocal(
            long id,
            String title,
            String path,
            long size,
            long duration,
            long dateModifiedMs,
            String thumbnail
    ) {
        VideoItem v = new VideoItem();
        v.id = id;
        v.title = title;
        v.path = path;
        v.size = size;
        v.duration = duration;
        v.dateModified = dateModifiedMs;
        v.thumbnail = thumbnail;
        v.isLocal = true;
        return v;
    }

    public static VideoItem fromOnline(
            String title,
            String url,
            long durationMs,
            String thumbnailUrl
    ) {
        VideoItem v = new VideoItem();
        v.id = -1;
        v.title = title;
        v.path = url;
        v.size = 0;
        v.duration = durationMs;
        v.dateModified = System.currentTimeMillis();
        v.thumbnail = thumbnailUrl;
        v.isLocal = false;
        return v;
    }

    public long getId() { return id; }
    public String getTitle() { return title; }
    public String getPath() { return path; }
    public long getSize() { return size; }
    public long getDuration() { return duration; }
    public long getDateModified() { return dateModified; }
    public String getThumbnail() { return thumbnail; }
    public boolean isLocal() { return isLocal; }

    public String getDurationFormatted() {
        long sec = duration / 1000;
        return String.format("%d:%02d", sec / 60, sec % 60);
    }


}
