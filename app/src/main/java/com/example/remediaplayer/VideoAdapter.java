package com.example.remediaplayer;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class VideoAdapter extends RecyclerView.Adapter<VideoAdapter.Holder> {

    Context ctx;
    ArrayList<VideoItem> videos;

    public VideoAdapter(Context ctx, ArrayList<VideoItem> videos) {
        this.ctx = ctx;
        this.videos = videos;
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(LayoutInflater.from(ctx).inflate(R.layout.video_item, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder h, int pos) {

        VideoItem v = videos.get(pos);

        h.videoName.setText(v.getTitle());
        h.videoSize.setText(formatSize(v.getSize()));
        h.videoDate.setText("Modified: " + formatDate(v.getDateModified()));
        h.videoDuration.setText(formatDuration(v.getDuration()));

        Glide.with(ctx)
                .load(Uri.fromFile(new File(v.getFilePath())))
                .into(h.thumbnail);

        h.itemView.setOnClickListener(view -> {
            Intent i = new Intent(ctx, VideoPlayerView.class);
            i.putExtra("video_path", v.getFilePath());
            ctx.startActivity(i);
        });
    }

    @Override
    public int getItemCount() {
        return videos.size();
    }

    class Holder extends RecyclerView.ViewHolder {
        ImageView thumbnail;
        TextView videoName, videoSize, videoDate, videoDuration;
        ImageButton btnMoreOptions;

        public Holder(@NonNull View v) {
            super(v);

            thumbnail = v.findViewById(R.id.videoThumbnail);
            videoName = v.findViewById(R.id.videoName);
            videoSize = v.findViewById(R.id.videoSize);
            videoDate = v.findViewById(R.id.videoDate);
            videoDuration = v.findViewById(R.id.videoDuration);
            btnMoreOptions = v.findViewById(R.id.btnMoreOptions);
        }
    }

    private String formatDuration(long ms) {
        long sec = ms / 1000;
        long min = sec / 60;
        long rem = sec % 60;
        return String.format("%02d:%02d", min, rem);
    }

    private String formatSize(long sizeBytes) {
        return (sizeBytes / (1024 * 1024)) + " MB";
    }

    private String formatDate(long unix) {
        return new SimpleDateFormat("dd MMM yyyy").format(new Date(unix * 1000));
    }
}
