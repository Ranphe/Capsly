package com.ranphe.capsly.ui.vault;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import android.content.Intent;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import com.ranphe.capsly.R;
import com.ranphe.capsly.data.model.UnlockedCapsule;
import com.ranphe.capsly.data.repository.CapsuleRepository;
import com.ranphe.capsly.media.files.CapsuleStorage;
import com.ranphe.capsly.media.playback.UnlockedMediaController;
import com.ranphe.capsly.MainActivity;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public final class UnlockedCapsuleActivity extends AppCompatActivity {
    public static final String EXTRA_CAPSULE_ID = "com.ranphe.capsly.extra.UNLOCKED_CAPSULE_ID";
    public static final String EXTRA_RECIPIENT_EMAIL = "com.ranphe.capsly.extra.UNLOCKED_RECIPIENT_EMAIL";
    private UnlockedCapsule capsule;
    private UnlockedMediaController mediaController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        configureWindow();
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                returnToVault();
            }
        });
        showLoading();
        loadCapsule();
    }

    private void configureWindow() {
        WindowCompat.enableEdgeToEdge(getWindow());
        WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView()).setAppearanceLightStatusBars(true);
    }

    @SuppressLint("SetTextI18n")
    private void showLoading() {
        TextView loading = new TextView(this);
        loading.setText("Cargando recuerdo...");
        loading.setTextSize(16);
        loading.setTextColor(Color.parseColor("#46534A"));
        loading.setBackgroundColor(Color.parseColor("#FCF8F1"));
        loading.setGravity(Gravity.CENTER);
        setContentView(loading);
        applyWindowInsets();
    }

    private void loadCapsule() {
        long capsuleId = getIntent().getLongExtra(EXTRA_CAPSULE_ID, -1);
        String recipientEmail = getIntent().getStringExtra(EXTRA_RECIPIENT_EMAIL);
        if (capsuleId <= 0 || recipientEmail == null || recipientEmail.trim().isEmpty()) {
            Toast.makeText(this, "No se pudo identificar el recuerdo", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        new Thread(() -> {
            UnlockedCapsule loadedCapsule;
            File mediaFile = null;
            try {
                CapsuleRepository repository = new CapsuleRepository(getApplicationContext());
                loadedCapsule = repository.getUnlockedCapsule(capsuleId, recipientEmail);
                if (loadedCapsule != null) {
                    CapsuleStorage storage = new CapsuleStorage(getApplicationContext());
                    try {
                        mediaFile = storage.resolveStoredFile(loadedCapsule.getFilePath());
                    } catch (IOException ignored) {
                    }
                }
                if (loadedCapsule != null && loadedCapsule.getOpeningDate() <= System.currentTimeMillis()) repository.markCapsuleOpened(capsuleId, recipientEmail);
            } catch (RuntimeException exception) {
                loadedCapsule = null;
            }
            UnlockedCapsule result = loadedCapsule;
            File resultFile = mediaFile;
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) return;
                if (result == null || result.getOpeningDate() > System.currentTimeMillis()) {
                    Toast.makeText(this, "Este recuerdo todavía no está disponible o no pertenece a tu bóveda", Toast.LENGTH_LONG).show();
                    finish();
                    return;
                }
                capsule = result;
                showUnlockedCapsule(result, resultFile);
            });
        }).start();
    }

    @SuppressLint("SetTextI18n")
    private void showUnlockedCapsule(UnlockedCapsule unlockedCapsule, File mediaFile) {
        setContentView(R.layout.screen_capsule_content);
        applyWindowInsets();
        TextView titleView = findViewById(R.id.tvTituloRecuerdoAbierto);
        TextView dateView = findViewById(R.id.tvFechaRecuerdoAbierto);
        TextView messageView = findViewById(R.id.tvTextoRecuerdoAbierto);
        View messageCard = findViewById(R.id.cardTextoRecuerdo);
        View backButton = findViewById(R.id.btnRegresarRecuerdo);
        titleView.setText(unlockedCapsule.getTitle());
        if (unlockedCapsule.getCreatedAt() > 0L) {
            dateView.setText(formatDate(unlockedCapsule.getCreatedAt()));
        } else {
            dateView.setText("Fecha de creación no disponible.");
        }
        messageView.setText(unlockedCapsule.getMessage() == null ? "" : unlockedCapsule.getMessage());
        messageCard.setVisibility(View.VISIBLE);
        backButton.setOnClickListener(view -> returnToVault());
        mediaController = new UnlockedMediaController(this);
        mediaController.show(unlockedCapsule.getFormat(), mediaFile);
    }

    private String formatDate(long timestamp) {
        SimpleDateFormat formatter = new SimpleDateFormat("d 'de' MMMM 'de' yyyy", new Locale("es", "MX"));
        return formatter.format(new Date(timestamp));
    }

    private void applyWindowInsets() {
        ViewGroup content = findViewById(android.R.id.content);
        View screen = content.getChildAt(0);
        if (screen == null) return;
        ViewCompat.setOnApplyWindowInsetsListener(screen, (view, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        ViewCompat.requestApplyInsets(screen);
    }

    private void returnToVault() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra(MainActivity.EXTRA_OPEN_VAULT, true);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (capsule != null && capsule.getOpeningDate() > System.currentTimeMillis()) {
            Toast.makeText(this, "Este recuerdo todavía está bloqueado", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    @Override
    protected void onStop() {
        if (mediaController != null) mediaController.pause();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        if (mediaController != null) mediaController.release();
        super.onDestroy();
    }
}