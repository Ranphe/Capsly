package com.ranphe.capsly.ui.create;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.DatePickerDialog;
import com.google.android.material.button.MaterialButton;
import com.ranphe.capsly.R;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public final class OpeningDatePicker {
    public interface OnDateSelectedListener {
        void onDateSelected(long timestamp);
    }

    private OpeningDatePicker() {}

    public static void show(Activity activity, OnDateSelectedListener listener) {
        Calendar minimumDate = Calendar.getInstance();
        minimumDate.set(Calendar.HOUR_OF_DAY, 0);
        minimumDate.set(Calendar.MINUTE, 0);
        minimumDate.set(Calendar.SECOND, 0);
        minimumDate.set(Calendar.MILLISECOND, 0);
        @SuppressLint("SetTextI18n") DatePickerDialog dialog = new DatePickerDialog(activity, (view, year, month, day) -> {
            Calendar selectedDate = Calendar.getInstance();
            selectedDate.set(year, month, day, 0, 0, 0);
            selectedDate.set(Calendar.MILLISECOND, 0);
            long timestamp = selectedDate.getTimeInMillis();
            String formattedDate = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(selectedDate.getTime());
            MaterialButton dateButton = activity.findViewById(R.id.btnFechaApertura);
            dateButton.setText("Apertura: " + formattedDate);
            listener.onDateSelected(timestamp);
        }, minimumDate.get(Calendar.YEAR), minimumDate.get(Calendar.MONTH), minimumDate.get(Calendar.DAY_OF_MONTH));
        dialog.getDatePicker().setMinDate(minimumDate.getTimeInMillis());
        dialog.show();
    }
}