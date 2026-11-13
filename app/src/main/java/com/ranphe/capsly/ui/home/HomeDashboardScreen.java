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
    private final Activity activity;
    private final String recipientEmail;
    private final CapsuleRepository repository;
    private final TextView capsuleCount;
    private final MaterialCardView emptyCard;
    private final TextView emptyMessage;
    private final LinearLayout unlockedContainer;
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
        unlockedContainer = root.findViewById(R.id.contenedorProximasCapsulas);
        capsuleCount.setText("Cargando cápsulas...");
        emptyMessage.setText("Buscando cápsulas desbloqueadas...");
        emptyCard.setVisibility(View.VISIBLE);
        unlockedContainer.setVisibility(View.GONE);
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
                    emptyMessage.setText("No se pudieron consultar las cápsulas desbloqueadas.");
                    emptyCard.setVisibility(View.VISIBLE);
                    unlockedContainer.setVisibility(View.GONE);
                });
                return;
            }
            activity.runOnUiThread(() -> {
                if (cannotUpdate()) return;
                showCapsuleCount(capsules.size());
                showUnlockedCapsules(capsules);
            });
        }).start();
    }

    @SuppressLint("SetTextI18n")
    private void showCapsuleCount(int count) {
        String label = count == 1 ? " cápsula" : " cápsulas";
        capsuleCount.setText("Tienes " + count + label);
    }

    @SuppressLint("SetTextI18n")
    private void showUnlockedCapsules(List<Capsule> capsules) {
        List<Capsule> unlockedCapsules = new ArrayList<>();
        for (Capsule capsule : capsules) {
            if (CapsuleAppearance.getStatus(capsule) == CapsuleAppearance.Status.UNLOCKED) unlockedCapsules.add(capsule);
        }
        unlockedCapsules.sort(Comparator.comparingLong(Capsule::getOpeningDate).reversed());
        unlockedContainer.removeAllViews();
        if (unlockedCapsules.isEmpty()) {
            emptyMessage.setText("No tienes cápsulas desbloqueadas por abrir.");
            emptyCard.setVisibility(View.VISIBLE);
            unlockedContainer.setVisibility(View.GONE);
            return;
        }
        for (Capsule capsule : unlockedCapsules) {
            addUnlockedCard(capsule);
        }
        emptyCard.setVisibility(View.GONE);
        unlockedContainer.setVisibility(View.VISIBLE);
    }

    @SuppressLint("SetTextI18n")
    private void addUnlockedCard(Capsule capsule) {
        MaterialCardView card = (MaterialCardView) LayoutInflater.from(activity).inflate(R.layout.item_capsule, unlockedContainer, false);
        ImageView image = card.findViewById(R.id.imgPastillaBoveda);
        TextView title = card.findViewById(R.id.tvNombreCapsula);
        TextView date = card.findViewById(R.id.tvFechaCapsula);
        TextView days = card.findViewById(R.id.tvDiasCapsula);
        TextView status = card.findViewById(R.id.tvEstadoCapsula);
        MaterialCardView statusBackground = card.findViewById(R.id.fondoEstadoCapsula);
        image.setImageResource(CapsuleAppearance.getImageResource(capsule));
        title.setText("Cápsula disponible");
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy", new Locale("es", "MX"));
        date.setText(dateFormat.format(new Date(capsule.getOpeningDate())));
        days.setText("Lista para abrir");
        status.setText("Desbloqueada");
        statusBackground.setCardBackgroundColor(Color.parseColor("#E0EBCB"));
        card.setOnClickListener(view -> openCapsule(capsule));
        unlockedContainer.addView(card);
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