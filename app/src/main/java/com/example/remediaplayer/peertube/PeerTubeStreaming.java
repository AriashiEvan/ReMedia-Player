package com.example.remediaplayer.peertube;

import com.google.gson.annotations.SerializedName;

public class PeerTubeStreaming {

    @SerializedName("playlistUrl")
    private String playlistUrl;

    @SerializedName("url")
    private String url;

    @SerializedName("resolutionLabel")
    private String resolutionLabel;

    @SerializedName("resolution")
    private Resolution resolution;

    @SerializedName("quality")
    private String quality;

    public String getPlaylistUrl() {
        return playlistUrl;
    }

    public String getUrl() {
        return url;
    }

    public String getQuality() {
        if (resolutionLabel != null) return resolutionLabel;
        if (quality != null) return quality;
        if (resolution != null) return resolution.getLabel();
        return "stream";
    }

    public static class Resolution {
        @SerializedName("label")
        private String label;

        public String getLabel() {
            return label;
        }
    }
}
