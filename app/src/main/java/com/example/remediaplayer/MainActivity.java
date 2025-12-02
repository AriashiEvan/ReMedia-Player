package com.example.remediaplayer;

import android.Manifest;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Patterns;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.SearchView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.remediaplayer.peertube.PeerTubeManager;

import java.util.ArrayList;
import java.util.Collections;

public class MainActivity extends AppCompatActivity implements VideoActionListener {

    private RecyclerView videoRecyclerView;
    private TextView videoCountText;
    private SearchView searchView;
    private ImageButton menuButton;
    private Switch switchSource;

    private ArrayList<VideoItem> localList = new ArrayList<>();
    private ArrayList<VideoItem> onlineList = new ArrayList<>();
    private VideoAdapter adapter;
    private PeerTubeManager peerTube;


    private boolean showingOnline = false;

    private long pendingId = -1;
    private String pendingNewName = null;
    private int pendingAction = 0; // 1 delete, 2 rename

    private ActivityResultLauncher<IntentSenderRequest> writeLauncher;

    private static final int PERMISSION_CODE = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        boolean dark = getSharedPreferences("settings", MODE_PRIVATE)
                .getBoolean("dark_mode", false);
        AppCompatDelegate.setDefaultNightMode(
                dark ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
        );

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bindViews();
        peerTube = new PeerTubeManager(this);
        setupRecycler();
        setupLauncher();
        setupSearch();
        setupMenu();
        setupSwitch();


        if (hasPermission()) loadLocalVideos();
        else requestPermission();
    }

    private void bindViews() {
        videoRecyclerView = findViewById(R.id.videoRecyclerView);
        videoCountText = findViewById(R.id.videoCountText);
        searchView = findViewById(R.id.searchbar);
        menuButton = findViewById(R.id.menu_button);
        switchSource = findViewById(R.id.switchSource);
    }

    private void setupRecycler() {
        adapter = new VideoAdapter(this, new ArrayList<>(), this);
        videoRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        videoRecyclerView.setAdapter(adapter);
    }

    private void setupMenu() {
        menuButton.setOnClickListener(v -> {
            PopupMenu m = new PopupMenu(this, v);
            m.inflate(R.menu.main_menu);

            m.setOnMenuItemClickListener(i -> {
                if (i.getItemId() == R.id.menu_settings) {
                    startActivity(new Intent(this, SettingsActivity.class));
                    return true;
                }
                return false;
            });

            m.show();
        });
    }

    private void searchOnline(String query) {

        if (peerTube == null) return;

        peerTube.search(query, results -> {

            if (results == null) results = new ArrayList<>();

            onlineList.clear();
            onlineList.addAll(results);

            adapter.updateList(onlineList);
            videoCountText.setText(results.size() + " online videos");
        });
    }

    private void setupSwitch() {
        switchSource.setOnCheckedChangeListener((button, isOnline) -> {
            showingOnline = isOnline;
            searchView.setQuery("", false);

            if (isOnline) {

                onlineList.clear();
                adapter.updateList(onlineList);
                videoCountText.setText("Loading online videos...");

                searchOnline(null);

            } else {
                adapter.updateList(localList);
                videoCountText.setText(localList.size() + " videos");
            }
        });

    }

    private void setupLauncher() {
        writeLauncher = registerForActivityResult(
                new ActivityResultContracts.StartIntentSenderForResult(),
                r -> {
                    if (r.getResultCode() != RESULT_OK) {
                        clearPending();
                        return;
                    }

                    if (pendingAction == 2) performRename();
                    if (pendingAction == 1) {
                        // SAF delete already performed
                        Toast.makeText(this, "Deleted", Toast.LENGTH_SHORT).show();
                    }

                    loadLocalVideos();
                    clearPending();
                }
        );
    }

    private void clearPending() {
        pendingAction = 0;
        pendingId = -1;
        pendingNewName = null;
    }

    private void performRename() {
        String newName = pendingNewName;
        if (!newName.contains(".")) newName += ".mp4";

        Uri uri = ContentUris.withAppendedId(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                pendingId
        );

        ContentValues cv = new ContentValues();
        cv.put(MediaStore.Video.Media.DISPLAY_NAME, newName);

        int res = getContentResolver().update(uri, cv, null, null);

        if (res <= 0)
            Toast.makeText(this, "Rename failed", Toast.LENGTH_LONG).show();
        else
            Toast.makeText(this, "Renamed", Toast.LENGTH_SHORT).show();
    }

    private void setupSearch() {
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String q) {

                if (q == null) return true;
                q = q.trim();

                if (isValidUrl(q)) {
                    Intent i = new Intent(MainActivity.this, VideoPlayerView.class);
                    i.putExtra("video_path", q);
                    startActivity(i);
                    return true;
                }

                int c = adapter.filter(q);
                videoCountText.setText(c + " videos");

                return true;
            }

            @Override
            public boolean onQueryTextChange(String t) {
                int c = adapter.filter(t == null ? "" : t);
                videoCountText.setText(c + " videos");
                return true;
            }
        });
    }

    private boolean isValidUrl(String s) {
        return s.startsWith("http://") ||
                s.startsWith("https://") ||
                Patterns.WEB_URL.matcher(s).matches();
    }

    private void loadLocalVideos() {
        localList = VideoLoader.loadVideos(this);

        if (!showingOnline) {
            adapter.updateList(localList);
            videoCountText.setText(localList.size() + " videos");
        }
    }

    private boolean hasPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            return checkSelfPermission(Manifest.permission.READ_MEDIA_VIDEO)
                    == PackageManager.PERMISSION_GRANTED;

        return ContextCompat.checkSelfPermission(
                this, Manifest.permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.READ_MEDIA_VIDEO},
                    PERMISSION_CODE
            );
        else
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    PERMISSION_CODE
            );
    }

    @Override
    public void onRequestPermissionsResult(int c, @NonNull String[] p, @NonNull int[] r) {
        if (c == PERMISSION_CODE &&
                r.length > 0 &&
                r[0] == PackageManager.PERMISSION_GRANTED)
            loadLocalVideos();

        super.onRequestPermissionsResult(c, p, r);
    }

    @Override
    public void onPlay(VideoItem item) {
        Intent i = new Intent(this, VideoPlayerView.class);
        i.putExtra("video_path", item.getPath());
        startActivity(i);
    }

    @Override
    public void onShare(VideoItem item) {}

    @Override
    public void onRenameRequest(VideoItem item) {
        pendingAction = 2;
        pendingId = item.getId();
        pendingNewName = item.getTempNewName();

        writeLauncher.launch(
                VideoLoader.getWriteRequestForId(this, pendingId)
        );
    }

    @Override
    public void onDeleteRequest(VideoItem item) {
        pendingAction = 1;
        pendingId = item.getId();

        writeLauncher.launch(
                VideoLoader.getDeleteRequestForIds(
                        this,
                        Collections.singletonList(item.getId())
                )
        );
    }
}
