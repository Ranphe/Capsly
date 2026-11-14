package com.ranphe.capsly.media.playback;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Toast;
import android.widget.VideoView;
import android.widget.ImageButton;
import android.widget.TextView;
import android.graphics.Matrix;
import androidx.exifinterface.media.ExifInterface;
import com.ranphe.capsly.R;
import java.util.Locale;
import java.io.File;
import java.io.IOException;

public final class UnlockedMediaController {
    private final Activity activity;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private MediaPlayer audioPlayer;
    private VideoView videoView;
    private Runnable progressUpdater;
    private Runnable videoProgressUpdater;
    private boolean videoPrepared = false;
    private boolean videoSeeking = false;
    private boolean audioPrepared = false;
    private boolean seeking = false;
    private boolean released = false;

    public UnlockedMediaController(Activity activity) {
        this.activity = activity;
    }

    public void show(String format, File file) {
        if (file == null || !file.isFile()) {
            Toast.makeText(activity, "No se encontró el archivo de este recuerdo", Toast.LENGTH_LONG).show();
            return;
        }
        switch (format) {
            case "Foto":
                showPhoto(file);
                break;
            case "Video":
                showVideo(file);
                break;
            case "Audio":
                showAudio(file);
                break;
            default:
                Toast.makeText(activity, "Formato de recuerdo no compatible", Toast.LENGTH_SHORT).show();
                break;
        }
    }

