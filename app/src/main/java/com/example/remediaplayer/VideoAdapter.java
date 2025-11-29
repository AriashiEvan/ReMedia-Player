package com.example.remediaplayer;

import android.app.AlertDialog;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class VideoAdapter extends RecyclerView.Adapter<VideoAdapter.Holder> {

    Context ctx;
    ArrayList<VideoItem> videos;
    ArrayList<VideoItem> filteredList;

    public VideoAdapter(Context ctx, ArrayList<VideoItem> videos) {
        this.ctx = ctx;
        this.videos = videos;
        this.filteredList = new ArrayList<>(videos);
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(LayoutInflater.from(ctx).inflate(R.layout.video_item, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder h, int pos) {

        VideoItem v = filteredList.get(pos);

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

        h.btnMoreOptions.setOnClickListener(view -> showMoreOptions(v));
    }

    @Override
    public int getItemCount() {
        return filteredList.size();
    }

    // ------------------------------------------------------------
    // MORE OPTIONS DIALOG
    // ------------------------------------------------------------
    private void showMoreOptions(VideoItem v) {

        String[] options = {"Play", "Share", "Delete", "Rename", "Details"};

        AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
        builder.setTitle(v.getTitle());
        builder.setItems(options, (dialog, which) -> {

            switch (which) {
                case 0: playVideo(v); break;
                case 1: shareVideo(v); break;
                case 2: deleteVideo(v); break;
                case 3: renameVideo(v); break;
                case 4: showDetails(v); break;
            }

        });

        builder.show();
    }

    // ------------------------------------------------------------
    // PLAY VIDEO
    // ------------------------------------------------------------
    private void playVideo(VideoItem v) {
        Intent i = new Intent(ctx, VideoPlayerView.class);
        i.putExtra("video_path", v.getFilePath());
        ctx.startActivity(i);
    }

    // ------------------------------------------------------------
    // SHARE VIDEO
    // ------------------------------------------------------------
    private void shareVideo(VideoItem v) {
        try {
            File file = new File(v.getFilePath());
            Uri uri = FileProvider.getUriForFile(ctx, ctx.getPackageName() + ".provider", file);

            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("video/*");
            intent.putExtra(Intent.EXTRA_STREAM, uri);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            ctx.startActivity(Intent.createChooser(intent, "Share Video"));

        } catch (Exception e) {
            Toast.makeText(ctx, "Share failed", Toast.LENGTH_SHORT).show();
        }
    }

    // ------------------------------------------------------------
    // DELETE VIDEO (MediaStore safe)
    // ------------------------------------------------------------
    private void deleteVideo(VideoItem v) {

        Uri videoUri = ContentUris.withAppendedId(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                v.getId()
        );

        AlertDialog.Builder confirm = new AlertDialog.Builder(ctx);
        confirm.setTitle("Delete Video?");
        confirm.setMessage("This action cannot be undone.");

        confirm.setPositiveButton("Delete", (dialog, which) -> {

            int rows = ctx.getContentResolver().delete(videoUri, null, null);

            if (rows > 0) {
                videos.remove(v);
                filteredList.remove(v);
                notifyDataSetChanged();
                Toast.makeText(ctx, "Deleted", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(ctx, "Failed to delete", Toast.LENGTH_SHORT).show();
            }
        });

        confirm.setNegativeButton("Cancel", null);
        confirm.show();
    }

    // ------------------------------------------------------------
    // RENAME VIDEO (MediaStore safe)
    // ------------------------------------------------------------
    private void renameVideo(VideoItem v) {

        AlertDialog.Builder dialog = new AlertDialog.Builder(ctx);
        dialog.setTitle("Rename Video");

        View view = LayoutInflater.from(ctx).inflate(R.layout.dialog_rename, null);
        TextView input = view.findViewById(R.id.rename_input);
        input.setText(v.getTitle());
        dialog.setView(view);

        dialog.setPositiveButton("Rename", (d, w) -> {

            String newName = input.getText().toString().trim();
            if (newName.isEmpty()) {
                Toast.makeText(ctx, "Invalid name", Toast.LENGTH_SHORT).show();
                return;
            }

            Uri videoUri = ContentUris.withAppendedId(
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                    v.getId()
            );

            ContentValues values = new ContentValues();
            values.put(MediaStore.Video.Media.DISPLAY_NAME, newName + ".mp4");

            int rows = ctx.getContentResolver().update(videoUri, values, null, null);

            if (rows > 0) {
                v.setTitle(newName);
                notifyDataSetChanged();
                Toast.makeText(ctx, "Renamed", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(ctx, "Rename failed", Toast.LENGTH_SHORT).show();
            }
        });

        dialog.setNegativeButton("Cancel", null);
        dialog.show();
    }

    // ------------------------------------------------------------
    // DETAILS
    // ------------------------------------------------------------
    private void showDetails(VideoItem v) {

        String msg =
                "Title: " + v.getTitle() + "\n\n" +
                        "Path: " + v.getFilePath() + "\n\n" +
                        "Duration: " + formatDuration(v.getDuration()) + "\n\n" +
                        "Size: " + formatSize(v.getSize()) + "\n\n" +
                        "Date Modified: " + formatDate(v.getDateModified());

        new AlertDialog.Builder(ctx)
                .setTitle("Details")
                .setMessage(msg)
                .setPositiveButton("OK", null)
                .show();
    }

    // ------------------------------------------------------------
    // SEARCH FILTER
    // ------------------------------------------------------------
    public int filter(String text) {
        text = text.toLowerCase();

        filteredList.clear();

        if (text.isEmpty()) {
            filteredList.addAll(videos);
        } else {
            for (VideoItem v : videos) {
                if (v.getTitle().toLowerCase().contains(text)) {
                    filteredList.add(v);
                }
            }
        }

        notifyDataSetChanged();
        return filteredList.size();
    }

    // ------------------------------------------------------------
    // HOLDER CLASS
    // ------------------------------------------------------------
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

    // ------------------------------------------------------------
    // HELPERS
    // ------------------------------------------------------------
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
