package com.ranphe.capsly.ui.vault;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.card.MaterialCardView;
import com.ranphe.capsly.R;
import com.ranphe.capsly.data.model.Capsule;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public final class CapsuleAdapter extends RecyclerView.Adapter<CapsuleAdapter.CapsuleHolder> {
    public interface OnCapsuleClickListener {
        void onCapsuleClick(Capsule capsule);
    }

    private static final long DAY_MILLISECONDS = 86400000L;
    private final List<Capsule> capsules = new ArrayList<>();
    private final OnCapsuleClickListener listener;

    public CapsuleAdapter(OnCapsuleClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public CapsuleHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_capsule, parent, false);
        return new CapsuleHolder(view);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull CapsuleHolder holder, int position) {
        Capsule capsule = capsules.get(position);
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy", new Locale("es", "MX"));
        holder.date.setText(dateFormat.format(new Date(capsule.getOpeningDate())));
        holder.image.setImageResource(CapsuleAppearance.getImageResource(capsule.getOpeningDate()));
        long remainingMilliseconds = capsule.getOpeningDate() - System.currentTimeMillis();
        if (remainingMilliseconds <= 0) {
            holder.title.setText("Cápsula disponible");
        } else if (remainingMilliseconds <= 7L * DAY_MILLISECONDS) {
            holder.title.setText("¡Muy pronto!");
        } else {
            holder.title.setText("Cápsula bloqueada");
        }
        if (remainingMilliseconds > 0) {
            long remainingDays = Math.max(1, (remainingMilliseconds + DAY_MILLISECONDS - 1) / DAY_MILLISECONDS);
            holder.days.setText(remainingDays == 1 ? "Falta 1 día" : "Faltan " + remainingDays + " días");
            if (remainingDays <= 7) {
                holder.status.setText("Próxima");
                holder.statusBackground.setCardBackgroundColor(Color.parseColor("#D1EBF6"));
            } else {
                holder.status.setText("Bloqueada");
                holder.statusBackground.setCardBackgroundColor(Color.parseColor("#E6E8E5"));
            }
        } else {
            holder.days.setText("Lista para abrir");
            holder.status.setText("Abierta");
            holder.statusBackground.setCardBackgroundColor(Color.parseColor("#D9EBD5"));
        }
        holder.itemView.setOnClickListener(view -> {
            int currentPosition = holder.getBindingAdapterPosition();
            if (currentPosition != RecyclerView.NO_POSITION) listener.onCapsuleClick(capsules.get(currentPosition));
        });
    }

    @Override
    public int getItemCount() {
        return capsules.size();
    }

    public void setCapsules(List<Capsule> items) {
        int previousCount = capsules.size();
        if (previousCount > 0) {
            capsules.clear();
            notifyItemRangeRemoved(0, previousCount);
        }
        capsules.addAll(items);
        if (!capsules.isEmpty()) notifyItemRangeInserted(0, capsules.size());
    }

    public static final class CapsuleHolder extends RecyclerView.ViewHolder {
        final ImageView image;
        final TextView title;
        final TextView date;
        final TextView days;
        final TextView status;
        final MaterialCardView statusBackground;

        CapsuleHolder(View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.imgPastillaBoveda);
            title = itemView.findViewById(R.id.tvNombreCapsula);
            date = itemView.findViewById(R.id.tvFechaCapsula);
            days = itemView.findViewById(R.id.tvDiasCapsula);
            status = itemView.findViewById(R.id.tvEstadoCapsula);
            statusBackground = itemView.findViewById(R.id.fondoEstadoCapsula);
        }
    }
}