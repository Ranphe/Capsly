package com.ranphe.capsly.data.repository;

import android.content.Context;
import android.net.Uri;
import android.util.Log;
import com.ranphe.capsly.data.local.DatabaseHelper;
import com.ranphe.capsly.media.files.CapsuleStorage;
import com.ranphe.capsly.data.model.Capsule;
import com.ranphe.capsly.data.model.UnlockedCapsule;
import java.io.File;
import java.io.IOException;
import java.util.List;

public final class CapsuleRepository {
    public enum SaveResult {
        SUCCESS,
        RECIPIENT_NOT_FOUND,
        SENDER_NOT_FOUND,
        FAILURE
    }
    private final Context context;
    private final CapsuleStorage storage;

    public CapsuleRepository(Context context) {
        this.context = context.getApplicationContext();
        this.storage = new CapsuleStorage(this.context);
    }

    public SaveResult saveCapsule(String senderEmail, String recipientEmail, String title, String message, String format, Uri fileUri, long openingDate) {
        DatabaseHelper database = new DatabaseHelper(context);
        File storedFile = null;
        try {
            if (!database.userExists(recipientEmail)) return SaveResult.RECIPIENT_NOT_FOUND;
            storedFile = storage.copyToPrivateStorage(fileUri, format);
            String filePath = "capsulas/" + storedFile.getName();
            long capsuleId = database.saveCapsule(senderEmail, recipientEmail, title, message, format, filePath, openingDate);
            if (capsuleId > 0) {
                storedFile = null;
                return SaveResult.SUCCESS;
            }
            if (capsuleId == -2) return SaveResult.SENDER_NOT_FOUND;
            if (capsuleId == -1) return SaveResult.RECIPIENT_NOT_FOUND;
            return SaveResult.FAILURE;
        } catch (IOException | RuntimeException exception) {
            return SaveResult.FAILURE;
        } finally {
            if (storedFile != null && storedFile.exists()) {
                boolean deleted = storedFile.delete();
                if (!deleted) Log.w("Capsly", "No se pudo eliminar el archivo almacenado");
            }
            database.close();
        }
    }

    public List<Capsule> getReceivedCapsules(String recipientEmail) {
        try (DatabaseHelper database = new DatabaseHelper(context)) {
            return database.getReceivedCapsules(recipientEmail);
        }
    }

    public Capsule getReceivedCapsule(long capsuleId, String recipientEmail) {
        try (DatabaseHelper database = new DatabaseHelper(context)) {
            return database.getReceivedCapsule(capsuleId, recipientEmail);
        }
    }

    public UnlockedCapsule getUnlockedCapsule(long capsuleId, String recipientEmail) {
        try (DatabaseHelper database = new DatabaseHelper(context)) {
            return database.getUnlockedCapsule(capsuleId, recipientEmail);
        }
    }

    public void markCapsuleOpened(long capsuleId, String recipientEmail) {
        try (DatabaseHelper database = new DatabaseHelper(context)) {
            database.markCapsuleOpened(capsuleId, recipientEmail);
        }
    }
}