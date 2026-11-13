package com.ranphe.capsly.ui.access;

import android.annotation.SuppressLint;
import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;
import android.text.Editable;
import android.text.TextWatcher;
import com.google.android.material.textfield.TextInputEditText;
import com.ranphe.capsly.R;

public final class SignupFormValidator {
    private SignupFormValidator() {}

    public static void bind(View page) {
        TextInputEditText nicknameInput = page.findViewById(R.id.etName);
        TextInputEditText emailInput = page.findViewById(R.id.etCorreo);
        TextInputEditText passwordInput = page.findViewById(R.id.etPassword);
        TextInputEditText confirmationInput = page.findViewById(R.id.etPasswordConfirm);
        CheckBox termsCheckbox = page.findViewById(R.id.checkTerms);
        TextView nicknameError = page.findViewById(R.id.tvNicknameError);
        TextView emailError = page.findViewById(R.id.tvRegisterEmailError);
        TextView passwordError = page.findViewById(R.id.tvRegisterPasswordError);
        TextView confirmationError = page.findViewById(R.id.tvRegisterConfirmPasswordError);
        TextView termsError = page.findViewById(R.id.tvTermsError);
        TextView nicknameCounter = page.findViewById(R.id.tvNicknameCounter);
        bindErrorReset(nicknameInput, nicknameError, nicknameCounter);
        bindErrorReset(emailInput, emailError, null);
        bindErrorReset(passwordInput, passwordError, null);
        bindErrorReset(confirmationInput, confirmationError, null);
        termsCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) termsError.setVisibility(View.GONE);
        });
    }

    @SuppressLint("SetTextI18n")
    public static boolean validate(View page) {
        TextInputEditText nicknameInput = page.findViewById(R.id.etName);
        TextInputEditText emailInput = page.findViewById(R.id.etCorreo);
        TextInputEditText passwordInput = page.findViewById(R.id.etPassword);
        TextInputEditText confirmationInput = page.findViewById(R.id.etPasswordConfirm);
        CheckBox termsCheckbox = page.findViewById(R.id.checkTerms);
        TextView nicknameError = page.findViewById(R.id.tvNicknameError);
        TextView emailError = page.findViewById(R.id.tvRegisterEmailError);
        TextView passwordError = page.findViewById(R.id.tvRegisterPasswordError);
        TextView confirmationError = page.findViewById(R.id.tvRegisterConfirmPasswordError);
        TextView termsError = page.findViewById(R.id.tvTermsError);
        clearErrors(page);
        String nickname = readInput(nicknameInput).trim();
        String email = readInput(emailInput).trim();
        String password = readInput(passwordInput);
        String confirmation = readInput(confirmationInput);
        View firstInvalidView = null;
        boolean valid = true;
        if (nickname.length() < 2 || nickname.length() > 20) {
            nicknameError.setText("El nombre o nick debe tener entre 2 y 20 caracteres");
            nicknameError.setVisibility(View.VISIBLE);
            firstInvalidView = nicknameInput;
            valid = false;
        } else if (!nickname.matches("[\\p{L}\\p{M}\\p{N}_]{2,20}")) {
            nicknameError.setText("Utiliza un solo nombre, sin espacios ni símbolos especiales");
            nicknameError.setVisibility(View.VISIBLE);
            firstInvalidView = nicknameInput;
            valid = false;
        }
        if (email.isEmpty()) {
            emailError.setText("Ingresa tu correo electrónico");
            emailError.setVisibility(View.VISIBLE);
            if (firstInvalidView == null) firstInvalidView = emailInput;
            valid = false;
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailError.setText("Ingresa un correo válido");
            emailError.setVisibility(View.VISIBLE);
            if (firstInvalidView == null) firstInvalidView = emailInput;
            valid = false;
        }
        if (password.isEmpty()) {
            passwordError.setText("Ingresa una contraseña");
            passwordError.setVisibility(View.VISIBLE);
            if (firstInvalidView == null) firstInvalidView = passwordInput;
            valid = false;
        } else if (password.length() < 8) {
            passwordError.setText("La contraseña debe tener mínimo 8 caracteres");
            passwordError.setVisibility(View.VISIBLE);
            if (firstInvalidView == null) firstInvalidView = passwordInput;
            valid = false;
        }
        if (confirmation.isEmpty()) {
            confirmationError.setText("Confirma tu contraseña");
            confirmationError.setVisibility(View.VISIBLE);
            if (firstInvalidView == null) firstInvalidView = confirmationInput;
            valid = false;
        } else if (!password.equals(confirmation)) {
            confirmationError.setText("Las contraseñas no coinciden");
            confirmationError.setVisibility(View.VISIBLE);
            if (firstInvalidView == null) firstInvalidView = confirmationInput;
            valid = false;
        }
        if (!termsCheckbox.isChecked()) {
            termsError.setVisibility(View.VISIBLE);
            if (firstInvalidView == null) firstInvalidView = termsCheckbox;
            valid = false;
        }
        if (!valid) firstInvalidView.requestFocus();
        return valid;
    }

    @SuppressLint("SetTextI18n")
    public static void showEmailRegisteredError(View page) {
        TextView emailError = page.findViewById(R.id.tvRegisterEmailError);
        emailError.setText("Este correo ya está registrado");
        emailError.setVisibility(View.VISIBLE);
    }

    public static void clearErrors(View page) {
        setGone(page, R.id.tvNicknameError);
        setGone(page, R.id.tvRegisterEmailError);
        setGone(page, R.id.tvRegisterPasswordError);
        setGone(page, R.id.tvRegisterConfirmPasswordError);
        setGone(page, R.id.tvTermsError);
    }


    @SuppressLint("SetTextI18n")
    private static void bindErrorReset(TextInputEditText input, TextView errorView, TextView counterView) {
        if (counterView != null) counterView.setText(readInput(input).length() + "/20");
        input.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence text, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence text, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable text) {
                errorView.setVisibility(View.GONE);
                if (counterView != null) counterView.setText(text.length() + "/20");
            }
        });
    }

    private static void setGone(View page, int viewId) {
        View view = page.findViewById(viewId);
        if (view != null) view.setVisibility(View.GONE);
    }

    private static String readInput(TextInputEditText input) {
        return input.getText() == null ? "" : input.getText().toString();
    }
}