    private void showPhoto(File file) {
        ImageView image = activity.findViewById(R.id.imgRecuerdoAbierto);
        image.setVisibility(View.VISIBLE);
        new Thread(() -> {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(file.getAbsolutePath(), options);
            int sampleSize = 1;
            while (options.outWidth / sampleSize > 1600 || options.outHeight / sampleSize > 1600) {
                sampleSize *= 2;
            }
            options.inJustDecodeBounds = false;
            options.inSampleSize = sampleSize;
            options.inPreferredConfig = Bitmap.Config.RGB_565;
            Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath(), options);
            Bitmap orientedBitmap = applyPhotoOrientation(bitmap, file);
            activity.runOnUiThread(() -> {
                if (released || activity.isFinishing() || activity.isDestroyed()) return;
                if (orientedBitmap == null) {
                    Toast.makeText(activity, "No se pudo abrir la fotografía", Toast.LENGTH_SHORT).show();
                    return;
                }
                image.setImageBitmap(orientedBitmap);
            });
        }).start();
    }

    private Bitmap applyPhotoOrientation(Bitmap bitmap, File file) {
        if (bitmap == null) return null;
        try {
            ExifInterface exif = new ExifInterface(file.getAbsolutePath());
            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            Matrix matrix = new Matrix();
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    matrix.setRotate(90);
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    matrix.setRotate(180);
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    matrix.setRotate(270);
                    break;
                case ExifInterface.ORIENTATION_FLIP_HORIZONTAL:
                    matrix.setScale(-1, 1);
                    break;
                case ExifInterface.ORIENTATION_FLIP_VERTICAL:
                    matrix.setScale(1, -1);
                    break;
                case ExifInterface.ORIENTATION_TRANSPOSE:
                    matrix.setRotate(90);
                    matrix.postScale(-1, 1);
                    break;
                case ExifInterface.ORIENTATION_TRANSVERSE:
                    matrix.setRotate(270);
                    matrix.postScale(-1, 1);
                    break;
                default:
                    return bitmap;
            }
            Bitmap correctedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            if (correctedBitmap != bitmap) bitmap.recycle();
            return correctedBitmap;
        } catch (IOException exception) {
            return bitmap;
        }
    }

    @SuppressLint("SetTextI18n")
    private void showVideo(File file) {
        videoView = activity.findViewById(R.id.videoRecuerdoAbierto);
        View videoControls = activity.findViewById(R.id.contenedorVideoRecuerdo);
        SeekBar seekBar = activity.findViewById(R.id.barraVideoRecuerdo);
        ImageButton playButton = activity.findViewById(R.id.btnReproducirVideo);
        TextView currentTime = activity.findViewById(R.id.tvTiempoActualVideo);
        TextView durationTime = activity.findViewById(R.id.tvDuracionVideo);
        currentTime.setText("00:00");
        durationTime.setText("00:00");
        if (videoView == null || videoControls == null || seekBar == null || playButton == null) return;
        VideoView currentVideo = videoView;
        videoPrepared = false;
        videoSeeking = false;
        currentVideo.setVisibility(View.VISIBLE);
        currentVideo.setMediaController(null);
        videoControls.setVisibility(View.VISIBLE);
        playButton.setImageResource(android.R.drawable.ic_media_play);
        playButton.setContentDescription("Reproducir video");
        playButton.setEnabled(false);
        seekBar.setEnabled(false);
        seekBar.setProgress(0);
        seekBar.setMax(1000);
        playButton.setOnClickListener(view -> {
            if (released || !videoPrepared || videoView != currentVideo) return;
            if (currentVideo.isPlaying()) {
                currentVideo.pause();
                playButton.setImageResource(android.R.drawable.ic_media_play);
                playButton.setContentDescription("Reproducir video");
                stopVideoProgressUpdates();
            } else {
                currentVideo.start();
                playButton.setImageResource(android.R.drawable.ic_media_pause);
                playButton.setContentDescription("Pausar video");
                startVideoProgressUpdates(currentVideo, seekBar);
            }
        });
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
                currentTime.setText(formatPlaybackTime(progress));
            }
            @Override
            public void onStartTrackingTouch(SeekBar bar) {
                videoSeeking = true;
            }
            @Override
            public void onStopTrackingTouch(SeekBar bar) {
                videoSeeking = false;
                if (videoPrepared && videoView == currentVideo) currentVideo.seekTo(bar.getProgress());
            }
        });
        currentVideo.setOnPreparedListener(player -> {
            if (released || videoView != currentVideo) return;
            videoPrepared = true;
            int duration = Math.max(player.getDuration(), 1);
            seekBar.setMax(duration);
            durationTime.setText(formatPlaybackTime(duration));
            seekBar.setEnabled(true);
            playButton.setEnabled(true);
            currentVideo.seekTo(1);
        });
        currentVideo.setOnCompletionListener(player -> {
            if (released || videoView != currentVideo) return;
            stopVideoProgressUpdates();
            playButton.setImageResource(android.R.drawable.ic_media_play);
            playButton.setContentDescription("Reproducir video");
            seekBar.setProgress(0);
            currentVideo.seekTo(1);
        });
        currentVideo.setOnErrorListener((player, what, extra) -> {
            if (released || videoView != currentVideo) return true;
            stopVideoProgressUpdates();
            videoPrepared = false;
            playButton.setEnabled(false);
            seekBar.setEnabled(false);
            Toast.makeText(activity, "No se pudo reproducir el video", Toast.LENGTH_SHORT).show();
            return true;
        });
        currentVideo.setVideoURI(Uri.fromFile(file));
    }

    @SuppressLint("SetTextI18n")
    private void showAudio(File file) {
        View audioContainer = activity.findViewById(R.id.contenedorAudioRecuerdo);
        SeekBar seekBar = activity.findViewById(R.id.barraAudioRecuerdo);
        ImageButton playButton = activity.findViewById(R.id.btnReproducirAudio);
        TextView currentTime = activity.findViewById(R.id.tvTiempoActualAudio);
        TextView durationTime = activity.findViewById(R.id.tvDuracionAudio);
        currentTime.setText("00:00");
        durationTime.setText("00:00");
        audioContainer.setVisibility(View.VISIBLE);
        playButton.setEnabled(false);
        playButton.setImageResource(android.R.drawable.ic_media_play);
        playButton.setContentDescription("Reproducir audio");
        seekBar.setEnabled(false);
        seekBar.setProgress(0);
        audioPlayer = new MediaPlayer();
        MediaPlayer currentPlayer = audioPlayer;
        playButton.setOnClickListener(view -> {
            if (!audioPrepared || audioPlayer != currentPlayer) return;
            try {
                if (currentPlayer.isPlaying()) {
                    currentPlayer.pause();
                    playButton.setImageResource(android.R.drawable.ic_media_play);
                    playButton.setContentDescription("Reproducir audio");
                    stopProgressUpdates();
                } else {
                    currentPlayer.start();
                    playButton.setImageResource(android.R.drawable.ic_media_pause);
                    playButton.setContentDescription("Pausar audio");
                    startProgressUpdates(currentPlayer, seekBar);
                }
            } catch (IllegalStateException exception) {
                Toast.makeText(activity, "No se pudo reproducir el audio", Toast.LENGTH_SHORT).show();
            }
        });
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
                currentTime.setText(formatPlaybackTime(progress));
            }
            @Override
            public void onStartTrackingTouch(SeekBar bar) {
                seeking = true;
            }
            @Override
            public void onStopTrackingTouch(SeekBar bar) {
                seeking = false;
                if (audioPrepared && audioPlayer == currentPlayer) currentPlayer.seekTo(bar.getProgress());
            }
        });
        currentPlayer.setOnPreparedListener(player -> {
            if (released || audioPlayer != player) return;
            audioPrepared = true;
            seekBar.setMax(Math.max(player.getDuration(), 1));
            durationTime.setText(formatPlaybackTime(player.getDuration()));
            seekBar.setEnabled(true);
            playButton.setEnabled(true);
        });
        currentPlayer.setOnCompletionListener(player -> {
            if (audioPlayer != player) return;
            stopProgressUpdates();
            playButton.setImageResource(android.R.drawable.ic_media_play);
            playButton.setContentDescription("Reproducir audio");
            seekBar.setProgress(0);
            player.seekTo(0);
        });
        currentPlayer.setOnErrorListener((player, what, extra) -> {
            if (audioPlayer != player) return true;
            stopProgressUpdates();
            playButton.setEnabled(false);
            seekBar.setEnabled(false);
            Toast.makeText(activity, "No se pudo reproducir el audio", Toast.LENGTH_SHORT).show();
            return true;
        });
        try {
            currentPlayer.setDataSource(file.getAbsolutePath());
            currentPlayer.prepareAsync();
        } catch (IOException | RuntimeException exception) {
            playButton.setEnabled(false);
            seekBar.setEnabled(false);
            releaseAudioPlayer();
            Toast.makeText(activity, "No se pudo abrir el archivo de audio", Toast.LENGTH_SHORT).show();
        }
    }

    private void startVideoProgressUpdates(VideoView expectedVideo, SeekBar seekBar) {
        stopVideoProgressUpdates();
        videoProgressUpdater = new Runnable() {
            @Override
            public void run() {
                if (released || videoView != expectedVideo || !videoPrepared) return;
                if (!videoSeeking) seekBar.setProgress(expectedVideo.getCurrentPosition());
                if (expectedVideo.isPlaying()) handler.postDelayed(this, 250);
            }
        };
        handler.post(videoProgressUpdater);
    }

    private void stopVideoProgressUpdates() {
        if (videoProgressUpdater != null) {
            handler.removeCallbacks(videoProgressUpdater);
            videoProgressUpdater = null;
        }
    }

    private void startProgressUpdates(MediaPlayer expectedPlayer, SeekBar seekBar) {
        stopProgressUpdates();
        progressUpdater = new Runnable() {
            @Override
            public void run() {
                if (released || audioPlayer != expectedPlayer || !audioPrepared) return;
                try {
                    if (!seeking) seekBar.setProgress(expectedPlayer.getCurrentPosition());
                    if (expectedPlayer.isPlaying()) handler.postDelayed(this, 250);
                } catch (IllegalStateException exception) {
                    stopProgressUpdates();
                }
            }
        };
        handler.post(progressUpdater);
    }

    private void stopProgressUpdates() {
        if (progressUpdater != null) {
            handler.removeCallbacks(progressUpdater);
            progressUpdater = null;
        }
    }

    public void pause() {
        if (videoView != null && videoView.isPlaying()) {
            videoView.pause();
            ImageButton videoButton = activity.findViewById(R.id.btnReproducirVideo);
            if (videoButton != null) {
                videoButton.setImageResource(android.R.drawable.ic_media_play);
                videoButton.setContentDescription("Reproducir video");
            }
        }
        stopVideoProgressUpdates();
        stopProgressUpdates();
        if (audioPlayer != null && audioPrepared) {
            try {
                if (audioPlayer.isPlaying()) audioPlayer.pause();
            } catch (IllegalStateException exception) {
                releaseAudioPlayer();
            }
            ImageButton playButton = activity.findViewById(R.id.btnReproducirAudio);
            if (playButton != null) {
                playButton.setImageResource(android.R.drawable.ic_media_play);
                playButton.setContentDescription("Reproducir audio");
            }
        }
    }

    private void releaseAudioPlayer() {
        stopProgressUpdates();
        audioPrepared = false;
        seeking = false;
        if (audioPlayer != null) {
            audioPlayer.release();
            audioPlayer = null;
        }
    }

    public void release() {
        released = true;
        stopVideoProgressUpdates();
        releaseAudioPlayer();
        if (videoView != null) {
            videoView.stopPlayback();
            videoView = null;
        }
        videoPrepared = false;
        videoSeeking = false;
    }

    private String formatPlaybackTime(int milliseconds) {
        long totalSeconds = Math.max(0L, milliseconds) / 1000L;
        long hours = totalSeconds / 3600L;
        long minutes = (totalSeconds % 3600L) / 60L;
        long seconds = totalSeconds % 60L;
        if (hours > 0L) return String.format(Locale.ROOT, "%d:%02d:%02d", hours, minutes, seconds);
        return String.format(Locale.ROOT, "%02d:%02d", minutes, seconds);
    }

}