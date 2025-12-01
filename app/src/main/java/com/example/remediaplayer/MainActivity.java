package com.example.remediaplayer;

import android.Manifest;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.SearchView;
import android.widget.TextView;
import android.app.AlertDialog;

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

import android.provider.MediaStore;

import java.util.ArrayList;
import java.util.Collections;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_CODE = 101;

    RecyclerView videoRecyclerView;
    TextView videoCountText;
    SearchView searchView;
    ImageButton menuButton;

    ArrayList<VideoItem> videoList = new ArrayList<>();
    VideoAdapter adapter;
    public Uri pendingModifyUri = null;
    public int pendingAction = 0;
    public String pendingNewName = null;
    ActivityResultLauncher<IntentSenderRequest> writeRequestLauncher;
    private Handler handler = new Handler();
    private Runnable hideRunnable = this::hideSystemUI;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        boolean darkMode = getSharedPreferences("settings", MODE_PRIVATE)
                .getBoolean("dark_mode", false);

        if (darkMode)
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        else
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        videoRecyclerView = findViewById(R.id.videoRecyclerView);
        videoCountText = findViewById(R.id.videoCountText);
        searchView = findViewById(R.id.searchbar);
        menuButton = findViewById(R.id.menu_button);

        setupWriteRequestHandler();
        setupMenuButton();
        setupSearchBar();
        setupSystemUIHiding();

        if (hasPermission()) {
            loadVideos();
        } else {
            requestPermission();
        }
    }
    private void setupWriteRequestHandler() {
        writeRequestLauncher = registerForActivityResult(
                new ActivityResultContracts.StartIntentSenderForResult(),
                result -> {

                    if (result.getResultCode() == RESULT_OK) {

                        if (pendingAction == 1) {
                            getContentResolver().delete(pendingModifyUri, null, null);
                        }
                        else if (pendingAction == 2) {
                            ContentValues values = new ContentValues();
                            values.put(MediaStore.Video.Media.DISPLAY_NAME, pendingNewName + ".mp4");
                            getContentResolver().update(pendingModifyUri, values, null, null);
                        }

                        pendingModifyUri = null;
                        pendingAction = 0;
                        pendingNewName = null;

                        loadVideos();
                    }
                }
        );
    }

    private void setupMenuButton() {

        menuButton.setOnClickListener(v -> {

            PopupMenu popup = new PopupMenu(MainActivity.this, v);
            popup.getMenuInflater().inflate(R.menu.main_menu, popup.getMenu());

            popup.setOnMenuItemClickListener(item -> {

                int id = item.getItemId();

                if (id == R.id.menu_sort) {
                    showSortDialog();
                    return true;
                }

                if (id == R.id.menu_settings) {
                    startActivity(new Intent(MainActivity.this, SettingsActivity.class));
                    return true;
                }

                return false;
            });

            popup.show();
        });
    }

    private void showSortDialog() {

        String[] options = {
                "Name (A–Z)",
                "Name (Z–A)",
                "Size (Large → Small)",
                "Size (Small → Large)",
                "Duration (Long → Short)",
                "Duration (Short → Long)",
                "Date (Newest → Oldest)",
                "Date (Oldest → Newest)"
        };

        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setTitle("Sort Videos");
        dialog.setItems(options, (d, which) -> applySort(which));
        dialog.show();
    }

    private void applySort(int type) {

        switch (type) {

            case 0: // Name A-Z
                Collections.sort(videoList, (a, b) -> a.getTitle().compareToIgnoreCase(b.getTitle()));
                break;

            case 1: // Name Z-A
                Collections.sort(videoList, (a, b) -> b.getTitle().compareToIgnoreCase(a.getTitle()));
                break;

            case 2: // Size Large > Small
                Collections.sort(videoList, (a, b) -> Long.compare(b.getSize(), a.getSize()));
                break;

            case 3: // Size Small > Large
                Collections.sort(videoList, (a, b) -> Long.compare(a.getSize(), b.getSize()));
                break;

            case 4: // Duration Long > Short
                Collections.sort(videoList, (a, b) -> Long.compare(b.getDuration(), a.getDuration()));
                break;

            case 5: // Duration Short > Long
                Collections.sort(videoList, (a, b) -> Long.compare(a.getDuration(), b.getDuration()));
                break;

            case 6: // Date Newest > Oldest
                Collections.sort(videoList, (a, b) -> Long.compare(b.getDateModified(), a.getDateModified()));
                break;

            case 7: // Date Oldest > Newest
                Collections.sort(videoList, (a, b) -> Long.compare(a.getDateModified(), b.getDateModified()));
                break;
        }

        adapter.updateList(videoList);
        videoCountText.setText(videoList.size() + " videos");
    }

    private void setupSearchBar() {

        searchView.clearFocus();

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {

            @Override
            public boolean onQueryTextSubmit(String q) {
                int count = adapter.filter(q);
                videoCountText.setText(count + " videos");
                return true;
            }

            @Override
            public boolean onQueryTextChange(String q) {
                int count = adapter.filter(q);
                videoCountText.setText(count + " videos");
                return true;
            }
        });
    }

    private void loadVideos() {
        videoList = VideoLoader.loadVideos(this);
        videoCountText.setText(videoList.size() + " videos");

        adapter = new VideoAdapter(this, videoList, writeRequestLauncher);
        videoRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        videoRecyclerView.setAdapter(adapter);
    }

    private boolean hasPermission() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            return ContextCompat.checkSelfPermission(this,
                    Manifest.permission.READ_MEDIA_VIDEO)
                    == PackageManager.PERMISSION_GRANTED;

        return ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermission() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_MEDIA_VIDEO},
                    PERMISSION_CODE);

        else ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                PERMISSION_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] perms,
                                           @NonNull int[] results) {

        super.onRequestPermissionsResult(requestCode, perms, results);

        if (requestCode == PERMISSION_CODE &&
                results.length > 0 &&
                results[0] == PackageManager.PERMISSION_GRANTED) {
            loadVideos();
        }
    }

    private void setupSystemUIHiding() {

        boolean hideUI = getSharedPreferences("settings", MODE_PRIVATE)
                .getBoolean("hide_ui", true);

        if (hideUI) handler.postDelayed(hideRunnable, 1500);

        findViewById(android.R.id.content).setOnTouchListener((v, e) -> {

            showSystemUI();

            boolean setting = getSharedPreferences("settings", MODE_PRIVATE)
                    .getBoolean("hide_ui", true);

            if (setting) {
                handler.removeCallbacks(hideRunnable);
                handler.postDelayed(hideRunnable, 3000);
            }

            return false;
        });
    }

    private void hideSystemUI() {

        boolean hideSetting = getSharedPreferences("settings", MODE_PRIVATE)
                .getBoolean("hide_ui", true);

        if (!hideSetting) return;


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {

            getWindow().setDecorFitsSystemWindows(false);
            WindowInsetsController controller = getWindow().getInsetsController();

            if (controller != null) {
                controller.hide(WindowInsets.Type.systemBars());
                controller.setSystemBarsBehavior(
                        WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                );
            }
        }
    }

    private void showSystemUI() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {

            getWindow().setDecorFitsSystemWindows(true);
            WindowInsetsController controller = getWindow().getInsetsController();

            if (controller != null) controller.show(WindowInsets.Type.systemBars());
        }
    }
    @Override
    protected void onResume() {
        super.onResume();

        boolean darkMode = getSharedPreferences("settings", MODE_PRIVATE)
                .getBoolean("dark_mode", false);

        if (darkMode)
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        else
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
    }
}
