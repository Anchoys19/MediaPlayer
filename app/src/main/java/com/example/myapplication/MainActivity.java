package com.example.myapplication;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {

    private RadioButton rbAudio, rbVideo;
    private EditText etUrl;
    private Button btnPickFile, btnLoadUrl, btnOpenPlayer;
    private TextView tvSelectedFile;

    private Uri selectedUri = null;
    private String selectedUrl = null;
    private boolean isVideo = false;

    // Лаунчер для вибору файлу через файловий менеджер
    private final ActivityResultLauncher<String[]> filePicker =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
                if (uri != null) {
                    selectedUri = uri;
                    selectedUrl = null;
                    // Зберігаємо доступ до URI між сесіями
                    getContentResolver().takePersistableUriPermission(
                            uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    tvSelectedFile.setText("Файл: " + uri.getLastPathSegment());
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        rbAudio       = findViewById(R.id.rbAudio);
        rbVideo       = findViewById(R.id.rbVideo);
        etUrl         = findViewById(R.id.etUrl);
        btnPickFile   = findViewById(R.id.btnPickFile);
        btnLoadUrl    = findViewById(R.id.btnLoadUrl);
        btnOpenPlayer = findViewById(R.id.btnOpenPlayer);
        tvSelectedFile = findViewById(R.id.tvSelectedFile);

        requestStoragePermission();

        btnPickFile.setOnClickListener(v -> pickFile());

        btnLoadUrl.setOnClickListener(v -> {
            String url = etUrl.getText().toString().trim();
            if (url.isEmpty()) {
                Toast.makeText(this, "Введіть URL!", Toast.LENGTH_SHORT).show();
                return;
            }
            selectedUrl = url;
            selectedUri = null;
            tvSelectedFile.setText("URL: " + url);
        });

        btnOpenPlayer.setOnClickListener(v -> openPlayer());
    }

    private void pickFile() {
        isVideo = rbVideo.isChecked();
        if (isVideo) {
            filePicker.launch(new String[]{"video/*"});
        } else {
            filePicker.launch(new String[]{"audio/*"});
        }
    }

    private void openPlayer() {
        isVideo = rbVideo.isChecked();

        if (selectedUri == null && selectedUrl == null) {
            Toast.makeText(this, "Оберіть файл або введіть URL!", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(this, PlayerActivity.class);
        intent.putExtra("is_video", isVideo);
        if (selectedUri != null) {
            intent.putExtra("uri", selectedUri.toString());
        } else {
            intent.putExtra("url", selectedUrl);
        }
        startActivity(intent);
    }

    private void requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ — окремі дозволи на медіа
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.READ_MEDIA_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{
                                Manifest.permission.READ_MEDIA_AUDIO,
                                Manifest.permission.READ_MEDIA_VIDEO
                        }, 1);
            }
        } else {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
            }
        }
    }
}