package com.example.remediaplayer;

import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.widget.ImageButton;
import android.widget.Switch;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

public class SettingsActivity extends AppCompatActivity {
    ImageButton backButton;
    Switch autoPlaySwitch, hideUiSwitch, darkModeSwitch;
    SharedPreferences prefs;

    Handler handler = new Handler();
    Runnable hideRunnable = this::hideSystemUI;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        prefs = getSharedPreferences("settings", MODE_PRIVATE);
        boolean darkMode = prefs.getBoolean("dark_mode", false);

        if (darkMode)
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        else
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        autoPlaySwitch = findViewById(R.id.autoPlaySwitch);
        hideUiSwitch = findViewById(R.id.hideUiSwitch);
        darkModeSwitch = findViewById(R.id.darkModeSwitch);
        backButton = findViewById(R.id.back_button);

        loadSettings();
        setupListeners();
        setupSystemUIBehavior();
        backButton.setOnClickListener(v -> finish());

    }


    private void loadSettings() {
        autoPlaySwitch.setChecked(prefs.getBoolean("auto_play", true));
        hideUiSwitch.setChecked(prefs.getBoolean("hide_ui", true));
        darkModeSwitch.setChecked(prefs.getBoolean("dark_mode", false));
    }

    private void setupListeners() {

        autoPlaySwitch.setOnCheckedChangeListener((buttonView, isChecked) ->
                prefs.edit().putBoolean("auto_play", isChecked).apply()
        );

        hideUiSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean("hide_ui", isChecked).apply();
            applyHideUISetting();
        });

        darkModeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean("dark_mode", isChecked).apply();
            recreate(); // refresh UI with new theme
        });
    }

    private void setupSystemUIBehavior() {

        boolean hideUI = prefs.getBoolean("hide_ui", true);

        if (hideUI) {
            handler.postDelayed(hideRunnable, 1000);
        }

        findViewById(android.R.id.content).setOnTouchListener((v, event) -> {

            boolean shouldHide = prefs.getBoolean("hide_ui", true);

            if (!shouldHide) {
                showSystemUI();
                return false;
            }

            showSystemUI(); // show temporarily
            handler.removeCallbacks(hideRunnable);
            handler.postDelayed(hideRunnable, 2000);

            return false;
        });
    }

    private void applyHideUISetting() {
        boolean hideUI = prefs.getBoolean("hide_ui", true);

        if (hideUI) {
            hideSystemUI();
        } else {
            showSystemUI();
        }
    }

    private void hideSystemUI() {
        boolean shouldHide = prefs.getBoolean("hide_ui", true);
        if (!shouldHide) return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {

            getWindow().setDecorFitsSystemWindows(false);
            WindowInsetsController c = getWindow().getInsetsController();

            if (c != null) {
                c.hide(WindowInsets.Type.systemBars());
                c.setSystemBarsBehavior(
                        WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                );
            }
        }
    }

    private void showSystemUI() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {

            getWindow().setDecorFitsSystemWindows(true);
            WindowInsetsController c = getWindow().getInsetsController();
            if (c != null) c.show(WindowInsets.Type.systemBars());
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        applyHideUISetting();
    }
}
