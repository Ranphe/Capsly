package com.ranphe.capsly.ui.vault;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import android.content.Intent;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import com.ranphe.capsly.R;
import com.ranphe.capsly.data.model.Capsule;
import com.ranphe.capsly.data.repository.CapsuleRepository;
import com.ranphe.capsly.data.local.DatabaseHelper;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public final class CapsuleDetailActivity extends AppCompatActivity {
    public static final String EXTRA_CAPSULE_ID = "com.ranphe.capsly.extra.CAPSULE_ID";
    public static final String EXTRA_RECIPIENT_EMAIL = "com.ranphe.capsly.extra.RECIPIENT_EMAIL";
    private static final long DAY_MILLISECONDS = 86400000L;
    private static final long REFRESH_INTERVAL = 30000L;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Capsule capsule;
    private long createdAt = -1L;
    private int currentLayoutId = 0;
    private boolean isResumed = false;

    private final Runnable countdownUpdater = () -> {
        if (!isResumed) return;
        renderDetail();
        scheduleCountdown();
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        configureWindow();
        showLoading();
        loadRequestedCapsule();
    }

    private void configureWindow() {
        WindowCompat.enableEdgeToEdge(getWindow());
        WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView()).setAppearanceLightStatusBars(true);
    }

    @SuppressLint("SetTextI18n")
    private void showLoading() {
        TextView loadingText = new TextView(this);
        loadingText.setText("Cargando cápsula...");
        loadingText.setTextSize(16);
        loadingText.setTextColor(Color.parseColor("#46534A"));
        loadingText.setBackgroundColor(Color.parseColor("#FCF8F1"));
        loadingText.setGravity(Gravity.CENTER);
        setContentView(loadingText);
        applyWindowInsets();
    }

    private void loadRequestedCapsule() {
        long capsuleId = getIntent().getLongExtra(EXTRA_CAPSULE_ID, -1);
        String recipientEmail = getIntent().getStringExtra(EXTRA_RECIPIENT_EMAIL);
        if (capsuleId <= 0 || recipientEmail == null || recipientEmail.trim().isEmpty()) {
            Toast.makeText(this, "No se pudo identificar la cápsula", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        new Thread(() -> {
            Capsule loadedCapsule;
            long loadedCreatedAt = -1L;
            try {
                CapsuleRepository repository = new CapsuleRepository(getApplicationContext());
                loadedCapsule = repository.getReceivedCapsule(capsuleId, recipientEmail);
            } catch (RuntimeException exception) {
                loadedCapsule = null;
            }
            if (loadedCapsule != null) {
                try (DatabaseHelper database = new DatabaseHelper(getApplicationContext())) {
                    loadedCreatedAt = database.getReceivedCapsuleCreationDate(capsuleId, recipientEmail);
                } catch (RuntimeException exception) {
                    loadedCreatedAt = -1L;
                }
            }
            Capsule result = loadedCapsule;
            long creationResult = loadedCreatedAt;
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) return;
                if (result == null) {
                    Toast.makeText(this, "No se encontró esta cápsula en tu bóveda", Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }
                capsule = result;
                createdAt = creationResult;
                if (CapsuleAppearance.getStatus(capsule) == CapsuleAppearance.Status.OPENED) {
                    openUnlockedCapsule();
                    return;
                }
                renderDetail();
                scheduleCountdown();
            });
        }).start();
    }

    @SuppressLint("SetTextI18n")
    private void renderDetail() {
        if (capsule == null) return;
        long remainingMilliseconds = capsule.getOpeningDate() - System.currentTimeMillis();
        int layoutId = getLayoutForRemainingTime(remainingMilliseconds);
        if (currentLayoutId != layoutId) {
            currentLayoutId = layoutId;
            setContentView(layoutId);
            applyWindowInsets();
            bindControls(layoutId);
        }
        String openingDateText = formatDate(capsule.getOpeningDate());
        if (layoutId == R.layout.screen_capsule_locked) {
            TextView dateView = findViewById(R.id.tvFechaBloqueada);
            dateView.setText("Este recuerdo se abrirá el " + openingDateText + ".");
            updateCountdown(remainingMilliseconds, R.id.tvDiasBloqueada, R.id.tvHorasBloqueada, R.id.tvMinutosBloqueada);
        } else if (layoutId == R.layout.screen_capsule_upcoming) {
            TextView dateView = findViewById(R.id.tvFechaMuyPronto);
            dateView.setText("Esta cápsula se abrirá el " + openingDateText + ".");
            updateCountdown(remainingMilliseconds, R.id.tvDiasMuyPronto, R.id.tvHorasMuyPronto, R.id.tvMinutosMuyPronto);
        } else {
            TextView messageView = findViewById(R.id.tvMensajeCapsulaAbierta);
            if (createdAt > 0L) {
                messageView.setText("Este recuerdo fue creado el " + formatDate(createdAt) + ".");
            } else {
                messageView.setText("Tu cápsula ya está disponible para abrirse.");
            }
        }
    }

    private int getLayoutForRemainingTime(long remainingMilliseconds) {
        if (remainingMilliseconds <= 0) return R.layout.screen_capsule_available;
        if (remainingMilliseconds <= 7L * DAY_MILLISECONDS) return R.layout.screen_capsule_upcoming;
        return R.layout.screen_capsule_locked;
    }

    private void bindControls(int layoutId) {
        int backButtonId;
        if (layoutId == R.layout.screen_capsule_locked) {
            backButtonId = R.id.btnRegresarBloqueada;
        } else if (layoutId == R.layout.screen_capsule_upcoming) {
            backButtonId = R.id.btnRegresarMuyPronto;
        } else {
            backButtonId = R.id.btnRegresarAbierta;
            findViewById(R.id.btnVerCapsulaAbierta).setOnClickListener(view -> openUnlockedCapsule());
        }
        findViewById(backButtonId).setOnClickListener(view -> finish());
    }

    private void updateCountdown(long remainingMilliseconds, int daysViewId, int hoursViewId, int minutesViewId) {
        long totalMinutes = (Math.max(0, remainingMilliseconds) + 59999L) / 60000L;
        long days = totalMinutes / 1440L;
        long hours = (totalMinutes % 1440L) / 60L;
        long minutes = totalMinutes % 60L;
        String dayLabel = days == 1 ? "día" : "días";
        String hourLabel = hours == 1 ? "hora" : "horas";
        String minuteLabel = minutes == 1 ? "minuto" : "minutos";
        TextView daysView = findViewById(daysViewId);
        TextView hoursView = findViewById(hoursViewId);
        TextView minutesView = findViewById(minutesViewId);
        daysView.setText(getString(R.string.countdown_value, days, dayLabel));
        hoursView.setText(getString(R.string.countdown_value, hours, hourLabel));
        minutesView.setText(getString(R.string.countdown_value, minutes, minuteLabel));
    }

    private String formatDate(long timestamp) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("d 'de' MMMM 'de' yyyy", new Locale("es", "MX"));
        return dateFormat.format(new Date(timestamp));
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

    private void scheduleCountdown() {
        handler.removeCallbacks(countdownUpdater);
        if (isResumed && capsule != null) handler.postDelayed(countdownUpdater, REFRESH_INTERVAL);
    }

    private void openUnlockedCapsule() {
        if (capsule == null || capsule.getOpeningDate() > System.currentTimeMillis()) {
            renderDetail();
            return;
        }
        String recipientEmail = getIntent().getStringExtra(EXTRA_RECIPIENT_EMAIL);
        if (recipientEmail == null || recipientEmail.trim().isEmpty()) {
            Toast.makeText(this, "No se pudo identificar al destinatario", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(this, UnlockedCapsuleActivity.class);
        intent.putExtra(UnlockedCapsuleActivity.EXTRA_CAPSULE_ID, capsule.getId());
        intent.putExtra(UnlockedCapsuleActivity.EXTRA_RECIPIENT_EMAIL, recipientEmail);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        isResumed = true;
        renderDetail();
        scheduleCountdown();
    }

    @Override
    protected void onPause() {
        isResumed = false;
        handler.removeCallbacks(countdownUpdater);
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacks(countdownUpdater);
        super.onDestroy();
    }
}