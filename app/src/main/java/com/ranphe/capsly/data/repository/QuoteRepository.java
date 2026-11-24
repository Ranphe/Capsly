package com.ranphe.capsly.data.repository;

import android.content.Context;

import com.ranphe.capsly.data.local.DatabaseHelper;
import com.ranphe.capsly.data.model.Quote;
import java.util.List;

public final class QuoteRepository {

    private final Context context;

    public QuoteRepository(Context context) {this.context = context.getApplicationContext();}
    public long saveQuote(String email, String text) {
        try (DatabaseHelper database =
                     new DatabaseHelper(context)) {
            return database.saveQuote(email, text);
        }
    }
    public List<Quote> getSavedQuotes(String email) {
        try (DatabaseHelper database =
                     new DatabaseHelper(context)) {
            return database.getSavedQuotes(email);
        }
    }

    public boolean updateQuote(long id,
                               String email,
                               String text) {
        try (DatabaseHelper database =
                     new DatabaseHelper(context)) {
            return database.updateQuote(id, email, text);
        }
    }

    public boolean deleteQuote(long id, String email) {
        try (DatabaseHelper database =
                     new DatabaseHelper(context)) {
            return database.deleteQuote(id, email);
        }
    }
}
