package com.example.remediaplayer;

import android.app.PictureInPictureParams;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Rational;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.MediaItem;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;

import java.io.File;

/** @noinspection ALL*/
public class VideoPlayerView extends AppCompatActivity {

    PlayerView playerView;
    ExoPlayer player;

    ImageButton backBtn, fullBtn, pipBtn;
    TextView fileNameText;

    boolean isFullscreen = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_player_view);

        String path = getIntent().getStringExtra("video_path");

        playerView = findViewById(R.id.playerview);
        backBtn = playerView.findViewById(R.id.back_icon);
        fullBtn = playerView.findViewById(R.id.fullscreen);
        pipBtn = playerView.findViewById(R.id.exo_pip);
        fileNameText = playerView.findViewById(R.id.File_name);

        fileNameText.setText(new File(path).getName());

        initPlayer(path);
        enableImmersiveMode();

        backBtn.setOnClickListener(v -> onBackPressed());

        fullBtn.setOnClickListener(v -> toggleFullscreen());

        pipBtn.setOnClickListener(v -> enterPipSafe());
    }

    private void enableImmersiveMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            getWindow().setDecorFitsSystemWindows(false);

            WindowInsetsController controller = getWindow().getInsetsController();
            if (controller != null) {
                controller.hide(WindowInsets.Type.systemBars());
                controller.setSystemBarsBehavior(
                        WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                );
            }
        } else {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            );
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) enableImmersiveMode();
    }

    private void initPlayer(String path) {
        player = new ExoPlayer.Builder(this).build();
        playerView.setPlayer(player);

        MediaItem item = MediaItem.fromUri(Uri.fromFile(new File(path)));
        player.setMediaItem(item);
        player.prepare();
        boolean autoPlay = getSharedPreferences("settings", MODE_PRIVATE)
                .getBoolean("auto_play", true);

        if (autoPlay)
            player.play();

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

    private void enterPipSafe() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) enterPiP();
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void enterPiP() {
        PictureInPictureParams params =
                new PictureInPictureParams.Builder()
                        .setAspectRatio(new Rational(16, 9))
                        .build();
        enterPictureInPictureMode(params);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (!isInPictureInPictureMode()) {
            player.pause();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        player.release();
    }
}