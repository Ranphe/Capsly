package com.ranphe.capsly.ui.create;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.View;
import android.widget.TextView;
import com.google.android.material.button.MaterialButton;
import com.ranphe.capsly.R;

public final class MemoryFormatView {
    private MemoryFormatView() {}

    @SuppressLint("SetTextI18n")
    public static void render(Activity activity, String format) {
        MaterialButton photoButton = activity.findViewById(R.id.btnFormatoFoto);
        MaterialButton videoButton = activity.findViewById(R.id.btnFormatoVideo);
        MaterialButton audioButton = activity.findViewById(R.id.btnFormatoAudio);
        updateSelection(photoButton, format.equals("Foto"));
        updateSelection(videoButton, format.equals("Video"));
        updateSelection(audioButton, format.equals("Audio"));
        View filePanel = activity.findViewById(R.id.panelSeleccionArchivo);
        TextView helpText = activity.findViewById(R.id.tvAyudaFormato);
        TextView fileTitle = activity.findViewById(R.id.tvTituloArchivo);
        TextView fileStatus = activity.findViewById(R.id.tvEstadoArchivo);
        MaterialButton selectButton = activity.findViewById(R.id.btnSeleccionarArchivo);
        MaterialButton captureButton = activity.findViewById(R.id.btnCapturarArchivo);
        filePanel.setVisibility(View.VISIBLE);
        helpText.setText("");
        helpText.setVisibility(View.GONE);
        fileStatus.setText("Ningún archivo seleccionado");
        fileStatus.setVisibility(View.VISIBLE);
        activity.findViewById(R.id.imgVistaPreviaRecuerdo).setVisibility(View.GONE);
        activity.findViewById(R.id.contenedorVideoPreview).setVisibility(View.GONE);
        activity.findViewById(R.id.controlesAudio).setVisibility(View.GONE);
        switch (format) {
            case "Foto":
                fileTitle.setText("Agregar fotografía");
                selectButton.setText("Galería");
                captureButton.setText("Tomar foto");
                selectButton.setIconResource(android.R.drawable.ic_menu_gallery);
                captureButton.setIconResource(android.R.drawable.ic_menu_camera);
                break;
            case "Video":
                fileTitle.setText("Agregar video");
                selectButton.setText("Galería");
                captureButton.setText("Grabar video");
                selectButton.setIconResource(android.R.drawable.ic_menu_gallery);
                captureButton.setIconResource(android.R.drawable.ic_menu_camera);
                break;
            case "Audio":
                fileTitle.setText("Agregar audio");
                selectButton.setText("Elegir audio");
                captureButton.setText("Grabar audio");
                selectButton.setIcon(null);
                captureButton.setIcon(null);
                break;
        }
    }

    private static void updateSelection(MaterialButton button, boolean selected) {
        int strokeWidth = Math.round((selected ? 3 : 1) * button.getResources().getDisplayMetrics().density);
        int strokeColor = Color.parseColor(selected ? "#34665A" : "#7F9990");
        button.setStrokeWidth(strokeWidth);
        button.setStrokeColor(ColorStateList.valueOf(strokeColor));
    }
}