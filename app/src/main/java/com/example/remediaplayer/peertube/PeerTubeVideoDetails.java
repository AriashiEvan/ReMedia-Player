package com.example.remediaplayer.peertube;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class PeerTubeVideoDetails {

    @SerializedName("thumbnailPath")
    private String thumbnailPath;

    @SerializedName("files")
    private List<VideoFile> files;

    public static class VideoFile {
        @SerializedName("fileUrl")
        private String fileUrl; // MP4 / WebM
        public String getFileUrl() { return fileUrl; }
    }

    @SerializedName("streamingPlaylists")
    private List<StreamingPlaylist> streamingPlaylists;

    public static class StreamingPlaylist {
        @SerializedName("playlistUrl")
        private String playlistUrl; // M3U8
        public String getPlaylistUrl() { return playlistUrl; }
    }

    public String getThumbnailPath() { return thumbnailPath; }
    public List<VideoFile> getFiles() { return files; }
    public List<StreamingPlaylist> getStreamingPlaylists() { return streamingPlaylists; }
}
