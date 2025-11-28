package com.example.remediaplayer;

import android.app.PictureInPictureParams;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Rational;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.MediaItem;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;

import java.io.File;

public class VideoPlayerView extends AppCompatActivity {

    private PlayerView playerView;
    private ExoPlayer player;

    private ImageButton backBtn, fullBtn, pipBtn;
    private TextView fileNameText;

    private boolean isFullscreen = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_player_view);

        String videoPath = getIntent().getStringExtra("video_path");

        // Bind views
        playerView = findViewById(R.id.playerview);
        backBtn = playerView.findViewById(R.id.back_icon);
        fullBtn = playerView.findViewById(R.id.fullscreen);
        pipBtn = playerView.findViewById(R.id.exo_pip);
        fileNameText = playerView.findViewById(R.id.File_name);

        // Set file name
        fileNameText.setText(new File(videoPath).getName());

        // Init player
        initPlayer(videoPath);

        // Back button
        backBtn.setOnClickListener(v -> onBackPressed());

        // Fullscreen toggle
        fullBtn.setOnClickListener(v -> toggleFullscreen());

        // Picture-in-Picture
        pipBtn.setOnClickListener(v -> enterPipModeSafe());
    }

    private void initPlayer(String path) {
        player = new ExoPlayer.Builder(this).build();
        playerView.setPlayer(player);

        MediaItem mediaItem = MediaItem.fromUri(Uri.fromFile(new File(path)));
        player.setMediaItem(mediaItem);
        player.prepare();
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

    private void enterPipModeSafe() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            enterPipMode();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void enterPipMode() {
        Rational aspectRatio = new Rational(playerView.getWidth(), playerView.getHeight());
        PictureInPictureParams params =
                new PictureInPictureParams.Builder()
                        .setAspectRatio(aspectRatio)
                        .build();
        enterPictureInPictureMode(params);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (!isInPictureInPictureMode() && player != null) {
            player.pause();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (player != null) player.release();
    }
}