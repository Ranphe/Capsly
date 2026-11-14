package com.ranphe.capsly.ui.create;

import android.net.Uri;
import android.util.Patterns;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.textfield.TextInputEditText;
import com.ranphe.capsly.R;
import com.ranphe.capsly.data.repository.CapsuleRepository;
import com.ranphe.capsly.session.UserSession;
import com.ranphe.capsly.config.DebugConfig;
import java.util.Calendar;
import java.util.function.BiConsumer;

public final class CreateMemoryController {
    private final AppCompatActivity activity;
    private final UserSession userSession;
    private final CapsuleRepository repository;
    private final BiConsumer<String, Long> onSaved;
    private long openingDate = -1;
    private boolean saving = false;

    public CreateMemoryController(AppCompatActivity activity, UserSession userSession, CapsuleRepository repository, BiConsumer<String, Long> onSaved) {
        this.activity = activity;
        this.userSession = userSession;
        this.repository = repository;
        this.onSaved = onSaved;
    }

    public boolean isSaving() {
        return saving;
    }

    public void chooseOpeningDate() {
        if (saving) return;
        OpeningDatePicker.show(activity, timestamp -> openingDate = timestamp);
    }

    public void save(String format, Uri fileUri) {
        if (saving) return;
        TextInputEditText titleInput = activity.findViewById(R.id.etTituloRecuerdo);
        TextInputEditText messageInput = activity.findViewById(R.id.etContenidoRecuerdo);
        TextInputEditText recipientInput = activity.findViewById(R.id.etDestinatarioRecuerdo);
        String title = readInput(titleInput).trim();
        String message = readInput(messageInput).trim();
        String recipientEmail = readInput(recipientInput).trim();
        if (title.isEmpty()) {
            titleInput.setError("Ingresa un título");
            titleInput.requestFocus();
            return;
        }
        if (format.isEmpty()) {
            Toast.makeText(activity, "Selecciona un formato", Toast.LENGTH_SHORT).show();
            return;
        }
        if (message.isEmpty()) {
            messageInput.setError("Escribe un mensaje para tu recuerdo");
            messageInput.requestFocus();
            return;
        }
        if (recipientEmail.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(recipientEmail).matches()) {
            recipientInput.setError("Ingresa un correo válido");
            recipientInput.requestFocus();
            return;
        }
        Calendar today = Calendar.getInstance();
        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);
        if (openingDate < today.getTimeInMillis()) {
            Toast.makeText(activity, "Selecciona una fecha de apertura futura", Toast.LENGTH_SHORT).show();
            return;
        }
        if (fileUri == null) {
            Toast.makeText(activity, "Selecciona o captura un archivo", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!userSession.isSignedIn()) {
            Toast.makeText(activity, "Debes iniciar sesión nuevamente", Toast.LENGTH_SHORT).show();
            return;
        }
        String senderEmail = userSession.getEmail();
        boolean isSelfRecipient = senderEmail.trim().equalsIgnoreCase(recipientEmail.trim());
        if (isSelfRecipient && !DebugConfig.canSendToSelf()) {
            recipientInput.setError("No puedes enviarte cápsulas a ti mismo");
            recipientInput.requestFocus();
            return;
        }
        long selectedOpeningDate = openingDate;
        setSaving(true);
        new Thread(() -> {
            CapsuleRepository.SaveResult result = repository.saveCapsule(senderEmail, recipientEmail, title, message, format, fileUri, selectedOpeningDate);
            activity.runOnUiThread(() -> {
                if (activity.isFinishing() || activity.isDestroyed()) return;
                if (result == CapsuleRepository.SaveResult.SUCCESS) {
                    saving = false;
                    onSaved.accept(recipientEmail, selectedOpeningDate);
                    return;
                }
                setSaving(false);
                switch (result) {
                    case RECIPIENT_NOT_FOUND:
                        recipientInput.setError("Este correo no tiene una cuenta registrada");
                        recipientInput.requestFocus();
                        break;
                    case SENDER_NOT_FOUND:
                        Toast.makeText(activity, "Debes iniciar sesión nuevamente", Toast.LENGTH_SHORT).show();
                        break;
                    default:
                        Toast.makeText(activity, "No se pudo guardar la cápsula. Revisa el archivo y el espacio disponible.", Toast.LENGTH_LONG).show();
                        break;
                }
            });
        }).start();
    }

    private String readInput(TextInputEditText input) {
        return input.getText() == null ? "" : input.getText().toString();
    }

    private void setSaving(boolean value) {
        saving = value;
        int[] controlIds = {
                R.id.etTituloRecuerdo,
                R.id.etContenidoRecuerdo,
                R.id.etDestinatarioRecuerdo,
                R.id.btnFormatoFoto,
                R.id.btnFormatoVideo,
                R.id.btnFormatoAudio,
                R.id.btnSeleccionarArchivo,
                R.id.btnCapturarArchivo,
                R.id.btnFechaApertura,
                R.id.btnContinuarRecuerdo,
                R.id.btnVolverInicio
        };
        for (int controlId : controlIds) {
            View control = activity.findViewById(controlId);
            if (control != null) control.setEnabled(!value);
        }
    }
}