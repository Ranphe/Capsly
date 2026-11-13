package com.ranphe.capsly.media.video;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;
import android.util.Log;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.FileProvider;
import com.ranphe.capsly.R;
import java.io.File;
import java.io.IOException;
import java.util.Locale;

public final class VideoController {
    private final Activity activity;
    private final ActivityResultLauncher<PickVisualMediaRequest> galleryLauncher;
    private final ActivityResultLauncher<Uri> cameraLauncher;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Uri selectedUri;
    private Uri pendingCameraUri;
    private File pendingCameraFile;
    private File capturedCameraFile;
    private VideoView videoView;
    private Runnable progressUpdater;
    private boolean isSeeking = false;
    private int previewGeneration = 0;

    public VideoController(Activity activity, ActivityResultLauncher<PickVisualMediaRequest> galleryLauncher, ActivityResultLauncher<Uri> cameraLauncher) {
        this.activity = activity;
        this.galleryLauncher = galleryLauncher;
        this.cameraLauncher = cameraLauncher;
    }

    public void openGallery() {
        pausePlayback();
        PickVisualMediaRequest request = new PickVisualMediaRequest.Builder().setMediaType(ActivityResultContracts.PickVisualMedia.VideoOnly.INSTANCE).build();
        galleryLauncher.launch(request);
    }

    public void openCamera() {
        pausePlayback();
        try {
            File directory = new File(activity.getCacheDir(), "videos");
            if (!directory.exists() && !directory.mkdirs()) {
                Toast.makeText(activity, "No se pudo preparar la cámara", Toast.LENGTH_SHORT).show();
                return;
            }
            pendingCameraFile = File.createTempFile("capsly_video_", ".mp4", directory);
            pendingCameraUri = FileProvider.getUriForFile(activity, activity.getPackageName() + ".fileprovider", pendingCameraFile);
            cameraLauncher.launch(pendingCameraUri);
        } catch (IOException | IllegalArgumentException | ActivityNotFoundException exception) {
            deletePendingCameraFile();
            Toast.makeText(activity, "No se pudo abrir la cámara de video", Toast.LENGTH_SHORT).show();
        }
    }

    public void onGalleryResult(Uri uri) {
        if (uri == null) return;
        showPreview(uri);
        deleteCapturedCameraFile();
    }

    public void onCameraResult(boolean saved, boolean allowPreview) {
        if (saved && pendingCameraUri != null && pendingCameraFile != null && pendingCameraFile.length() > 0 && allowPreview) {
            Uri resultUri = pendingCameraUri;
            File resultFile = pendingCameraFile;
            pendingCameraUri = null;
            pendingCameraFile = null;
            showPreview(resultUri);
            deleteCapturedCameraFile();
            capturedCameraFile = resultFile;
        } else {
            deletePendingCameraFile();
            if (allowPreview) Toast.makeText(activity, "No se guardó ningún video", Toast.LENGTH_SHORT).show();
        }
    }

    public Uri getSelectedUri() {
        return selectedUri;
    }

    public void clearSelection() {
        stopPlayback();
        selectedUri = null;
        deleteCapturedCameraFile();
    }

    public void pausePlayback() {
        if (videoView != null && videoView.isPlaying()) {
            videoView.pause();
            ImageButton playButton = activity.findViewById(R.id.btnPlayPausaVideo);
            if (playButton != null) playButton.setImageResource(android.R.drawable.ic_media_play);
        }
    }

    public void release() {
        clearSelection();
        deletePendingCameraFile();
    }

    @SuppressLint("SetTextI18n")
    private void showPreview(Uri uri) {
        stopPlayback();
        VideoView video = activity.findViewById(R.id.videoVistaPreviaRecuerdo);
        View container = activity.findViewById(R.id.contenedorVideoPreview);
        TextView status = activity.findViewById(R.id.tvEstadoArchivo);
        ImageButton playButton = activity.findViewById(R.id.btnPlayPausaVideo);
        SeekBar seekBar = activity.findViewById(R.id.seekBarVideo);
        TextView currentTime = activity.findViewById(R.id.tvTiempoActualVideo);
        TextView totalTime = activity.findViewById(R.id.tvDuracionVideo);
        if (video == null || container == null || status == null || playButton == null || seekBar == null || currentTime == null || totalTime == null) return;
        status.setText("");
        status.setVisibility(View.GONE);
        selectedUri = uri;
        videoView = video;
        int generation = previewGeneration;
        View imagePreview = activity.findViewById(R.id.imgVistaPreviaRecuerdo);
        View audioControls = activity.findViewById(R.id.controlesAudio);
        if (imagePreview != null) imagePreview.setVisibility(View.GONE);
        if (audioControls != null) audioControls.setVisibility(View.GONE);
        video.setMediaController(null);
        video.setVisibility(View.VISIBLE);
        container.setVisibility(View.VISIBLE);
        ViewGroup.LayoutParams originalParams = video.getLayoutParams();
        if (originalParams instanceof FrameLayout.LayoutParams) {
            FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            layoutParams.gravity = Gravity.CENTER;
            video.setLayoutParams(layoutParams);
        }
        playButton.setImageResource(android.R.drawable.ic_media_play);
        playButton.setEnabled(false);
        seekBar.setEnabled(false);
        seekBar.setProgress(0);
        seekBar.setMax(1000);
        currentTime.setText("00:00");
        totalTime.setText("00:00");
        isSeeking = false;
        playButton.setOnClickListener(view -> {
            if (generation != previewGeneration || videoView != video) return;
            if (video.isPlaying()) {
                video.pause();
                playButton.setImageResource(android.R.drawable.ic_media_play);
            } else {
                video.start();
                playButton.setImageResource(android.R.drawable.ic_media_pause);
            }
        });
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
                if (fromUser) currentTime.setText(formatTime(progress));
            }
            @Override
            public void onStartTrackingTouch(SeekBar bar) {
                isSeeking = true;
            }

