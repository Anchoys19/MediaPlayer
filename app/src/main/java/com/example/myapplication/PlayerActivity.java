package com.example.myapplication;

import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;

import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class PlayerActivity extends AppCompatActivity {

    private ExoPlayer player;
    private PlayerView playerView;

    private ImageButton btnPlayPause, btnStop;
    private SeekBar seekBar;
    private TextView tvCurrentTime, tvTotalTime, tvTitle;

    private boolean isVideo;
    private String uriString, urlString;

    private ScheduledExecutorService executor;
    private ScheduledFuture<?> seekUpdater;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        isVideo   = getIntent().getBooleanExtra("is_video", false);
        uriString = getIntent().getStringExtra("uri");
        urlString = getIntent().getStringExtra("url");

        playerView   = findViewById(R.id.playerView);
        btnPlayPause = findViewById(R.id.btnPlayPause);
        btnStop      = findViewById(R.id.btnStop);
        seekBar      = findViewById(R.id.seekBar);
        tvCurrentTime = findViewById(R.id.tvCurrentTime);
        tvTotalTime   = findViewById(R.id.tvTotalTime);
        tvTitle       = findViewById(R.id.tvTitle);

        // Для аудіо ховаємо поверхню відео
        if (!isVideo) {
            playerView.setVisibility(View.GONE);
            tvTitle.setText("Аудіоплеєр");
        } else {
            tvTitle.setText("Відеоплеєр");
        }

        initPlayer();
        setupControls();
    }

    private void initPlayer() {
        player = new ExoPlayer.Builder(this).build();
        playerView.setPlayer(player);

        // Визначаємо джерело медіа
        MediaItem mediaItem;
        if (uriString != null) {
            mediaItem = MediaItem.fromUri(Uri.parse(uriString));
        } else if (urlString != null) {
            mediaItem = MediaItem.fromUri(urlString);
        } else {
            Toast.makeText(this, "Помилка: немає джерела!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        player.setMediaItem(mediaItem);
        player.prepare();
        player.setPlayWhenReady(true);

        player.addListener(new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int state) {
                if (state == Player.STATE_READY) {
                    long duration = player.getDuration();
                    seekBar.setMax((int) duration);
                    tvTotalTime.setText(formatTime(duration));
                }
                if (state == Player.STATE_ENDED) {
                    btnPlayPause.setImageResource(R.drawable.ic_play);
                    stopSeekBarUpdater();
                }
            }

            @Override
            public void onIsPlayingChanged(boolean isPlaying) {
                if (isPlaying) {
                    btnPlayPause.setImageResource(R.drawable.ic_pause);
                    startSeekBarUpdater();
                } else {
                    btnPlayPause.setImageResource(R.drawable.ic_play);
                    stopSeekBarUpdater();
                }
            }

            @Override
            public void onPlayerError(androidx.media3.common.PlaybackException error) {
                Toast.makeText(PlayerActivity.this,
                        "Помилка відтворення: " + error.getMessage(),
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    private void setupControls() {
        // Play / Pause
        btnPlayPause.setOnClickListener(v -> {
            if (player.isPlaying()) {
                player.pause();
            } else {
                if (player.getPlaybackState() == Player.STATE_ENDED) {
                    player.seekTo(0);
                }
                player.play();
            }
        });

        // Stop — зупинити та перемотати на початок
        btnStop.setOnClickListener(v -> {
            player.stop();
            player.seekTo(0);
            seekBar.setProgress(0);
            tvCurrentTime.setText("0:00");
            btnPlayPause.setImageResource(R.drawable.ic_play);
        });

        // SeekBar — перемотування
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar sb, int progress, boolean fromUser) {
                if (fromUser) {
                    player.seekTo(progress);
                    tvCurrentTime.setText(formatTime(progress));
                }
            }
            @Override public void onStartTrackingTouch(SeekBar sb) { stopSeekBarUpdater(); }
            @Override public void onStopTrackingTouch(SeekBar sb)  {
                if (player.isPlaying()) startSeekBarUpdater();
            }
        });
    }

    private void startSeekBarUpdater() {
        stopSeekBarUpdater();
        executor = Executors.newSingleThreadScheduledExecutor();
        seekUpdater = executor.scheduleAtFixedRate(() -> runOnUiThread(() -> {
            if (player != null && player.isPlaying()) {
                long pos = player.getCurrentPosition();
                seekBar.setProgress((int) pos);
                tvCurrentTime.setText(formatTime(pos));
            }
        }), 0, 500, TimeUnit.MILLISECONDS);
    }

    private void stopSeekBarUpdater() {
        if (seekUpdater != null) { seekUpdater.cancel(false); seekUpdater = null; }
        if (executor != null)    { executor.shutdown(); executor = null; }
    }

    private String formatTime(long ms) {
        long seconds = (ms / 1000) % 60;
        long minutes = (ms / 1000) / 60;
        return String.format(Locale.getDefault(), "%d:%02d", minutes, seconds);
    }

    @Override
    protected void onPause()   { super.onPause();   if (player != null) player.pause(); }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopSeekBarUpdater();
        if (player != null) { player.release(); player = null; }
    }
}