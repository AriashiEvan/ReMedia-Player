package com.example.remediaplayer;

import android.Manifest;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.widget.SearchView;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_CODE = 101;

    RecyclerView videoRecyclerView;
    TextView videoCountText;
    SearchView searchView;

    ArrayList<VideoItem> videoList = new ArrayList<>();
    VideoAdapter adapter;

    // For rename/delete WriteRequest
    public Uri pendingModifyUri = null;
    public int pendingAction = 0; // 1 = delete, 2 = rename
    public String pendingNewName = null;

    private ActivityResultLauncher<IntentSenderRequest> writeRequestLauncher;

    Handler handler = new Handler();
    Runnable hideRunnable = this::hideSystemUI;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        videoRecyclerView = findViewById(R.id.videoRecyclerView);
        videoCountText = findViewById(R.id.videoCountText);
        searchView = findViewById(R.id.searchbar);

        initWriteRequestHandler();
        hideAfterDelay();

        setupTouchToShowSystemUI();

        if (hasPermission()) {
            loadVideos();
        } else requestPermission();

        handleSearch();
    }

    private void initWriteRequestHandler() {
        writeRequestLauncher = registerForActivityResult(
                new ActivityResultContracts.StartIntentSenderForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {

                        if (pendingAction == 1) { // DELETE
                            getContentResolver().delete(pendingModifyUri, null, null);
                        }
                        else if (pendingAction == 2) { // RENAME
                            android.content.ContentValues values = new android.content.ContentValues();
                            values.put(android.provider.MediaStore.Video.Media.DISPLAY_NAME, pendingNewName + ".mp4");
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

    private void handleSearch() {
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

    private boolean hasPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_VIDEO)
                    == PackageManager.PERMISSION_GRANTED;

        return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_MEDIA_VIDEO}, PERMISSION_CODE);
        else
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSION_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int code, @NonNull String[] perms, @NonNull int[] results) {
        super.onRequestPermissionsResult(code, perms, results);
        if (code == PERMISSION_CODE && results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED)
            loadVideos();
    }

    private void loadVideos() {
        videoList = VideoLoader.loadVideos(this);
        videoCountText.setText(videoList.size() + " videos");

        adapter = new VideoAdapter(this, videoList, writeRequestLauncher);
        videoRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        videoRecyclerView.setAdapter(adapter);
    }

    private void hideAfterDelay() {
        handler.postDelayed(hideRunnable, 1500);
    }

    private void setupTouchToShowSystemUI() {
        findViewById(android.R.id.content).setOnTouchListener((v, e) -> {
            showSystemUI();
            handler.removeCallbacks(hideRunnable);
            handler.postDelayed(hideRunnable, 3000);
            return false;
        });
    }

    private void hideSystemUI() {
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
}
