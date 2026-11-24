package com.ranphe.capsly.ui.inspiration;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.ranphe.capsly.R;
import com.ranphe.capsly.data.model.Quote;
import com.ranphe.capsly.data.remote.QuoteApi;
import com.ranphe.capsly.data.repository.QuoteRepository;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.widget.ImageButton;
import android.util.TypedValue;

public final class InspirationActivity extends AppCompatActivity {
    public static final String EXTRA_USER_EMAIL = "user_email";

    private TextView tvFrase;
    private TextView tvSinFrases;
    private Button btnNueva;
    private Button btnGuardar;
    private ProgressBar progreso;
    private TableLayout tabla;
    private QuoteApi quoteApi;
    private QuoteRepository repository;
    private String userEmail;
    private String fraseActual = "";
    private boolean cargando = false;
    private boolean guardada = false;
    private boolean operacionBD = false;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.enableEdgeToEdge(getWindow());
        WindowCompat.getInsetsController(
                getWindow(),
                getWindow().getDecorView()
        ).setAppearanceLightStatusBars(true);
        setContentView(R.layout.page_inspiration);
        applyWindowInsets();

        userEmail = getIntent().getStringExtra(EXTRA_USER_EMAIL);
        if (userEmail == null || userEmail.trim().isEmpty()) {
            mostrarMensaje("No se encontró el usuario.");
            finish();
            return;
        }

