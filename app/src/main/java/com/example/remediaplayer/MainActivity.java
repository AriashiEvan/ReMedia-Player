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
import java.io.File;
import androidx.core.content.FileProvider;
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
import java.util.List;

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


    private int onlineOffset = 0;
    private final int PAGE_SIZE = 20;
    private int onlineTotal = Integer.MAX_VALUE;
    private boolean loadingOnline = false;


    private long pendingId = -1;
    private String pendingNewName = null;
    private int pendingAction = 0;

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
        setupRecycler();
        setupMenu();
        setupSwitch();
        setupLauncher();
        setupSearch();

        peerTube = new PeerTubeManager(this);

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

        videoRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView rv, int dx, int dy) {
                super.onScrolled(rv, dx, dy);

                if (!showingOnline) return;
                if (loadingOnline) return;

                LinearLayoutManager lm = (LinearLayoutManager) rv.getLayoutManager();
                if (lm == null) return;

                int visible = lm.getChildCount();
                int total = lm.getItemCount();
                int first = lm.findFirstVisibleItemPosition();

                // Trigger next page load
                if ((first + visible) >= (total - 4) && total < onlineTotal) {
                    loadMoreOnline(null);
                }
            }
        });
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

    private void setupSearch() {
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {

            @Override
            public boolean onQueryTextSubmit(String q) {

                if (q == null) return true;
                q = q.trim();

                // URL shortcut
                if (isValidUrl(q)) {
                    Intent i = new Intent(MainActivity.this, VideoPlayerView.class);
                    i.putExtra("video_path", q);
                    startActivity(i);
                    return true;
                }

                if (showingOnline) {
                    // New search → reset pagination
                    onlineList.clear();
                    adapter.updateList(new ArrayList<>());
                    videoCountText.setText("Loading online videos...");
                    onlineOffset = 0;
                    onlineTotal = Integer.MAX_VALUE;
                    loadMoreOnline(q);
                } else {
                    int c = adapter.filter(q);
                    videoCountText.setText(c + " videos");
                }
                return true;
            }

            @Override
            public boolean onQueryTextChange(String t) {
                if (!showingOnline) {
                    int c = adapter.filter(t == null ? "" : t);
                    videoCountText.setText(c + " videos");
                }
                return true;
            }
        });
    }

    private boolean isValidUrl(String s) {
        return s.startsWith("http://") ||
                s.startsWith("https://") ||
                Patterns.WEB_URL.matcher(s).matches();
    }

    private void loadMoreOnline(String query) {
        if (peerTube == null) return;

        loadingOnline = true;
        videoCountText.setText("Loading online videos...");

        peerTube.search(query, onlineOffset, PAGE_SIZE, new PeerTubeManager.PeerTubeCallback() {
            @Override
            public void onResult(List<VideoItem> results, int total) {

                runOnUiThread(() -> {

                    final List<VideoItem> safeResults =
                            (results == null) ? new ArrayList<>() : results;


                    onlineTotal = total;

                    onlineList.addAll(results);
                    adapter.updateList(onlineList);

                    onlineOffset += results.size();

                    videoCountText.setText(onlineList.size() + " online videos");
                    loadingOnline = false;
                });
            }
        });

    }


    private void setupSwitch() {
        switchSource.setOnCheckedChangeListener((button, isOnline) -> {

            showingOnline = isOnline;
            searchView.setQuery("", false);

            if (isOnline) {
                // Reset online mode state
                onlineList.clear();
                adapter.updateList(onlineList);
                videoCountText.setText("Loading online videos...");

                onlineOffset = 0;
                onlineTotal = Integer.MAX_VALUE;

                loadMoreOnline(null);

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
    public void onRequestPermissionsResult(int c,
                                           @NonNull String[] p,
                                           @NonNull int[] r) {
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
    public void onShare(VideoItem item) {

        if (item.isLocal()) {

            File f = new File(item.getPath());

            Uri uri = FileProvider.getUriForFile(
                    this,
                    getPackageName() + ".provider",
                    f
            );

            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("video/*");
            shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            startActivity(Intent.createChooser(shareIntent, "Share Video"));
        }
        else {
            // ONLINE VIDEO SHARE
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");

            // Online video URL stored in path
            shareIntent.putExtra(Intent.EXTRA_TEXT, item.getPath());

            startActivity(Intent.createChooser(shareIntent, "Share Link"));
        }
    }



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
