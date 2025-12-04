package com.example.remediaplayer.peertube;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class PeerTubeVideoDetails {

    @SerializedName("files")
    private List<PeerTubeFile> files;

    @SerializedName("streamingPlaylists")
    private List<PeerTubeStreaming> streamingPlaylists;

    public List<PeerTubeFile> getFiles() {
        return files;
    }

    public List<PeerTubeStreaming> getStreamingPlaylists() {
        return streamingPlaylists;
    }

    public static class PeerTubeFile {

        @SerializedName("fileUrl")
        private String fileUrl;

        @SerializedName("size")
        private long size;

        @SerializedName("bitrate")
        private long bitrate;

        @SerializedName("resolution")
        private String resolution;

        @SerializedName("mimeType")
        private String mimeType;

        @SerializedName("quality")
        private String quality;

        public String getAnyUrl() {
            return fileUrl;
        }

        public String getMimeType() {
            return mimeType;
        }

        public long getSize() {
            return size;
        }

        public long getBitrate() {
            return bitrate;
        }

        public String getQuality() {
            if (quality != null) return quality;
            if (resolution != null) return resolution;
            return "unknown";
        }
    }
}
