package com.example.remediaplayer;

import android.annotation.SuppressLint;
import android.app.PictureInPictureParams;
import android.content.pm.ActivityInfo;
import android.os.Build;
import android.os.Bundle;
import android.util.Rational;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;

import androidx.activity.EdgeToEdge;
import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.media3.common.MediaItem;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;

public class VideoPlayerView extends AppCompatActivity {

    ExoPlayer player;
    PlayerView playerView;
    ImageButton pipButton, fullscreenbutton, backbutton;
    boolean isFullscreen = false;
    boolean isInPip = false;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_video_player_view);

        playerView = findViewById(R.id.playerview);
        fullscreenbutton = findViewById(R.id.fullscreen);
        pipButton = findViewById(R.id.exo_pip);
        backbutton = findViewById(R.id.back_icon);


        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        setupPlayer();
        setupFullscreenButton();
        setupPipButton();

        backbutton.setOnClickListener(v -> {
            finish();
        });

    }

    private void setupPlayer(){

        String url = getIntent().getStringExtra("url");
        player = new ExoPlayer.Builder(this).build();
        playerView.setPlayer(player);
        player.setMediaItem(MediaItem.fromUri(url));
        player.prepare();
        player.play();

    }
    private void setupFullscreenButton() {
        fullscreenbutton.setOnClickListener(v -> {
            if(!isFullscreen){
                enterFullscreen();
            }
            else {
                exitFullscreen();
            }
        });
    }

    private void enterFullscreen() {
        isFullscreen = true;

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        hideSystemUI();

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);

        fullscreenbutton.setImageResource(R.drawable.controls_fullscreen_exit);
    }

    private void exitFullscreen() {
        isFullscreen = false;

        showSystemUI();

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);

        fullscreenbutton.setImageResource(R.drawable.controls_fullscreen);
    }

    private void setupPipButton(){

        pipButton.setOnClickListener(v -> enterPipMode());

    }

    private void enterPipMode(){

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            Rational aspectRatio = new Rational(playerView.getWidth(), playerView.getHeight());

            PictureInPictureParams params = new PictureInPictureParams.Builder().setAspectRatio(aspectRatio).build();

            enterPictureInPictureMode(params);

        }

    }

    public void onUserLeaveHint() {
        super.onUserLeaveHint();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            enterPipMode();
        }
    }

    @OptIn(markerClass = UnstableApi.class)
    public void onPictureInPictureModeChanged(boolean isInPictureInPictureMode){

        super.onPictureInPictureModeChanged(isInPictureInPictureMode);

        isInPip = isInPictureInPictureMode;

        if(isInPictureInPictureMode){
            pipButton.setVisibility(View.GONE);
            playerView.hideController();
        }
        else {
            pipButton.setVisibility(View.VISIBLE);
            playerView.showController();
        }

    }

    private void hideSystemUI() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        );
    }

    private void showSystemUI() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_VISIBLE
        );
    }

    @Override
    protected void onStart() {
        super.onStart();
        player.setPlayWhenReady(true);
    }

    @Override
    protected void onResume() {
        super.onResume();
        hideSystemUI();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (!isInPip){
            player.pause();
        }
        player.setPlayWhenReady(false);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        player.release();
    }

}