package com.ranphe.capsly.ui.create;

import android.net.Uri;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.ranphe.capsly.R;
import com.ranphe.capsly.data.repository.CapsuleRepository;
import com.ranphe.capsly.media.audio.AudioController;
import com.ranphe.capsly.media.photo.PhotoController;
import com.ranphe.capsly.media.video.VideoController;
import com.ranphe.capsly.session.UserSession;

public final class CreateMemoryScreen {
    private final AppCompatActivity activity;
    private final PhotoController photoController;
    private final VideoController videoController;
    private final AudioController audioController;
    private final CreateMemoryController memoryController;
    private String selectedFormat = "";

    public CreateMemoryScreen(AppCompatActivity activity, UserSession userSession, PhotoController photoController, VideoController videoController, AudioController audioController, Runnable onReturnHome) {
        this.activity = activity;
        this.photoController = photoController;
        this.videoController = videoController;
        this.audioController = audioController;
        CapsuleRepository repository = new CapsuleRepository(activity.getApplicationContext());
        memoryController = new CreateMemoryController(activity, userSession, repository, onReturnHome);
        bind(onReturnHome);
    }

    public String getSelectedFormat() {
        return selectedFormat;
    }

    public boolean isSaving() {
        return memoryController.isSaving();
    }

    private void bind(Runnable onReturnHome) {
        activity.findViewById(R.id.btnVolverInicio).setOnClickListener(view -> onReturnHome.run());
        MaterialButton photoButton = activity.findViewById(R.id.btnFormatoFoto);
        MaterialButton videoButton = activity.findViewById(R.id.btnFormatoVideo);
        MaterialButton audioButton = activity.findViewById(R.id.btnFormatoAudio);
        photoButton.setOnClickListener(view -> selectFormat("Foto"));
        videoButton.setOnClickListener(view -> selectFormat("Video"));
        audioButton.setOnClickListener(view -> selectFormat("Audio"));
        activity.findViewById(R.id.btnSeleccionarArchivo).setOnClickListener(view -> selectFile());
        activity.findViewById(R.id.btnCapturarArchivo).setOnClickListener(view -> captureFile());
        activity.findViewById(R.id.btnFechaApertura).setOnClickListener(view -> memoryController.chooseOpeningDate());
        activity.findViewById(R.id.btnContinuarRecuerdo).setOnClickListener(view -> memoryController.save(selectedFormat, getSelectedFileUri()));
    }

    private void selectFormat(String format) {
        if (memoryController.isSaving()) return;
        audioController.clearSelection();
        photoController.clearSelection();
        videoController.clearSelection();
        selectedFormat = format;
        MemoryFormatView.render(activity, format);
    }

    private void selectFile() {
        if (memoryController.isSaving()) return;
        switch (selectedFormat) {
            case "Foto":
                photoController.openGallery();
                break;
            case "Video":
                videoController.openGallery();
                break;
            case "Audio":
                audioController.openGallery();
                break;
            default:
                Toast.makeText(activity, "Selecciona un formato", Toast.LENGTH_SHORT).show();
                break;
        }
    }

    private void captureFile() {
        if (memoryController.isSaving()) return;
        switch (selectedFormat) {
            case "Foto":
                photoController.openCamera();
                break;
            case "Video":
                videoController.openCamera();
                break;
            case "Audio":
                audioController.toggleRecording();
                break;
            default:
                Toast.makeText(activity, "Selecciona un formato", Toast.LENGTH_SHORT).show();
                break;
        }
    }

    private Uri getSelectedFileUri() {
        switch (selectedFormat) {
            case "Foto":
                return photoController.getSelectedUri();
            case "Video":
                return videoController.getSelectedUri();
            case "Audio":
                return audioController.getSelectedUri();
            default:
                return null;
        }
    }
}