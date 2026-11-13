package com.ranphe.capsly.media.audio;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log;
import androidx.activity.result.ActivityResultLauncher;
import androidx.core.content.ContextCompat;
import com.google.android.material.button.MaterialButton;
import com.ranphe.capsly.R;
import java.io.File;
import java.io.IOException;
import java.util.Locale;

public final class AudioController {
    private final Activity activity;
    private final ActivityResultLauncher<String[]> galleryLauncher;
    private final ActivityResultLauncher<String> permissionLauncher;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Uri selectedUri;
    private File temporaryFile;
    private MediaRecorder recorder;
    private MediaPlayer player;
    private Runnable progressUpdater;
    private boolean recording = false;
    private boolean seeking = false;

    public AudioController(Activity activity, ActivityResultLauncher<String[]> galleryLauncher, ActivityResultLauncher<String> permissionLauncher) {
        this.activity = activity;
        this.galleryLauncher = galleryLauncher;
        this.permissionLauncher = permissionLauncher;
    }

    public Uri getSelectedUri() {
        return selectedUri;
    }

    public void openGallery() {
        if (recording) return;
        pausePlayback();
        galleryLauncher.launch(new String[]{"audio/*"});
    }

    public void onGalleryResult(Uri uri) {
        if (uri == null) return;
        releasePlayer();
        deleteTemporaryFile();
        showPreview(uri);
    }

    public void toggleRecording() {
        if (recording) {
            stopRecording(true);
        } else {
            requestRecording();
        }
    }

