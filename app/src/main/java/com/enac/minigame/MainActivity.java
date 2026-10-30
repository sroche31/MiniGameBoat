package com.enac.minigame;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {

    private GameView gameView;

    // Si la permission micro est refusée, on prévient que le boost se fera avec le bouton
    private final ActivityResultLauncher<String> micPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(), granted -> {
                if (!granted) {
                    Toast.makeText(this, R.string.mic_denied, Toast.LENGTH_LONG).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        gameView = new GameView(this, this::requestMicPermission);
        setContentView(gameView);

        // Barres système + encoche -> padding, utilisé par GameView pour placer le texte
        ViewCompat.setOnApplyWindowInsetsListener(gameView, (v, insets) -> {
            Insets bars = insets.getInsets(
                    WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });

        // + Demande la permission micro si elle n'est pas encore accordée
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            requestMicPermission();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        gameView.pause();
    }

    private void requestMicPermission() {
        micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO);
    }

}