package com.ranphe.capsly;

import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import android.content.Intent;
import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.viewpager2.widget.ViewPager2;
import androidx.appcompat.app.AppCompatDelegate;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.ranphe.capsly.data.local.DatabaseHelper;
import com.ranphe.capsly.media.audio.AudioController;
import com.ranphe.capsly.media.photo.PhotoController;
import com.ranphe.capsly.media.video.VideoController;
import com.ranphe.capsly.session.UserSession;
import com.ranphe.capsly.ui.access.AccessAdapter;
import com.ranphe.capsly.ui.create.CreateMemoryScreen;
import com.ranphe.capsly.ui.home.HomeScreen;
import com.ranphe.capsly.ui.inspiration.InspirationActivity;
import com.ranphe.capsly.ui.welcome.WelcomeScreen;
import android.widget.TextView;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    public static final String EXTRA_OPEN_VAULT = "com.ranphe.capsly.extra.OPEN_VAULT";
    private boolean isOnAccessScreen = false;
    private boolean isOnCreateMemoryScreen = false;
    private boolean isOnCapsuleCreatedScreen = false;
    private final UserSession userSession = new UserSession();
    private PhotoController photoController;
    private VideoController videoController;
    private AudioController audioController;
    private CreateMemoryScreen createMemoryScreen;
    private final ActivityResultLauncher<PickVisualMediaRequest> photoGalleryLauncher = registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {
        if (photoController != null && isCreatingFormat("Foto")) photoController.onGalleryResult(uri);
    });
    private final ActivityResultLauncher<Uri> photoCameraLauncher = registerForActivityResult(new ActivityResultContracts.TakePicture(), saved -> {
        if (photoController != null) photoController.onCameraResult(Boolean.TRUE.equals(saved), isCreatingFormat("Foto"));
    });
    private final ActivityResultLauncher<PickVisualMediaRequest> videoGalleryLauncher = registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {
        if (videoController != null && isCreatingFormat("Video")) videoController.onGalleryResult(uri);
    });
    private final ActivityResultLauncher<Uri> videoCameraLauncher = registerForActivityResult(new ActivityResultContracts.CaptureVideo(), saved -> {
        if (videoController != null) videoController.onCameraResult(Boolean.TRUE.equals(saved), isCreatingFormat("Video"));
    });
    private final ActivityResultLauncher<String[]> audioGalleryLauncher = registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
        if (audioController != null && isCreatingFormat("Audio")) audioController.onGalleryResult(uri);
    });
    private final ActivityResultLauncher<String> microphonePermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
        if (audioController != null) audioController.onMicrophonePermissionResult(Boolean.TRUE.equals(granted), isCreatingFormat("Audio"));
    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        super.onCreate(savedInstanceState);
        initializeControllers();
        initializeDatabase();
        configureWindow();
        showWelcome();
        configureBackNavigation();
    }

    private void initializeControllers() {
        photoController = new PhotoController(this, photoGalleryLauncher, photoCameraLauncher);
        videoController = new VideoController(this, videoGalleryLauncher, videoCameraLauncher);
        audioController = new AudioController(this, audioGalleryLauncher, microphonePermissionLauncher);
    }

    private void initializeDatabase() {
        DatabaseHelper databaseHelper = new DatabaseHelper(this);
        databaseHelper.getWritableDatabase().close();
    }

    private void configureWindow() {
        WindowCompat.enableEdgeToEdge(getWindow());
        WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView()).setAppearanceLightStatusBars(true);
    }

    private void configureBackNavigation() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (isOnCapsuleCreatedScreen) {
                    showHome(userSession.getName());
                } else if (isOnCreateMemoryScreen) {
                    showHome(userSession.getName());
                } else if (isOnAccessScreen) {
                    showWelcome();
                } else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });
    }

    private void applyWindowInsets() {
        ViewGroup content = findViewById(android.R.id.content);
        View screen = content.getChildAt(0);
        ViewCompat.setOnApplyWindowInsetsListener(screen, (view, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            Insets keyboard = insets.getInsets(WindowInsetsCompat.Type.ime());
            int bottomPadding = Math.max(systemBars.bottom, keyboard.bottom);
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, bottomPadding);
            return insets;
        });
        ViewCompat.requestApplyInsets(screen);
    }

    private void showWelcome() {
        isOnAccessScreen = false;
        setContentView(R.layout.page_welcome);
        applyWindowInsets();
        WelcomeScreen.bind(this, () -> showAccess(0), () -> showAccess(1));
    }

    private void showAccess(int initialTab) {
        isOnAccessScreen = true;
        setContentView(R.layout.item_auth);
        applyWindowInsets();
        TabLayout tabs = findViewById(R.id.tabsAcceso);
        ViewPager2 pager = findViewById(R.id.pagerAcceso);
        pager.setAdapter(new AccessAdapter(this, pager, (name, email) -> {
            if (!isOnAccessScreen) return;
            userSession.signIn(name, email);
            showHome(name);
        }));
        new TabLayoutMediator(tabs, pager, (tab, position) -> tab.setText(position == 0 ? "Iniciar sesión" : "Crear cuenta")).attach();
        pager.setCurrentItem(initialTab, false);
    }

    private void showHome(String name) {
        if (createMemoryScreen != null && createMemoryScreen.isSaving()) {
            Toast.makeText(this, "Se está guardando tu cápsula", Toast.LENGTH_SHORT).show();
            return;
        }
        clearMediaSelections();
        createMemoryScreen = null;
        isOnCreateMemoryScreen = false;
        isOnAccessScreen = false;
        isOnCapsuleCreatedScreen = false;
        setContentView(R.layout.item_home_navigation);
        applyWindowInsets();
        HomeScreen.bind(this, name, userSession.getEmail(), this::showCreateMemory, this::signOut, userSession::updateName);
    }

    private void signOut() {
        clearMediaSelections();
        createMemoryScreen = null;
        isOnCreateMemoryScreen = false;
        isOnAccessScreen = false;
        userSession.signOut();
        showWelcome();
    }

    private void showCreateMemory() {
        isOnCreateMemoryScreen = true;
        isOnCapsuleCreatedScreen = false;
        isOnAccessScreen = false;
        setContentView(R.layout.page_create_memory);
        applyWindowInsets();
        createMemoryScreen = new CreateMemoryScreen(this, userSession, photoController, videoController, audioController, () -> showHome(userSession.getName()), this::showCapsuleCreated);
        findViewById(R.id.btnInspiracion).setOnClickListener(view -> showInspiration());
    }

    private void showInspiration() {
        if (createMemoryScreen == null || createMemoryScreen.isSaving()) {
            return;
        }

        Intent intent = new Intent(this, InspirationActivity.class);
        intent.putExtra(InspirationActivity.EXTRA_USER_EMAIL, userSession.getEmail());
        startActivity(intent);
    }

    private void showCapsuleCreated(String recipientEmail, long openingDate) {
        clearMediaSelections();
        createMemoryScreen = null;
        isOnCreateMemoryScreen = false;
        isOnAccessScreen = false;
        isOnCapsuleCreatedScreen = true;
        setContentView(R.layout.screen_capsule_created);
        applyWindowInsets();
        TextView recipientView = findViewById(R.id.tvDestinatarioCapsulaLista);
        TextView dateView = findViewById(R.id.tvFechaAperturaCapsulaLista);
        SimpleDateFormat dateFormat = new SimpleDateFormat("d 'de' MMMM 'de' yyyy", new Locale("es", "MX"));
        recipientView.setText("Destinatario: " + recipientEmail);
        dateView.setText("Apertura: " + dateFormat.format(new Date(openingDate)));
        findViewById(R.id.btnRegresarCapsulaLista).setOnClickListener(view -> showHome(userSession.getName()));
        findViewById(R.id.btnVolverInicioCapsulaLista).setOnClickListener(view -> showHome(userSession.getName()));
        findViewById(R.id.btnCrearOtraCapsulaLista).setOnClickListener(view -> showCreateMemory());
    }

    private boolean isCreatingFormat(String format) {
        return isOnCreateMemoryScreen && createMemoryScreen != null && format.equals(createMemoryScreen.getSelectedFormat());
    }

    private void clearMediaSelections() {
        if (photoController != null) photoController.clearSelection();
        if (videoController != null) videoController.clearSelection();
        if (audioController != null) audioController.clearSelection();
    }

    private void pauseMedia() {
        if (videoController != null) videoController.pausePlayback();
        if (audioController != null) audioController.onStop();
    }

    private void releaseMedia() {
        if (videoController != null) videoController.release();
        if (audioController != null) audioController.release();
    }

    private void openVaultIfRequested() {
        Intent intent = getIntent();
        if (intent == null || !intent.getBooleanExtra(EXTRA_OPEN_VAULT, false)) return;
        intent.removeExtra(EXTRA_OPEN_VAULT);
        if (!userSession.isSignedIn()) return;
        showHome(userSession.getName());
        ViewPager2 pager = findViewById(R.id.pagerPrincipal);
        if (pager != null) pager.setCurrentItem(1, false);
    }

    @Override
    protected void onNewIntent(@NonNull Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        openVaultIfRequested();
    }

    @Override
    protected void onResume() {
        super.onResume();
        openVaultIfRequested();
    }


    @Override
    protected void onStop() {
        pauseMedia();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        releaseMedia();
        super.onDestroy();
    }
}
