package com.ranphe.capsly.ui.profile;

import android.view.View;
import android.widget.TextView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.ranphe.capsly.R;

public final class ProfileScreen {
    private ProfileScreen() {}

    public static void bind(View root, String userName, String userEmail, Runnable onSignOut) {
        TextView nameView = root.findViewById(R.id.tvNombrePerfil);
        TextView emailView = root.findViewById(R.id.tvCorreoPerfil);
        TextView helpView = root.findViewById(R.id.tvAyudaPerfil);
        String displayName = userName == null || userName.trim().isEmpty() ? "Usuario" : userName;
        String displayEmail = userEmail == null || userEmail.trim().isEmpty() ? "Correo no disponible" : userEmail;
        nameView.setText(displayName);
        emailView.setText(displayEmail);
        helpView.setText("Para crear una cápsula, toca el botón + y elige una fecha de apertura.\n\nEn Mi Bóveda encontrarás las cápsulas que recibiste. Usa los filtros para consultar las bloqueadas o las que ya están disponibles.\n\nEn Inicio puedes consultar tus tres próximas aperturas.");
        root.findViewById(R.id.btnCerrarSesion).setOnClickListener(view -> showSignOutConfirmation(root, onSignOut));
    }

    private static void showSignOutConfirmation(View root, Runnable onSignOut) {
        new MaterialAlertDialogBuilder(root.getContext())
                .setTitle("Cerrar sesión")
                .setMessage("¿Quieres cerrar tu sesión actual?")
                .setNegativeButton("Cancelar", (dialog, which) -> dialog.dismiss())
                .setPositiveButton("Cerrar sesión", (dialog, which) -> onSignOut.run())
                .show();
    }
}
