package com.example.remediaplayer;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class VideoAdapter extends RecyclerView.Adapter<VideoAdapter.Holder> {

    private Context ctx;
    private ArrayList<VideoItem> list;
    private VideoActionListener listener;

    public VideoAdapter(Context ctx, ArrayList<VideoItem> list, VideoActionListener listener) {
        this.ctx = ctx;
        this.list = list;
        this.listener = listener;
    }

    public void updateList(ArrayList<VideoItem> newList) {
        this.list = newList;
        notifyDataSetChanged();
    }

    public int filter(String q) {
        if (q == null || q.trim().isEmpty()) return list.size();

        q = q.toLowerCase(Locale.ROOT);
        ArrayList<VideoItem> filtered = new ArrayList<>();

        for (VideoItem v : list) {
            if (v.getTitle().toLowerCase(Locale.ROOT).contains(q))
                filtered.add(v);
        }

        this.list = filtered;
        notifyDataSetChanged();
        return filtered.size();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup p, int vType) {
        View v = LayoutInflater.from(ctx).inflate(R.layout.video_item, p, false);
        return new Holder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder h, int pos) {
        VideoItem v = list.get(pos);

        h.title.setText(v.getTitle());
        h.size.setText(formatFileSize(v.getSize()));
        h.duration.setText(v.getDurationFormatted());
        h.date.setText(formatDate(v.getDateModified()));

        Glide.with(ctx)
                .load(v.getThumbnail())
                .placeholder(R.drawable.thumbnail_placeholder)
                .into(h.thumbnail);

        h.itemView.setOnClickListener(x -> listener.onPlay(v));
        h.menuBtn.setOnClickListener(btn -> showMenu(btn, v));
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    private void showMenu(View view, VideoItem v) {
        PopupMenu menu = new PopupMenu(ctx, view);
        menu.inflate(R.menu.video_options);

        menu.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();

            if (id == R.id.optPlay) listener.onPlay(v);
            else if (id == R.id.optShare) listener.onShare(v);
            else if (id == R.id.optRename) renameDialog(v);
            else if (id == R.id.optDelete) deleteDialog(v);

            return true;
        });

        menu.show();
    }

    private void renameDialog(VideoItem v) {

        if (!v.isLocal()) {
            toast("Cannot rename online videos.");
            return;
        }

        View dialog = LayoutInflater.from(ctx).inflate(R.layout.dialog_rename, null);
        TextView input = dialog.findViewById(R.id.rename_input);
        input.setText(v.getTitle());

        new AlertDialog.Builder(ctx)
                .setTitle("Rename")
                .setView(dialog)
                .setPositiveButton("OK", (d, w) -> {
                    String newName = input.getText().toString().trim();
                    if (newName.isEmpty()) { toast("Name cannot be empty"); return; }

                    v.setTempNewName(newName);
                    listener.onRenameRequest(v);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteDialog(VideoItem v) {

        if (!v.isLocal()) { toast("Cannot delete online videos."); return; }

        new AlertDialog.Builder(ctx)
                .setTitle("Delete?")
                .setMessage("Delete this video permanently?")
                .setPositiveButton("Delete", (d, w) -> listener.onDeleteRequest(v))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void toast(String m) {
        android.widget.Toast.makeText(ctx, m, android.widget.Toast.LENGTH_SHORT).show();
    }

    private String formatFileSize(long s) {
        if (s <= 0) return "0 MB";
        return new DecimalFormat("#.#").format(s / (1024f * 1024f)) + " MB";
    }

    private String formatDate(long ms) {
        return new SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                .format(new Date(ms));
    }

    static class Holder extends RecyclerView.ViewHolder {

        ImageView thumbnail;
        TextView title, size, duration, date;
        ImageButton menuBtn;

        public Holder(@NonNull View v) {
            super(v);
            thumbnail = v.findViewById(R.id.videoThumbnail);
            title = v.findViewById(R.id.videoName);
            size = v.findViewById(R.id.videoSize);
            duration = v.findViewById(R.id.videoDuration);
            date = v.findViewById(R.id.videoDate);
            menuBtn = v.findViewById(R.id.btnMoreOptions);
        }
    }
}
