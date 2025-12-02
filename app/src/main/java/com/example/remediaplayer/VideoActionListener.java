package com.example.remediaplayer;

public interface VideoActionListener {

    void onPlay(VideoItem item);
    void onShare(VideoItem item);
    void onRenameRequest(VideoItem item);
    void onDeleteRequest(VideoItem item);

}
