package com.ranphe.capsly.ui.home;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.ranphe.capsly.R;
import com.ranphe.capsly.ui.profile.ProfileScreen;
import com.ranphe.capsly.ui.vault.VaultScreen;
import java.util.function.Consumer;

public final class HomeAdapter extends RecyclerView.Adapter<HomeAdapter.PageHolder> {
    private final Activity activity;
    private String userName;
    private final String recipientEmail;
    private final Runnable onCreateCapsule;
    private final Runnable onOpenVault;
    private final Runnable onOpenProfile;
    private final Runnable onSignOut;
    private final Consumer<String> onNameChanged;

    public HomeAdapter(Activity activity, String userName, String recipientEmail, Runnable onCreateCapsule, Runnable onOpenVault, Runnable onOpenProfile, Runnable onSignOut, Consumer<String> onNameChanged) {
        this.activity = activity;
        this.userName = userName;
        this.recipientEmail = recipientEmail;
        this.onCreateCapsule = onCreateCapsule;
        this.onOpenVault = onOpenVault;
        this.onOpenProfile = onOpenProfile;
        this.onSignOut = onSignOut;
        this.onNameChanged = onNameChanged;
    }

    @Override
    public int getItemViewType(int position) {
        return position;
    }

    @NonNull
    @Override
    public PageHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layoutId;
        switch (viewType) {
            case 0:
                layoutId = R.layout.page_home;
                break;
            case 1:
                layoutId = R.layout.page_vault;
                break;
            default:
                layoutId = R.layout.page_profile;
                break;
        }
        View view = LayoutInflater.from(parent.getContext()).inflate(layoutId, parent, false);
        return new PageHolder(view);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull PageHolder holder, int position) {
        releaseHolder(holder);
        switch (position) {
            case 0:
                holder.homeDashboard = new HomeDashboardScreen(activity, holder.itemView, userName, recipientEmail, onCreateCapsule, onOpenVault, onOpenProfile);
                break;
            case 1:
                holder.vaultScreen = new VaultScreen(activity, holder.itemView, recipientEmail);
                break;
            case 2:
                ProfileScreen.bind(holder.itemView, userName, recipientEmail, newName -> {
                    userName = newName;
                    onNameChanged.accept(newName);
                    notifyItemChanged(0);
                }, onSignOut);
                break;
        }
    }

    @Override
    public void onViewRecycled(@NonNull PageHolder holder) {
        releaseHolder(holder);
        super.onViewRecycled(holder);
    }

    private void releaseHolder(PageHolder holder) {
        if (holder.homeDashboard != null) {
            holder.homeDashboard.dispose();
            holder.homeDashboard = null;
        }
        if (holder.vaultScreen != null) {
            holder.vaultScreen.dispose();
            holder.vaultScreen = null;
        }
    }

    @Override
    public int getItemCount() {
        return 3;
    }

    public static final class PageHolder extends RecyclerView.ViewHolder {
        HomeDashboardScreen homeDashboard;
        VaultScreen vaultScreen;
        PageHolder(View itemView) {
            super(itemView);
        }
    }
}