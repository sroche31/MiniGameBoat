package com.enac.minigame;

import android.Manifest;                          // +
import android.content.pm.PackageManager;         // +
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;               // +
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;          // +
import androidx.core.content.ContextCompat;       // +
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {

    private static final int MIC_PERMISSION_REQUEST_CODE = 1;   // +
    private GameView jeu;                                         // + (devient un champ)

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        jeu = new GameView(this);                                 // + (plus de "GameView" devant)
        setContentView(jeu);

        // Barres système + encoche -> padding, utilisé par GameView pour placer le texte
        ViewCompat.setOnApplyWindowInsetsListener(jeu, (v, insets) -> {
            Insets bars = insets.getInsets(
                    WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });

        // + Demande la permission micro si elle n'est pas encore accordée
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    MIC_PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        jeu.pause();
    }

}