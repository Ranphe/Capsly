package com.ranphe.capsly.ui.vault;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.view.View;
import android.widget.TextView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.chip.Chip;
import com.ranphe.capsly.R;
import com.ranphe.capsly.data.model.Capsule;
import com.ranphe.capsly.data.repository.CapsuleRepository;
import java.util.ArrayList;
import java.util.List;

public final class VaultScreen {
    private static final long DAY_MILLISECONDS = 86400000L;
    private static final long UPCOMING_WINDOW_MILLISECONDS = 7L * DAY_MILLISECONDS;
    private enum Filter {
        ALL,
        OPEN,
        UPCOMING,
        LOCKED
    }
    private final Activity activity;
    private final String recipientEmail;
    private final CapsuleRepository repository;
    private final List<Capsule> allCapsules = new ArrayList<>();
    private final RecyclerView capsuleList;
    private final TextView emptyMessage;
    private final Chip allChip;
    private final Chip openChip;
    private final Chip upcomingChip;
    private final Chip lockedChip;
    private final CapsuleAdapter adapter;
    private Filter selectedFilter = Filter.ALL;
    private boolean loaded = false;
    private boolean disposed = false;
    private boolean loadFailed = false;

    @SuppressLint("SetTextI18n")
    public VaultScreen(Activity activity, View root, String recipientEmail) {
        this.activity = activity;
        this.recipientEmail = recipientEmail;
        this.repository = new CapsuleRepository(activity.getApplicationContext());
        capsuleList = root.findViewById(R.id.listaCapsulasBoveda);
        emptyMessage = root.findViewById(R.id.tvBovedaVacia);
        allChip = root.findViewById(R.id.filtroTodas);
        openChip = root.findViewById(R.id.filtroAbiertas);
        upcomingChip = root.findViewById(R.id.filtroProximas);
        lockedChip = root.findViewById(R.id.filtroBloqueadas);
        adapter = new CapsuleAdapter(this::onCapsuleSelected);
        capsuleList.setLayoutManager(new LinearLayoutManager(activity));
        capsuleList.setAdapter(adapter);
        allChip.setOnClickListener(view -> selectFilter(Filter.ALL));
        openChip.setOnClickListener(view -> selectFilter(Filter.OPEN));
        upcomingChip.setOnClickListener(view -> selectFilter(Filter.UPCOMING));
        lockedChip.setOnClickListener(view -> selectFilter(Filter.LOCKED));
        emptyMessage.setText("Cargando cápsulas...");
        loadCapsules();
    }

    private void loadCapsules() {
        new Thread(() -> {
            List<Capsule> results;
            boolean failed = false;
            try {
                results = repository.getReceivedCapsules(recipientEmail);
            } catch (RuntimeException exception) {
                results = new ArrayList<>();
                failed = true;
            }
            List<Capsule> loadedCapsules = results;
            boolean loadingFailed = failed;
            activity.runOnUiThread(() -> {
                if (disposed || activity.isFinishing() || activity.isDestroyed()) return;
                allCapsules.clear();
                allCapsules.addAll(loadedCapsules);
                loaded = true;
                loadFailed = loadingFailed;
                render();
            });
        }).start();
    }

    private void selectFilter(Filter filter) {
        selectedFilter = filter;
        render();
    }

    private Filter getCapsuleStatus(Capsule capsule, long now) {
        long remainingTime = capsule.getOpeningDate() - now;
        if (remainingTime <= 0) return Filter.OPEN;
        if (remainingTime <= UPCOMING_WINDOW_MILLISECONDS) return Filter.UPCOMING;
        return Filter.LOCKED;
    }

    @SuppressLint("SetTextI18n")
    private void render() {
        if (!loaded) {
            capsuleList.setVisibility(View.GONE);
            emptyMessage.setVisibility(View.VISIBLE);
            emptyMessage.setText("Cargando cápsulas...");
            return;
        }
        if (loadFailed) {
            capsuleList.setVisibility(View.GONE);
            emptyMessage.setVisibility(View.VISIBLE);
            emptyMessage.setText("No se pudieron cargar las cápsulas");
            return;
        }
        long now = System.currentTimeMillis();
        int openCount = 0;
        int upcomingCount = 0;
        int lockedCount = 0;
        List<Capsule> visibleCapsules = new ArrayList<>();
        for (Capsule capsule : allCapsules) {
            Filter capsuleStatus = getCapsuleStatus(capsule, now);
            switch (capsuleStatus) {
                case OPEN:
                    openCount++;
                    break;
                case UPCOMING:
                    upcomingCount++;
                    break;
                case LOCKED:
                    lockedCount++;
                    break;
                default:
                    break;
            }
            if (selectedFilter == Filter.ALL || selectedFilter == capsuleStatus) visibleCapsules.add(capsule);
        }
        allChip.setText("Todas (" + allCapsules.size() + ")");
        openChip.setText("Abiertas (" + openCount + ")");
        upcomingChip.setText("Próximas (" + upcomingCount + ")");
        lockedChip.setText("Bloqueadas (" + lockedCount + ")");
        adapter.setCapsules(visibleCapsules);
        boolean empty = visibleCapsules.isEmpty();
        capsuleList.setVisibility(empty ? View.GONE : View.VISIBLE);
        emptyMessage.setVisibility(empty ? View.VISIBLE : View.GONE);
        if (empty) {
            switch (selectedFilter) {
                case OPEN:
                    emptyMessage.setText("No tienes cápsulas abiertas.");
                    break;
                case UPCOMING:
                    emptyMessage.setText("No tienes cápsulas próximas en los siguientes 7 días.");
                    break;
                case LOCKED:
                    emptyMessage.setText("No tienes cápsulas bloqueadas.");
                    break;
                default:
                    emptyMessage.setText("Aún no has recibido cápsulas.");
                    break;
            }
        }
    }

    private void onCapsuleSelected(Capsule capsule) {
        Intent intent = new Intent(activity, CapsuleDetailActivity.class);
        intent.putExtra(CapsuleDetailActivity.EXTRA_CAPSULE_ID, capsule.getId());
        intent.putExtra(CapsuleDetailActivity.EXTRA_RECIPIENT_EMAIL, recipientEmail);
        activity.startActivity(intent);
    }

    public void dispose() {
        disposed = true;
    }
}