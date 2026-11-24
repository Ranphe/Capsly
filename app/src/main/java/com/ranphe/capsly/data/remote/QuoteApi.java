package com.ranphe.capsly.data.remote;

import android.content.Context;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.TimeoutError;
import com.android.volley.NoConnectionError;
import com.android.volley.ParseError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

public final class QuoteApi {

    private static final String URL = "https://www.positive-api.online/phrase/esp";
    private final RequestQueue requestQueue;
    public interface QuoteCallback {
        void onSuccess(String quote);
        void onError(String message);}
    public QuoteApi(Context context) {
        requestQueue = Volley.newRequestQueue(
                context.getApplicationContext());
    }
    public void getRandomQuote(QuoteCallback callback) {
        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET,
                URL,
                null,
                response -> {
                    Object value = response.opt("text");
                    String quote = value instanceof String
                            ? ((String) value).trim()
                            : "";

                    if (quote.isEmpty()) {
                        callback.onError("No se recibió una frase válida.");
                        return;
                    }
                    callback.onSuccess(quote);
                },
                error -> {
                    if (error instanceof NoConnectionError) {
                        callback.onError("No hay conexión a Internet.");
                    } else if (error instanceof TimeoutError) {
                        callback.onError("El servidor tardó demasiado.");
                    } else if (error instanceof ParseError) {
                        callback.onError("La respuesta no es válida.");
                    } else if (error.networkResponse != null) {
                        callback.onError("Error del servidor: " + error.networkResponse.statusCode);
                    } else {
                        callback.onError("No se pudo obtener una frase.");
                    }
                }
        );

        request.setShouldCache(false);
        request.setRetryPolicy(
                new DefaultRetryPolicy(10000, 0, 1f)
        );
        request.setTag(this);
        requestQueue.add(request);
    }

    public void close() {
        requestQueue.cancelAll(this);
        requestQueue.stop();
    }
}
