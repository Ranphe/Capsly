package com.ranphe.capsly.ui.access;

import android.annotation.SuppressLint;
import android.database.sqlite.SQLiteException;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.text.Editable;
import android.text.TextWatcher;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;
import com.google.android.material.textfield.TextInputEditText;
import com.ranphe.capsly.R;
import com.ranphe.capsly.data.local.DatabaseHelper;
import java.security.GeneralSecurityException;

public class AccessAdapter extends RecyclerView.Adapter<AccessAdapter.PageHolder> {
    public interface AuthenticationListener {
        void onAuthenticated(String name, String email);
    }

    private final AppCompatActivity activity;
    private final ViewPager2 pager;
    private final AuthenticationListener authenticationListener;

    public AccessAdapter(AppCompatActivity activity, ViewPager2 pager, AuthenticationListener authenticationListener) {
        this.activity = activity;
        this.pager = pager;
        this.authenticationListener = authenticationListener;
    }

    @NonNull
    @Override
    public PageHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        FrameLayout container = new FrameLayout(parent.getContext());
        container.setLayoutParams(new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        return new PageHolder(container);
    }

    @Override
    public void onBindViewHolder(@NonNull PageHolder holder, int position) {
        holder.container.removeAllViews();
        int layoutId = position == 0 ? R.layout.page_login : R.layout.page_sign_up;
        View page = LayoutInflater.from(holder.container.getContext()).inflate(layoutId, holder.container, false);
        holder.container.addView(page);
        if (position == 0) {
            bindLogin(page);
        } else {
            bindRegistration(page);
        }
    }

    @Override
    public int getItemCount() {
        return 2;
    }

    @SuppressLint("SetTextI18n")
    private void bindLogin(View page) {
        TextInputEditText emailInput = page.findViewById(R.id.etEmail);
        TextInputEditText passwordInput = page.findViewById(R.id.etPassword);
        TextView emailError = page.findViewById(R.id.tvLoginEmailError);
        TextView passwordError = page.findViewById(R.id.tvLoginPasswordError);
        View loginButton = page.findViewById(R.id.btniniciar);
        View registerButton = page.findViewById(R.id.btnCrearCuenta);
        bindErrorReset(emailInput, emailError);
        bindErrorReset(passwordInput, passwordError);
        registerButton.setOnClickListener(view -> pager.setCurrentItem(1, true));
        loginButton.setOnClickListener(view -> {
            clearLoginErrors(page);
            String email = readInput(emailInput).trim();
            String password = readInput(passwordInput);
            boolean valid = true;
            if (email.isEmpty()) {
                emailError.setText("Ingresa tu correo electrónico");
                emailError.setVisibility(View.VISIBLE);
                valid = false;
            } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                emailError.setText("Ingresa un correo válido");
                emailError.setVisibility(View.VISIBLE);
                valid = false;
            }
            if (password.isEmpty()) {
                passwordError.setText("Ingresa tu contraseña");
                passwordError.setVisibility(View.VISIBLE);
                valid = false;
            }
            if (!valid) {
                if (emailError.getVisibility() == View.VISIBLE) {
                    emailInput.requestFocus();
                } else {
                    passwordInput.requestFocus();
                }
                return;
            }
            loginButton.setEnabled(false);
            new Thread(() -> {
                try {
                    DatabaseHelper database = new DatabaseHelper(activity.getApplicationContext());
                    String name;
                    try {
                        name = database.authenticateUser(email, password);
                    } finally {
                        database.close();
                    }
                    activity.runOnUiThread(() -> {
                        if (isUiInactive()) return;
                        loginButton.setEnabled(true);
                        if (name != null) {
                            Toast.makeText(activity, "Bienvenido, " + name, Toast.LENGTH_SHORT).show();
                            authenticationListener.onAuthenticated(name, email);
                        } else {
                            passwordError.setText("Correo o contraseña incorrectos");
                            passwordError.setVisibility(View.VISIBLE);
                            passwordInput.requestFocus();
                        }
                    });
                } catch (GeneralSecurityException | SQLiteException exception) {
                    activity.runOnUiThread(() -> {
                        if (isUiInactive()) return;
                        loginButton.setEnabled(true);
                        Toast.makeText(activity, "Error al iniciar sesión", Toast.LENGTH_SHORT).show();
                    });
                }
            }).start();
        });
    }

    private void bindRegistration(View page) {
        TextInputEditText nameInput = page.findViewById(R.id.etName);
        TextInputEditText emailInput = page.findViewById(R.id.etCorreo);
        TextInputEditText passwordInput = page.findViewById(R.id.etPassword);
        TextInputEditText confirmationInput = page.findViewById(R.id.etPasswordConfirm);
        View registerButton = page.findViewById(R.id.btncrear);
        SignupFormValidator.bind(page);
        registerButton.setOnClickListener(view -> {
            if (!SignupFormValidator.validate(page)) return;
            String name = readInput(nameInput).trim();
            String email = readInput(emailInput).trim();
            String password = readInput(passwordInput);
            registerButton.setEnabled(false);
            new Thread(() -> {
                try {
                    DatabaseHelper database = new DatabaseHelper(activity.getApplicationContext());
                    boolean registered;
                    try {
                        registered = database.registerUser(name, email, password);
                    } finally {
                        database.close();
                    }
                    activity.runOnUiThread(() -> {
                        if (isUiInactive()) return;
                        registerButton.setEnabled(true);
                        if (registered) {
                            Toast.makeText(activity, "Cuenta creada correctamente", Toast.LENGTH_SHORT).show();
                            nameInput.setText("");
                            emailInput.setText("");
                            passwordInput.setText("");
                            confirmationInput.setText("");
                            SignupFormValidator.clearErrors(page);
                            pager.setCurrentItem(0, true);
                        } else {
                            SignupFormValidator.showEmailRegisteredError(page);
                            emailInput.requestFocus();
                        }
                    });
                } catch (GeneralSecurityException | SQLiteException exception) {
                    activity.runOnUiThread(() -> {
                        if (isUiInactive()) return;
                        registerButton.setEnabled(true);
                        Toast.makeText(activity, "Error al registrar usuario", Toast.LENGTH_SHORT).show();
                    });
                }
            }).start();
        });
    }

    private void bindErrorReset(TextInputEditText input, TextView errorView) {
        input.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence text, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence text, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable text) {
                errorView.setVisibility(View.GONE);
            }
        });
    }

    private void clearLoginErrors(View page) {
        setGone(page, R.id.tvLoginEmailError);
        setGone(page, R.id.tvLoginPasswordError);
    }

    private void setGone(View page, int viewId) {
        View view = page.findViewById(viewId);
        if (view != null) {
            view.setVisibility(View.GONE);
        }
    }

    private String readInput(TextInputEditText input) {
        return input.getText() == null ? "" : input.getText().toString();
    }

    private boolean isUiInactive() {
        return activity.isFinishing() || activity.isDestroyed() || !pager.isAttachedToWindow();
    }

    public static final class PageHolder extends RecyclerView.ViewHolder {
        final FrameLayout container;
        PageHolder(FrameLayout container) {
            super(container);
            this.container = container;
        }
    }
}