    public void requestRecording() {
        if (recording) return;
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            startRecording();
        } else {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO);
        }
    }

    public void onMicrophonePermissionResult(boolean granted, boolean allowRecording) {
        if (!allowRecording) return;
        if (granted) {
            startRecording();
        } else {
            Toast.makeText(activity, "Se necesita permiso de micrófono para grabar", Toast.LENGTH_SHORT).show();
        }
    }

    @SuppressLint("SetTextI18n")
    private void startRecording() {
        if (recording) return;
        View controls = activity.findViewById(R.id.controlesAudio);
        MaterialButton recordButton = activity.findViewById(R.id.btnCapturarArchivo);
        View selectButton = activity.findViewById(R.id.btnSeleccionarArchivo);
        TextView status = activity.findViewById(R.id.tvEstadoArchivo);
        if (controls == null || recordButton == null || selectButton == null || status == null) return;
        try {
            releasePlayer();
            selectedUri = null;
            deleteTemporaryFile();
            controls.setVisibility(View.GONE);
            File directory = new File(activity.getCacheDir(), "audio");
            if (!directory.exists() && !directory.mkdirs()) {
                Toast.makeText(activity, "No se pudo preparar la grabadora", Toast.LENGTH_SHORT).show();
                return;
            }
            temporaryFile = File.createTempFile("capsly_audio_", ".m4a", directory);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                recorder = new MediaRecorder(activity);
            } else {
                recorder = new MediaRecorder();
            }
            recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            recorder.setAudioEncodingBitRate(128000);
            recorder.setAudioSamplingRate(44100);
            recorder.setOutputFile(temporaryFile.getAbsolutePath());
            recorder.prepare();
            recorder.start();
            recording = true;
            recordButton.setText("Detener grabación");
            selectButton.setEnabled(false);
            status.setText("");
            status.setVisibility(View.GONE);
        } catch (IOException | RuntimeException exception) {
            if (recorder != null) {
                recorder.release();
                recorder = null;
            }
            recording = false;
            deleteTemporaryFile();
            recordButton.setText("Grabar audio");
            selectButton.setEnabled(true);
            Toast.makeText(activity, "No se pudo iniciar la grabación", Toast.LENGTH_SHORT).show();
        }
    }

    @SuppressLint("SetTextI18n")
    public void stopRecording(boolean save) {
        if (recorder == null) return;
        boolean validRecording = true;
        try {
            recorder.stop();
        } catch (RuntimeException exception) {
            validRecording = false;
        } finally {
            recorder.release();
            recorder = null;
            recording = false;
        }
        MaterialButton recordButton = activity.findViewById(R.id.btnCapturarArchivo);
        View selectButton = activity.findViewById(R.id.btnSeleccionarArchivo);
        TextView status = activity.findViewById(R.id.tvEstadoArchivo);
        if (recordButton != null) recordButton.setText("Grabar audio");
        if (selectButton != null) selectButton.setEnabled(true);
        if (save && validRecording && temporaryFile != null && temporaryFile.length() > 0) {
            showPreview(Uri.fromFile(temporaryFile));
        } else {
            deleteTemporaryFile();
            selectedUri = null;
            if (save) {
                if (status != null) {
                    status.setText("No se pudo guardar la grabación");
                    status.setVisibility(View.VISIBLE);
                }
                Toast.makeText(activity, "La grabación fue demasiado corta o no se pudo guardar", Toast.LENGTH_SHORT).show();
            } else {
                if (status != null) {
                    status.setText("");
                    status.setVisibility(View.GONE);
                }
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private void showPreview(Uri uri) {
        releasePlayer();
        View controls = activity.findViewById(R.id.controlesAudio);
        TextView status = activity.findViewById(R.id.tvEstadoArchivo);
        ImageButton playButton = activity.findViewById(R.id.btnPlayPausaAudio);
        SeekBar seekBar = activity.findViewById(R.id.seekBarAudio);
        TextView currentTime = activity.findViewById(R.id.tvTiempoActualAudio);
        TextView totalTime = activity.findViewById(R.id.tvDuracionAudio);
        if (controls == null || status == null || playButton == null || seekBar == null || currentTime == null || totalTime == null) return;
        selectedUri = uri;
        seeking = false;
        View photoPreview = activity.findViewById(R.id.imgVistaPreviaRecuerdo);
        View videoPreview = activity.findViewById(R.id.contenedorVideoPreview);
        if (photoPreview != null) photoPreview.setVisibility(View.GONE);
        if (videoPreview != null) videoPreview.setVisibility(View.GONE);
        controls.setVisibility(View.VISIBLE);
        status.setText("");
        status.setVisibility(View.GONE);
        playButton.setImageResource(android.R.drawable.ic_media_play);
        playButton.setEnabled(false);
        seekBar.setProgress(0);
        seekBar.setMax(1000);
        seekBar.setEnabled(false);
        currentTime.setText("00:00");
        totalTime.setText("00:00");
        player = new MediaPlayer();
        MediaPlayer currentPlayer = player;
        playButton.setOnClickListener(view -> {
            if (player != currentPlayer) return;
            if (currentPlayer.isPlaying()) {
                currentPlayer.pause();
                playButton.setImageResource(android.R.drawable.ic_media_play);
            } else {
                currentPlayer.start();
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
                seeking = true;
            }
            @Override
            public void onStopTrackingTouch(SeekBar bar) {
                seeking = false;
                if (player == currentPlayer) currentPlayer.seekTo(bar.getProgress());
            }
        });

        currentPlayer.setOnPreparedListener(mediaPlayer -> {
            if (player != mediaPlayer) return;
            int duration = Math.max(mediaPlayer.getDuration(), 0);
            seekBar.setMax(Math.max(duration, 1));
            totalTime.setText(formatTime(duration));
            playButton.setEnabled(true);
            seekBar.setEnabled(true);
            if (progressUpdater != null) handler.removeCallbacks(progressUpdater);
            progressUpdater = new Runnable() {
                @Override
                public void run() {
                    if (player != mediaPlayer) return;
                    if (!seeking) {
                        int position = mediaPlayer.getCurrentPosition();
                        seekBar.setProgress(position);
                        currentTime.setText(formatTime(position));
                    }
                    handler.postDelayed(this, 250);
                }
            };
            handler.post(progressUpdater);
        });
        currentPlayer.setOnCompletionListener(mediaPlayer -> {
            if (player != mediaPlayer) return;
            playButton.setImageResource(android.R.drawable.ic_media_play);
            seekBar.setProgress(0);
            currentTime.setText("00:00");
            mediaPlayer.seekTo(0);
        });
        currentPlayer.setOnErrorListener((mediaPlayer, what, extra) -> {
            if (player != mediaPlayer) return true;
            selectedUri = null;
            status.setText("No se pudo reproducir este audio");
            status.setVisibility(View.VISIBLE);
            playButton.setEnabled(false);
            seekBar.setEnabled(false);
            return true;
        });
        try {
            currentPlayer.setDataSource(activity, uri);
            currentPlayer.prepareAsync();
        } catch (IOException | RuntimeException exception) {
            releasePlayer();
            selectedUri = null;
            controls.setVisibility(View.GONE);
            status.setText("No se pudo abrir el archivo de audio");
            status.setVisibility(View.VISIBLE);
        }
    }

    public void pausePlayback() {
        if (player == null) return;
        try {
            if (player.isPlaying()) {
                player.pause();
                ImageButton playButton = activity.findViewById(R.id.btnPlayPausaAudio);
                if (playButton != null) playButton.setImageResource(android.R.drawable.ic_media_play);
            }
        } catch (IllegalStateException exception) {
            releasePlayer();
        }
    }

    private void releasePlayer() {
        if (progressUpdater != null) {
            handler.removeCallbacks(progressUpdater);
            progressUpdater = null;
        }
        if (player != null) {
            player.setOnPreparedListener(null);
            player.setOnCompletionListener(null);
            player.setOnErrorListener(null);
            player.release();
            player = null;
        }
        seeking = false;
    }

    private void deleteTemporaryFile() {
        if (temporaryFile != null) {
            boolean deleted = temporaryFile.delete();
            if (!deleted) Log.w("Capsly", "No se pudo eliminar el archivo temporal");
            temporaryFile = null;
        }
    }

    public void clearSelection() {
        if (recording) stopRecording(false);
        releasePlayer();
        selectedUri = null;
        deleteTemporaryFile();
    }

    public void onStop() {
        if (recording) stopRecording(false);
        pausePlayback();
    }

    public void release() {
        clearSelection();
    }

    private String formatTime(int milliseconds) {
        int totalSeconds = milliseconds / 1000;
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
    }
}