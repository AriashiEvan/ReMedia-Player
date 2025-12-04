package com.example.remediaplayer;

import android.app.PictureInPictureParams;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.util.Rational;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import com.example.remediaplayer.peertube.PeerTubeManager;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.common.PlaybackException;
import androidx.media3.ui.PlayerView;
import androidx.media3.datasource.DefaultHttpDataSource;
import androidx.media3.datasource.DefaultDataSource;
import androidx.media3.exoplayer.source.ProgressiveMediaSource;
import androidx.media3.exoplayer.hls.HlsMediaSource;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.source.MediaSource;
import java.io.File;

public class VideoPlayerView extends AppCompatActivity {

    private static final String TAG_PLAYER = "PLAYER_DEBUG";
    private static final String TAG_EXO = "EXO_ERROR";
    private static final String TAG_PEERTUBE = "PEERTUBE_RESOLVE";

    private PlayerView playerView;
    private ExoPlayer player;

    private ImageButton backBtn, fullBtn, pipBtn;
    private TextView fileNameText;

    private boolean isFullscreen = false;
    private String originalPath;
    private PeerTubeManager peerTube;

    @UnstableApi
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        boolean dark = getSharedPreferences("settings", MODE_PRIVATE)
                .getBoolean("dark_mode", false);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_player_view);

        peerTube = new PeerTubeManager(this);

        playerView = findViewById(R.id.playerview);
        backBtn = playerView.findViewById(R.id.back_icon);
        fullBtn = playerView.findViewById(R.id.fullscreen);
        pipBtn = playerView.findViewById(R.id.exo_pip);
        fileNameText = playerView.findViewById(R.id.File_name);

        originalPath = getIntent().getStringExtra("video_path");
        if (originalPath == null) originalPath = "";

        if (originalPath.startsWith("http")) {
            fileNameText.setText(originalPath);
        } else {
            fileNameText.setText(new File(originalPath).getName());
        }

        initPlayer(originalPath);
        enableImmersiveMode();

        backBtn.setOnClickListener(v -> onBackPressed());
        fullBtn.setOnClickListener(v -> toggleFullscreen());
        pipBtn.setOnClickListener(v -> enterPipSafely());
    }

    @UnstableApi
    private void initPlayer(String path) {
        if (player != null) {
            player.release();
            player = null;
        }

        player = new ExoPlayer.Builder(this).build();
        playerView.setPlayer(player);


        player.addListener(new Player.Listener() {
            @Override
            public void onPlayerError(PlaybackException error) {
                Log.e(TAG_EXO, "Playback failed: " + (error == null ? "null" : error.getMessage()), error);
                Toast.makeText(VideoPlayerView.this, "Playback failed: " + (error == null ? "error" : error.getMessage()), Toast.LENGTH_LONG).show();
            }
        });

        if (path.startsWith("http") && path.contains("/videos/watch/")) {
            Log.d(TAG_PLAYER, "Detected PeerTube URL -> resolving...");
            peerTube.resolveStreamUrl(path, new PeerTubeManager.ResolveCallback() {
                @UnstableApi
                @Override
                public void onResolved(String streamUrl) {
                    Log.d(TAG_PLAYER, "Resolved URL = " + streamUrl);
                    runOnUiThread(() -> {
                        prepareAndPlay(streamUrl);
                    });
                }

                @UnstableApi
                @Override
                public void onError(Throwable t) {
                    Log.e(TAG_PEERTUBE, "Resolve FAILED", t);
                    runOnUiThread(() -> {
                        Toast.makeText(VideoPlayerView.this, "Could not resolve video stream", Toast.LENGTH_SHORT).show();

                        prepareAndPlay(path);
                    });
                }
            });
        } else {

            prepareAndPlay(path);
        }
    }

    @UnstableApi
    private void prepareAndPlay(String path) {
        Log.d(TAG_PLAYER, "Preparing media: " + path);

        MediaItem item;
        try {
            if (path.startsWith("http") || path.startsWith("https")) {
                item = MediaItem.fromUri(path);
            } else if (path.startsWith("file://")) {
                item = MediaItem.fromUri(Uri.parse(path));
            } else {
                item = MediaItem.fromUri(Uri.fromFile(new File(path)));
            }
        } catch (Exception e) {
            Log.e(TAG_PLAYER, "Invalid media path", e);
            Toast.makeText(this, "Invalid media path", Toast.LENGTH_SHORT).show();
            return;
        }

        DefaultHttpDataSource.Factory httpFactory = new DefaultHttpDataSource.Factory();
        DefaultDataSource.Factory dataSourceFactory = new DefaultDataSource.Factory(this, httpFactory);

        MediaSource mediaSource;
        String lower = path.toLowerCase();
        if (lower.contains(".m3u8") || lower.contains(".m3u") || lower.contains("playlist") && lower.contains(".m3u")) {

            Log.d(TAG_PLAYER, "Detected HLS playlist");
            mediaSource = new HlsMediaSource.Factory(dataSourceFactory).createMediaSource(item);
        } else if (lower.endsWith(".mp4") || lower.endsWith(".webm") || lower.endsWith(".mkv") || lower.endsWith(".3gp") || lower.endsWith(".ts")) {
            mediaSource = new ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(item);
        } else {
            Log.d(TAG_PLAYER, "Unknown extension: trying progressive then HLS fallback");
            mediaSource = new ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(item);
        }

        player.setMediaSource(mediaSource);
        player.prepare();

        boolean autoPlay = getSharedPreferences("settings", MODE_PRIVATE)
                .getBoolean("auto_play", true);

        if (autoPlay) player.play();
    }

    private void toggleFullscreen() {
        if (!isFullscreen) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            isFullscreen = true;
        } else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            isFullscreen = false;
        }
    }

    private void enterPipSafely() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            enterPiP();
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private void enterPiP() {
        PictureInPictureParams params =
                new PictureInPictureParams.Builder()
                        .setAspectRatio(new Rational(16, 9))
                        .build();
        enterPictureInPictureMode(params);
    }

    private void enableImmersiveMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            getWindow().setDecorFitsSystemWindows(false);
            WindowInsetsController c = getWindow().getInsetsController();
            if (c != null) {
                c.hide(WindowInsets.Type.systemBars());
                c.setSystemBarsBehavior(
                        WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                );
            }
        } else {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY |
                            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                            View.SYSTEM_UI_FLAG_FULLSCREEN |
                            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            );
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) enableImmersiveMode();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (player != null && !isInPictureInPictureMode()) {
            player.pause();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (player != null) {
            player.release();
            player = null;
        }
    }
}
