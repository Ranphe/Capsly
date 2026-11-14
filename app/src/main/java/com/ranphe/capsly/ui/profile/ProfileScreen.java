package com.ranphe.capsly.ui.profile;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.ranphe.capsly.R;
import com.ranphe.capsly.data.local.DatabaseHelper;
import java.util.function.Consumer;

public final class ProfileScreen {
    private ProfileScreen() {}

    public static void bind(View root, String userName, String userEmail, Consumer<String> onNameChanged, Runnable onSignOut) {
        TextView emailView = root.findViewById(R.id.tvCorreoPerfil);
        TextView helpView = root.findViewById(R.id.tvAyudaPerfil);
        String displayName = userName == null || userName.trim().isEmpty() ? "Usuario" : userName;
        String displayEmail = userEmail == null || userEmail.trim().isEmpty() ? "Correo no disponible" : userEmail;
        emailView.setText(displayEmail);
        helpView.setText("Para crear una cápsula, toca el botón + y elige una fecha de apertura.\n\nEn Mi Bóveda encontrarás las cápsulas que recibiste. Usa los filtros para consultar sus estados.\n\nEn Inicio puedes consultar tus cápsulas desbloqueadas.");
        new NameEditor(root, displayName, userEmail, onNameChanged).bind();
        root.findViewById(R.id.btnCerrarSesion).setOnClickListener(view -> showSignOutConfirmation(root, onSignOut));
    }

    private static final class NameEditor {
        private final View root;
        private final EditText nameInput;
        private final ImageButton editButton;
        private final String email;
        private final Consumer<String> onNameChanged;
        private String savedName;
        private boolean editing = false;
        private boolean saving = false;

        private NameEditor(View root, String initialName, String email, Consumer<String> onNameChanged) {
            this.root = root;
            this.nameInput = root.findViewById(R.id.etNombrePerfil);
            this.editButton = root.findViewById(R.id.btnEditarNombrePerfil);
            this.email = email;
            this.onNameChanged = onNameChanged;
            this.savedName = initialName;
        }

        private void bind() {
            nameInput.setText(savedName);
            editButton.setOnClickListener(view -> {
                if (saving) return;
                if (editing) {
                    saveName();
                } else {
                    startEditing();
                }
            });
            nameInput.setOnEditorActionListener((view, actionId, event) -> {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    saveName();
                    return true;
                }
                return false;
            });
        }

        private void startEditing() {
            editing = true;
            nameInput.setFocusable(true);
            nameInput.setFocusableInTouchMode(true);
            nameInput.setCursorVisible(true);
            nameInput.requestFocus();
            nameInput.selectAll();
            editButton.setImageResource(android.R.drawable.ic_menu_save);
            editButton.setContentDescription("Guardar nickname");
            InputMethodManager keyboard = (InputMethodManager) root.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            nameInput.post(() -> {
                if (keyboard != null) keyboard.showSoftInput(nameInput, InputMethodManager.SHOW_IMPLICIT);
            });
        }

        private void saveName() {
            if (!editing || saving) return;
            String newName = nameInput.getText() == null ? "" : nameInput.getText().toString();
            if (newName.length() < 2 || newName.length() > 20 || newName.matches(".*\\s.*")) {
                nameInput.setError("Usa de 2 a 20 caracteres, sin espacios");
                nameInput.requestFocus();
                return;
            }
            if (newName.equals(savedName)) {
                finishEditing();
                return;
            }
            saving = true;
            editButton.setEnabled(false);
            nameInput.setEnabled(false);
            new Thread(() -> {
                boolean updated = false;
                try (DatabaseHelper database = new DatabaseHelper(root.getContext().getApplicationContext())) {
                    updated = database.updateUserName(email, newName);
                } catch (RuntimeException ignored) {
                }
                boolean result = updated;
                new Handler(Looper.getMainLooper()).post(() -> {
                    if (!root.isAttachedToWindow()) return;
                    saving = false;
                    editButton.setEnabled(true);
                    nameInput.setEnabled(true);
                    if (!result) {
                        nameInput.setError("No se pudo guardar el nickname");
                        nameInput.requestFocus();
                        return;
                    }
                    savedName = newName;
                    onNameChanged.accept(newName);
                    finishEditing();
                    Toast.makeText(root.getContext(), "Nickname actualizado", Toast.LENGTH_SHORT).show();
                });
            }).start();
        }


        private void finishEditing() {
            editing = false;
            nameInput.setError(null);
            nameInput.setText(savedName);
            nameInput.setCursorVisible(false);
            nameInput.setFocusable(false);
            nameInput.setFocusableInTouchMode(false);
            nameInput.clearFocus();
            editButton.setImageResource(android.R.drawable.ic_menu_edit);
            editButton.setContentDescription("Editar nickname");
            InputMethodManager keyboard = (InputMethodManager) root.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (keyboard != null) keyboard.hideSoftInputFromWindow(nameInput.getWindowToken(), 0);
        }
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