package com.example.remediaplayer.peertube;

import com.google.gson.annotations.SerializedName;


public class PeerTubeFile {

    @SerializedName("filePath")
    private String filePath;


    @SerializedName("fileUrl")
    private String fileUrl;


    @SerializedName("resolutionLabel")
    private String resolutionLabel;

    @SerializedName("resolution")
    private Resolution resolution;

    @SerializedName("contentType")
    private String contentType;

    public String getFilePath() { return filePath; }
    public String getFileUrl() { return fileUrl; }

    public String getContentType() { return contentType; }

    public String getLabel() {
        if (resolutionLabel != null) return resolutionLabel;
        if (resolution != null) return resolution.getLabel();
        if (contentType != null) return contentType;
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
