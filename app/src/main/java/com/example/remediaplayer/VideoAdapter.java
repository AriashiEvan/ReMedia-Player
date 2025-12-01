package com.example.remediaplayer;

import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.ContentUris;
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

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;
import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;

public class VideoAdapter extends RecyclerView.Adapter<VideoAdapter.Holder> {

    Context ctx;
    ArrayList<VideoItem> videos;
    ArrayList<VideoItem> filteredList;

    ActivityResultLauncher<IntentSenderRequest> writeLauncher;

    public VideoAdapter(Context ctx, ArrayList<VideoItem> videos,
                        ActivityResultLauncher<IntentSenderRequest> launcher) {

        this.ctx = ctx;
        this.videos = new ArrayList<>(videos);
        this.filteredList = new ArrayList<>(videos);
        this.writeLauncher = launcher;
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

    private void showMoreOptions(VideoItem v) {

        String[] options = {"Play", "Share", "Delete", "Rename", "Details"};

        AlertDialog.Builder b = new AlertDialog.Builder(ctx);
        b.setTitle(v.getTitle());
        b.setItems(options, (dialog, which) -> {

            switch (which) {
                case 0:
                    playVideo(v);
                    break;

                case 1:
                    shareVideo(v);
                    break;

                case 2:
                    requestDelete(v);
                    break;

                case 3:
                    requestRename(v);
                    break;

                case 4:
                    showDetails(v);
                    break;
            }
        });

        b.show();
    }

    private void playVideo(VideoItem v) {
        Intent i = new Intent(ctx, VideoPlayerView.class);
        i.putExtra("video_path", v.getFilePath());
        ctx.startActivity(i);
    }

    private void shareVideo(VideoItem v) {

        try {
            File file = new File(v.getFilePath());

            Uri uri = FileProvider.getUriForFile(
                    ctx,
                    ctx.getPackageName() + ".provider",
                    file
            );

            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("video/*");
            shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            ctx.startActivity(Intent.createChooser(shareIntent, "Share Video"));

        } catch (Exception e) {
            Toast.makeText(ctx, "Unable to share video", Toast.LENGTH_SHORT).show();
        }
    }

    private void requestDelete(VideoItem v) {

        Uri mediaUri = ContentUris.withAppendedId(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                v.getId()
        );

        ((MainActivity) ctx).pendingModifyUri = mediaUri;
        ((MainActivity) ctx).pendingAction = 1;

        try {
            PendingIntent pi = MediaStore.createWriteRequest(
                    ctx.getContentResolver(),
                    Collections.singletonList(mediaUri)
            );

            IntentSenderRequest req = new IntentSenderRequest.Builder(pi.getIntentSender()).build();
            writeLauncher.launch(req);

        } catch (Exception e) {
            Toast.makeText(ctx, "Delete failed", Toast.LENGTH_SHORT).show();
        }
    }

    private void requestRename(VideoItem v) {

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

            Uri mediaUri = ContentUris.withAppendedId(
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                    v.getId()
            );

            ((MainActivity) ctx).pendingModifyUri = mediaUri;
            ((MainActivity) ctx).pendingAction = 2;
            ((MainActivity) ctx).pendingNewName = newName;

            try {
                PendingIntent pi = MediaStore.createWriteRequest(
                        ctx.getContentResolver(),
                        Collections.singletonList(mediaUri)
                );

                IntentSenderRequest req = new IntentSenderRequest.Builder(pi.getIntentSender()).build();
                writeLauncher.launch(req);

            } catch (Exception e) {
                Toast.makeText(ctx, "Rename failed", Toast.LENGTH_SHORT).show();
            }

        });

        dialog.setNegativeButton("Cancel", null);
        dialog.show();
    }

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

    public void updateList(ArrayList<VideoItem> newList) {
        videos = new ArrayList<>(newList);
        filteredList = new ArrayList<>(newList);
        notifyDataSetChanged();
    }

    // -------------------------------------------------------------------------
    // HOLDER
    // -------------------------------------------------------------------------
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

    private String formatSize(long bytes) {
        return (bytes / (1024 * 1024)) + " MB";
    }

    private String formatDate(long unix) {
        return new SimpleDateFormat("dd MMM yyyy").format(new Date(unix * 1000));
    }
}
