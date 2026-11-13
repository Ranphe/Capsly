package com.ranphe.capsly.ui.home;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.google.android.material.card.MaterialCardView;
import com.ranphe.capsly.R;
import com.ranphe.capsly.data.model.Capsule;
import com.ranphe.capsly.data.repository.CapsuleRepository;
import com.ranphe.capsly.ui.vault.CapsuleDetailActivity;
import com.ranphe.capsly.ui.vault.CapsuleAppearance;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public final class HomeDashboardScreen {
    private static final long DAY_MILLISECONDS = 86400000L;
    private static final long UPCOMING_WINDOW_MILLISECONDS = 7L * DAY_MILLISECONDS;
    private final Activity activity;
    private final String recipientEmail;
    private final CapsuleRepository repository;
    private final TextView capsuleCount;
    private final MaterialCardView emptyCard;
    private final TextView emptyMessage;
    private final LinearLayout upcomingContainer;
    private boolean disposed = false;

    @SuppressLint("SetTextI18n")
    public HomeDashboardScreen(Activity activity, View root, String userName, String recipientEmail, Runnable onCreateCapsule, Runnable onOpenVault, Runnable onOpenProfile) {
        this.activity = activity;
        this.recipientEmail = recipientEmail;
        this.repository = new CapsuleRepository(activity.getApplicationContext());
        TextView welcomeText = root.findViewById(R.id.tvBienvenida);
        welcomeText.setText("¡Hola, " + userName + "!");
        root.findViewById(R.id.btnCrearCapsula).setOnClickListener(view -> onCreateCapsule.run());
        root.findViewById(R.id.btnFotoPerfil).setOnClickListener(view -> onOpenProfile.run());
        root.findViewById(R.id.btnVerTodasAperturas).setOnClickListener(view -> onOpenVault.run());
        capsuleCount = root.findViewById(R.id.tvCantidadCapsulas);
        emptyCard = root.findViewById(R.id.cardSinCapsulas);
        emptyMessage = (TextView) emptyCard.getChildAt(0);
        upcomingContainer = root.findViewById(R.id.contenedorProximasCapsulas);
        capsuleCount.setText("Cargando cápsulas...");
        emptyMessage.setText("Buscando próximas aperturas...");
        emptyCard.setVisibility(View.VISIBLE);
        upcomingContainer.setVisibility(View.GONE);
        loadCapsules();
    }

    @SuppressLint("SetTextI18n")
    private void loadCapsules() {
        new Thread(() -> {
            List<Capsule> capsules;
            try {
                capsules = repository.getReceivedCapsules(recipientEmail);
            } catch (RuntimeException exception) {
                activity.runOnUiThread(() -> {
                    if (cannotUpdate()) return;

                    capsuleCount.setText("No se pudieron cargar las cápsulas");
                    emptyMessage.setText("No se pudieron consultar las próximas aperturas.");

                    emptyCard.setVisibility(View.VISIBLE);
                    upcomingContainer.setVisibility(View.GONE);
                });
                return;
            }
            activity.runOnUiThread(() -> {
                if (cannotUpdate()) return;
                showCapsuleCount(capsules.size());
                showUpcomingCapsules(capsules);
            });
        }).start();
    }

    @SuppressLint("SetTextI18n")
    private void showCapsuleCount(int count) {
        String label = count == 1 ? " cápsula" : " cápsulas";
        capsuleCount.setText("Tienes " + count + label);
    }

    @SuppressLint("SetTextI18n")
    private void showUpcomingCapsules(List<Capsule> capsules) {
        long now = System.currentTimeMillis();
        List<Capsule> upcomingCapsules = new ArrayList<>();
        for (Capsule capsule : capsules) {
            long remainingTime = capsule.getOpeningDate() - now;
            if (remainingTime > 0 && remainingTime <= UPCOMING_WINDOW_MILLISECONDS) upcomingCapsules.add(capsule);
        }
        upcomingCapsules.sort(Comparator.comparingLong(Capsule::getOpeningDate));
        upcomingContainer.removeAllViews();
        if (upcomingCapsules.isEmpty()) {
            emptyMessage.setText("No tienes cápsulas próximas en los siguientes 7 días.");
            emptyCard.setVisibility(View.VISIBLE);
            upcomingContainer.setVisibility(View.GONE);
            return;
        }
        for (Capsule capsule : upcomingCapsules) {
            addUpcomingCard(capsule);
        }
        emptyCard.setVisibility(View.GONE);
        upcomingContainer.setVisibility(View.VISIBLE);
    }

    @SuppressLint("SetTextI18n")
    private void addUpcomingCard(Capsule capsule) {
        MaterialCardView card = (MaterialCardView) LayoutInflater.from(activity).inflate(R.layout.item_capsule, upcomingContainer, false);
        ImageView image = card.findViewById(R.id.imgPastillaBoveda);
        TextView title = card.findViewById(R.id.tvNombreCapsula);
        TextView date = card.findViewById(R.id.tvFechaCapsula);
        TextView days = card.findViewById(R.id.tvDiasCapsula);
        TextView status = card.findViewById(R.id.tvEstadoCapsula);
        MaterialCardView statusBackground = card.findViewById(R.id.fondoEstadoCapsula);
        image.setImageResource(CapsuleAppearance.getImageResource(capsule.getOpeningDate()));
        title.setText("¡Muy pronto!");
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy", new Locale("es", "MX"));
        date.setText(dateFormat.format(new Date(capsule.getOpeningDate())));
        long remainingMilliseconds = Math.max(0, capsule.getOpeningDate() - System.currentTimeMillis());
        long remainingDays = Math.max(1, (remainingMilliseconds + DAY_MILLISECONDS - 1) / DAY_MILLISECONDS);
        days.setText(remainingDays == 1 ? "Falta 1 día" : "Faltan " + remainingDays + " días");
        status.setText("Próxima");
        statusBackground.setCardBackgroundColor(Color.parseColor("#D1EBF6"));
        card.setOnClickListener(view -> openCapsule(capsule));
        upcomingContainer.addView(card);
    }

    private void openCapsule(Capsule capsule) {
        if (cannotUpdate()) return;
        Intent intent = new Intent(activity, CapsuleDetailActivity.class);
        intent.putExtra(CapsuleDetailActivity.EXTRA_CAPSULE_ID, capsule.getId());
        intent.putExtra(CapsuleDetailActivity.EXTRA_RECIPIENT_EMAIL, recipientEmail);
        activity.startActivity(intent);
    }

    private boolean cannotUpdate() {
        return disposed || activity.isFinishing() || activity.isDestroyed();
    }

    public void dispose() {
        disposed = true;
    }
}