            @Override
            public void onStopTrackingTouch(SeekBar bar) {
                isSeeking = false;
                if (generation == previewGeneration && videoView == video) video.seekTo(bar.getProgress());
            }
        });

        video.setOnPreparedListener(player -> {
            if (generation != previewGeneration || videoView != video) return;
            int duration = Math.max(player.getDuration(), 1);
            int videoWidth = player.getVideoWidth();
            int videoHeight = player.getVideoHeight();
            seekBar.setMax(duration);
            totalTime.setText(formatTime(duration));
            playButton.setEnabled(true);
            seekBar.setEnabled(true);
            video.post(() -> {
                if (generation != previewGeneration || videoView != video) return;
                View parent = (View) video.getParent();
                if (parent != null && parent.getWidth() > 0 && parent.getHeight() > 0 && videoWidth > 0 && videoHeight > 0) {
                    float videoRatio = (float) videoWidth / videoHeight;
                    float containerRatio = (float) parent.getWidth() / parent.getHeight();
                    int finalWidth;
                    int finalHeight;
                    if (videoRatio > containerRatio) {
                        finalWidth = parent.getWidth();
                        finalHeight = Math.round(finalWidth / videoRatio);
                    } else {
                        finalHeight = parent.getHeight();
                        finalWidth = Math.round(finalHeight * videoRatio);
                    }
                    FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(finalWidth, finalHeight);
                    layoutParams.gravity = Gravity.CENTER;
                    video.setLayoutParams(layoutParams);
                }
                video.seekTo(1);
            });

            if (progressUpdater != null) handler.removeCallbacks(progressUpdater);

            progressUpdater = new Runnable() {
                @Override
                public void run() {
                    if (generation != previewGeneration || videoView != video) return;
                    if (!isSeeking) {
                        int position = video.getCurrentPosition();
                        seekBar.setProgress(position);
                        currentTime.setText(formatTime(position));
                    }
                    handler.postDelayed(this, 250);
                }
            };
            handler.post(progressUpdater);
        });

        video.setOnCompletionListener(player -> {
            if (generation != previewGeneration || videoView != video) return;
            playButton.setImageResource(android.R.drawable.ic_media_play);
            seekBar.setProgress(0);
            currentTime.setText("00:00");
            video.seekTo(1);
        });
        video.setOnErrorListener((player, what, extra) -> {
            if (generation != previewGeneration || videoView != video) return true;
            status.setText("No se pudo reproducir este video");
            status.setVisibility(View.VISIBLE);
            playButton.setEnabled(false);
            seekBar.setEnabled(false);
            return true;
        });
        video.setVideoURI(uri);
        video.requestLayout();
    }

    private void stopPlayback() {
        previewGeneration++;
        if (progressUpdater != null) {
            handler.removeCallbacks(progressUpdater);
            progressUpdater = null;
        }
        isSeeking = false;
        if (videoView != null) {
            videoView.setOnPreparedListener(null);
            videoView.setOnCompletionListener(null);
            videoView.setOnErrorListener(null);
            videoView.stopPlayback();
            videoView = null;
        }
    }

    private void deletePendingCameraFile() {
        if (pendingCameraFile != null && pendingCameraFile.exists()) {
            boolean deleted = pendingCameraFile.delete();
            if (!deleted) Log.w("Capsly", "No se pudo eliminar el archivo pendiente de la cámara");
        }
        pendingCameraFile = null;
        pendingCameraUri = null;
    }

    private void deleteCapturedCameraFile() {
        if (capturedCameraFile != null && capturedCameraFile.exists()) {
            boolean deleted = capturedCameraFile.delete();
            if (!deleted) Log.w("Capsly", "No se pudo eliminar el archivo capturado por la cámara");
        }
        capturedCameraFile = null;
    }

    private String formatTime(int milliseconds) {
        int totalSeconds = milliseconds / 1000;
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
    }
}