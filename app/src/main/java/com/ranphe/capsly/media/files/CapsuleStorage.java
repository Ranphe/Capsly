package com.ranphe.capsly.media.files;

import android.content.Context;
import android.net.Uri;
import android.webkit.MimeTypeMap;
import android.util.Log;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

public final class CapsuleStorage {
    private final Context context;

    public CapsuleStorage(Context context) {
        this.context = context.getApplicationContext();
    }

    public File copyToPrivateStorage(Uri sourceUri, String format) throws IOException {
        File directory = new File(context.getNoBackupFilesDir(), "capsulas");
        if (!directory.exists() && !directory.mkdirs()) throw new IOException("Could not create the capsule directory");
        String defaultExtension = "Foto".equals(format) ? "jpg" : "Video".equals(format) ? "mp4" : "m4a";
        String mimeType = context.getContentResolver().getType(sourceUri);
        String extension = mimeType == null ? null : MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType);
        if (extension == null || !extension.matches("[a-zA-Z0-9]{1,8}")) extension = defaultExtension;
        File destination = new File(directory, UUID.randomUUID().toString() + "." + extension);
        long maximumSize = "Video".equals(format) ? 200L * 1024 * 1024 : "Audio".equals(format) ? 30L * 1024 * 1024 : 20L * 1024 * 1024;
        try (InputStream input = context.getContentResolver().openInputStream(sourceUri)) {
            if (input == null) throw new IOException("Could not read the selected file");
            try (FileOutputStream output = new FileOutputStream(destination)) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                long totalBytes = 0;
                while ((bytesRead = input.read(buffer)) != -1) {
                    totalBytes += bytesRead;
                    if (totalBytes > maximumSize) throw new IOException("The selected file exceeds the allowed size");
                    output.write(buffer, 0, bytesRead);
                }
                if (totalBytes == 0) throw new IOException("The selected file is empty");
            }
            return destination;
        } catch (IOException | RuntimeException exception) {
            if (destination.exists()) {
                boolean deleted = destination.delete();
                if (!deleted) Log.w("Capsly", "No se pudo eliminar el archivo de destino");
            }
            throw exception;
        }
    }

    public File resolveStoredFile(String relativePath) throws IOException {
        if (relativePath == null || !relativePath.startsWith("capsulas/")) throw new IOException("Invalid capsule file path");
        File directory = new File(context.getNoBackupFilesDir(), "capsulas").getCanonicalFile();
        File file = new File(context.getNoBackupFilesDir(), relativePath).getCanonicalFile();
        if (!directory.equals(file.getParentFile()) || !file.isFile()) throw new IOException("Capsule file not found");
        return file;
    }
}