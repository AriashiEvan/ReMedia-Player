package com.example.remediaplayer.peertube;

import com.google.gson.annotations.SerializedName;

public class PeerTubeVideo {

    @SerializedName("name")
    private String name;

    @SerializedName("url")
    private String videoUrl;

    @SerializedName("duration")
    private long duration;

    @SerializedName("thumbnailPath")
    private String thumbnailPath;

    public String getName() {
        return name;
    }

    public String getVideoUrl() {
        return videoUrl;
    }

    public long getDuration() {
        return duration;
    }

    public String getThumbnailPath() {
        return thumbnailPath;
    }

    @SerializedName("uuid")
    private String uuid;

    public String getUuid() {
        return uuid;
    }
}
