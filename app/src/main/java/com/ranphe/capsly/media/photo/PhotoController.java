package com.ranphe.capsly.media.photo;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.net.Uri;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.FileProvider;
import com.ranphe.capsly.R;
import java.io.File;
import java.io.IOException;

public final class PhotoController {
    private final Activity activity;
    private final ActivityResultLauncher<PickVisualMediaRequest> galleryLauncher;
    private final ActivityResultLauncher<Uri> cameraLauncher;
    private Uri selectedUri;
    private Uri cameraUri;
    private File cameraFile;

    public PhotoController(Activity activity, ActivityResultLauncher<PickVisualMediaRequest> galleryLauncher, ActivityResultLauncher<Uri> cameraLauncher) {
        this.activity = activity;
        this.galleryLauncher = galleryLauncher;
        this.cameraLauncher = cameraLauncher;
    }

    public void openGallery() {
        PickVisualMediaRequest request = new PickVisualMediaRequest.Builder().setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE).build();
        galleryLauncher.launch(request);
    }

    public void openCamera() {
        try {
            File directory = new File(activity.getCacheDir(), "fotos");
            if (!directory.exists() && !directory.mkdirs()) {
                Toast.makeText(activity, "No se pudo preparar la cámara", Toast.LENGTH_SHORT).show();
                return;
            }
            cameraFile = File.createTempFile("capsly_", ".jpg", directory);
            cameraUri = FileProvider.getUriForFile(activity, activity.getPackageName() + ".fileprovider", cameraFile);
            cameraLauncher.launch(cameraUri);
        } catch (IOException | IllegalArgumentException | ActivityNotFoundException exception) {
            deletePendingCameraFile();
            Toast.makeText(activity, "No se pudo abrir la cámara", Toast.LENGTH_SHORT).show();
        }
    }

    public void onGalleryResult(Uri uri) {
        if (uri != null) showPreview(uri);
    }

    public void onCameraResult(boolean saved, boolean allowPreview) {
        if (saved && cameraUri != null) {
            if (allowPreview) {
                showPreview(cameraUri);
            } else {
                deletePendingCameraFile();
            }
        } else {
            deletePendingCameraFile();
            if (allowPreview) Toast.makeText(activity, "No se tomó ninguna fotografía", Toast.LENGTH_SHORT).show();
        }
        cameraFile = null;
        cameraUri = null;
    }

    public Uri getSelectedUri() {
        return selectedUri;
    }

    public void clearSelection() {
        selectedUri = null;
    }

    private void showPreview(Uri uri) {
        ImageView image = activity.findViewById(R.id.imgVistaPreviaRecuerdo);
        TextView status = activity.findViewById(R.id.tvEstadoArchivo);
        if (image == null) return;
        selectedUri = uri;
        image.setImageURI(null);
        image.setImageURI(uri);
        image.setVisibility(View.VISIBLE);
        activity.findViewById(R.id.contenedorVideoPreview).setVisibility(View.GONE);
        activity.findViewById(R.id.controlesAudio).setVisibility(View.GONE);
        if (status != null) {
            status.setText("");
            status.setVisibility(View.GONE);
        }
    }

    private void deletePendingCameraFile() {
        if (cameraFile != null && cameraFile.exists()) {
            boolean deleted = cameraFile.delete();
            if (!deleted) Log.w("Capsly", "No se pudo eliminar el archivo de la cámara");
        }
        cameraFile = null;
        cameraUri = null;
    }
}