        repository = new QuoteRepository(this);
        quoteApi = new QuoteApi(this);
        inicializarVistas();
        configurarBotones();
        cargarFrasesGuardadas();
        obtenerNuevaFrase();
    }

    private void inicializarVistas() {
        tvFrase = findViewById(R.id.tvFraseInspiracion);
        tvSinFrases = findViewById(R.id.tvSinFrases);
        btnNueva = findViewById(R.id.btnNuevaFrase);
        btnGuardar = findViewById(R.id.btnGuardarFrase);
        progreso = findViewById(R.id.progresoFrase);
        tabla = findViewById(R.id.tablaFrasesGuardadas);
    }

    private void configurarBotones() {
        findViewById(R.id.btnVolverCrearRecuerdo).setOnClickListener(view -> finish());
        btnNueva.setOnClickListener(view -> obtenerNuevaFrase());
        btnGuardar.setOnClickListener(view -> guardarFrase());
    }


    private void obtenerNuevaFrase() {
        if (cargando) {
            return;
        }

        cargando = true;
        guardada = false;
        fraseActual = "";
        progreso.setVisibility(View.VISIBLE);
        tvFrase.setText("Buscando inspiración...");
        actualizarBotones();
        quoteApi.getRandomQuote(new QuoteApi.QuoteCallback() {
            @Override
            public void onSuccess(String quote) {
                if (isFinishing() || isDestroyed()) {return;}
                fraseActual = quote;
                tvFrase.setText(quote);
                cargando = false;
                progreso.setVisibility(View.GONE);
                actualizarBotones();
            }

            @Override
            public void onError(String message) {
                if (isFinishing() || isDestroyed()) {return;}
                cargando = false;
                tvFrase.setText(message);
                progreso.setVisibility(View.GONE);
                actualizarBotones();
            }
        });
    }


    private void guardarFrase() {
        if (fraseActual.isEmpty() || guardada || cargando || operacionBD) {return;}
        String texto = fraseActual;
        ejecutarOperacionBD(() -> {
            long id = repository.saveQuote(userEmail, texto);
            if (id == -1) {
                throw new IllegalStateException("No se pudo guardar");
            }
            return repository.getSavedQuotes(userEmail);
        }, "Frase guardada", true);
    }

    private void cargarFrasesGuardadas() {
        ejecutarOperacionBD(
                () -> repository.getSavedQuotes(userEmail),
                null,
                false
        );
    }

    private void mostrarFrases(List<Quote> frases) {
        tabla.removeAllViews();
        tvSinFrases.setVisibility(frases.isEmpty() ? View.VISIBLE : View.GONE);
        for (Quote quote : frases) {
            agregarFila(quote);
        }
    }


    private void agregarFila(Quote quote) {
        TableRow fila = new TableRow(this);
        fila.setGravity(Gravity.CENTER_VERTICAL);
        fila.setPadding(0, dp(6), 0, dp(6));
        TextView texto = new TextView(this);
        texto.setText(quote.getText());
        texto.setTextSize(14);
        texto.setTextColor(Color.parseColor("#252F27"));
        texto.setTypeface(androidx.core.content.res.ResourcesCompat.getFont(this, R.font.lora_bold));
        texto.setPadding(0, dp(8), dp(8), dp(8));
        texto.setLayoutParams(new TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1f));
        ImageButton btnCopiar = crearBotonIcono(R.drawable.ic_copy, "Copiar frase", "#34665A");
        btnCopiar.setOnClickListener(view -> copiarFrase(quote.getText()));
        ImageButton btnEditar = crearBotonIcono(R.drawable.ic_edit, "Editar frase", "#34665A");
        btnEditar.setOnClickListener(view -> mostrarDialogoEditar(quote));
        ImageButton btnBorrar = crearBotonIcono(R.drawable.ic_delete, "Eliminar frase", "#9B4945");
        btnBorrar.setOnClickListener(view -> mostrarDialogoEliminar(quote));
        fila.addView(texto);
        fila.addView(btnCopiar);
        fila.addView(btnEditar);
        fila.addView(btnBorrar);
        tabla.addView(fila);
        View linea = new View(this);
        linea.setLayoutParams(new TableLayout.LayoutParams(TableLayout.LayoutParams.MATCH_PARENT, dp(1)));
        linea.setBackgroundColor(Color.parseColor("#CCD9D0"));
        tabla.addView(linea);
    }




    private ImageButton crearBotonIcono(
            int icono,
            String descripcion,
            String color
    ) {
        ImageButton boton = new ImageButton(this);
        boton.setImageResource(icono);
        boton.setImageTintList(ColorStateList.valueOf(Color.parseColor(color)));
        TypedValue valor = new TypedValue();
        getTheme().resolveAttribute(android.R.attr.selectableItemBackgroundBorderless, valor, true);
        boton.setBackgroundResource(valor.resourceId);
        boton.setScaleType(ImageButton.ScaleType.CENTER);
        boton.setPadding(dp(8), dp(8), dp(8), dp(8));
        boton.setContentDescription(descripcion);
        TableRow.LayoutParams params = new TableRow.LayoutParams(dp(48), dp(48));
        boton.setLayoutParams(params);
        return boton;
    }

    private void copiarFrase(String frase) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null) {
            ClipData clip = ClipData.newPlainText("Frase inspiradora", frase);
            clipboard.setPrimaryClip(clip);
            if (android.os.Build.VERSION.SDK_INT < 33) {mostrarMensaje("Frase copiada");}
        } else {
            mostrarMensaje("No se pudo copiar la frase");
        }
    }

    private void mostrarDialogoEditar(Quote quote) {
        if (operacionBD) {return;}
        EditText entrada = new EditText(this);
        entrada.setText(quote.getText());
        entrada.setSelection(entrada.getText().length());
        entrada.setPadding(dp(20), dp(12), dp(20), dp(12));
        AlertDialog dialogo = new AlertDialog.Builder(this)
                .setTitle("Editar frase")
                .setView(entrada)
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Guardar", null)
                .create();

        dialogo.setOnShowListener(view -> {
            dialogo.getButton(AlertDialog.BUTTON_POSITIVE)
                    .setOnClickListener(button -> {
                        String nuevoTexto =
                                entrada.getText()
                                        .toString()
                                        .trim();
                        if (nuevoTexto.isEmpty()) {
                            entrada.setError("Escribe una frase");
                            return;
                        }
                        editarFrase(quote.getId(), nuevoTexto);
                        dialogo.dismiss();
                    });
        });
        dialogo.show();
    }


    private void editarFrase(long id, String nuevoTexto) {
        ejecutarOperacionBD(() -> {
            boolean actualizado = repository.updateQuote(id, userEmail, nuevoTexto);
            if (!actualizado) {
                throw new IllegalStateException("No se pudo editar");
            }
            return repository.getSavedQuotes(userEmail);
        }, "Frase actualizada", false);
    }


    private void mostrarDialogoEliminar(Quote quote) {
        if (operacionBD) {return;}
        new AlertDialog.Builder(this)
                .setTitle("Eliminar frase")
                .setMessage("¿Deseas eliminar esta frase guardada?")
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Eliminar", (dialog, which) -> eliminarFrase(quote.getId()))
                .show();
    }

    private void eliminarFrase(long id) {
        ejecutarOperacionBD(() -> {
            boolean eliminado = repository.deleteQuote(id, userEmail);
            if (!eliminado) {
                throw new IllegalStateException("No se pudo eliminar");
            }
            return repository.getSavedQuotes(userEmail);
        }, "Frase eliminada", false);
    }

    private void ejecutarOperacionBD(
            Callable<List<Quote>> operacion,
            String mensaje,
            boolean marcarGuardada
    ) {
        if (operacionBD) {
            return;
        }
        operacionBD = true;
        actualizarBotones();
        executor.execute(() -> {
            try {
                List<Quote> resultado = operacion.call();
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) {return;}
                    operacionBD = false;
                    if (marcarGuardada) {guardada = true;}
                    mostrarFrases(resultado);
                    actualizarBotones();
                    if (mensaje != null) {mostrarMensaje(mensaje);}
                });

            } catch (Exception exception) {
                android.util.Log.e(
                        "InspirationActivity",
                        "Error de SQLite",
                        exception
                );

                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) {return;}
                    operacionBD = false;
                    actualizarBotones();
                    mostrarMensaje("No se pudo completar la operación.");
                });
            }
        });
    }


    private void actualizarBotones() {
        btnNueva.setEnabled(!cargando && !operacionBD);
        btnGuardar.setEnabled(
                !cargando &&
                        !operacionBD &&
                        !guardada &&
                        !fraseActual.isEmpty()
        );
    }

    private void mostrarMensaje(String mensaje) {
        Toast.makeText(
                this,
                mensaje,
                Toast.LENGTH_SHORT
        ).show();
    }

    private int dp(int valor) {
        return Math.round(
                valor * getResources()
                        .getDisplayMetrics().density
        );
    }

    private void applyWindowInsets() {
        View root = findViewById(R.id.inspirationRoot);
        ViewCompat.setOnApplyWindowInsetsListener(
                root,
                (view, insets) -> {
                    Insets systemBars = insets.getInsets(
                            WindowInsetsCompat.Type.systemBars()
                    );
                    view.setPadding(
                            systemBars.left,
                            systemBars.top,
                            systemBars.right,
                            systemBars.bottom
                    );
                    return insets;
                }
        );
        ViewCompat.requestApplyInsets(root);
    }


    @Override
    protected void onDestroy() {
        if (quoteApi != null) {quoteApi.close();}
        executor.shutdown();
        super.onDestroy();
    }
}
