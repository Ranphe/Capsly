package com.ranphe.capsly.data.local;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import com.ranphe.capsly.security.PasswordSecurity;
import com.ranphe.capsly.data.model.Capsule;
import com.ranphe.capsly.data.model.UnlockedCapsule;
import com.ranphe.capsly.data.model.Quote;
import java.security.GeneralSecurityException;
import java.util.Locale;
import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "capsly.db";
    private static final int DATABASE_VERSION = 4;
    public static final String TABLE_USERS = "usuarios";
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_NAME = "nombre";
    public static final String COLUMN_EMAIL = "correo";
    public static final String COLUMN_PASSWORD_HASH = "password_hash";
    public static final String COLUMN_PASSWORD_SALT = "password_salt";
    public static final String TABLE_CAPSULES = "capsulas";
    public static final String COLUMN_SENDER_ID = "remitente_id";
    public static final String COLUMN_RECIPIENT_ID = "destinatario_id";
    public static final String COLUMN_TITLE = "titulo";
    public static final String COLUMN_MESSAGE = "mensaje";
    public static final String COLUMN_FORMAT = "formato";
    public static final String COLUMN_FILE_PATH = "ruta_archivo";
    public static final String COLUMN_OPEN_DATE = "fecha_apertura";
    public static final String COLUMN_CREATED_AT = "fecha_creacion";
    public static final String COLUMN_OPENED_AT = "fecha_primera_apertura";
    public static final String TABLE_QUOTES = "frases_guardadas";
    public static final String COLUMN_USER_ID = "usuario_id";
    public static final String COLUMN_QUOTE = "frase";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onConfigure(SQLiteDatabase database) {
        super.onConfigure(database);
        database.setForeignKeyConstraintsEnabled(true);
    }

    @Override
    public void onCreate(SQLiteDatabase database) {
        String createUsersSql = "CREATE TABLE " + TABLE_USERS + " (" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_NAME + " TEXT NOT NULL, " +
                COLUMN_EMAIL + " TEXT NOT NULL COLLATE NOCASE UNIQUE, " +
                COLUMN_PASSWORD_HASH + " TEXT NOT NULL, " +
                COLUMN_PASSWORD_SALT + " TEXT NOT NULL)";
        database.execSQL(createUsersSql);
        createCapsulesTable(database);
        createQuotesTable(database);
    }

    private void createCapsulesTable(SQLiteDatabase database) {
        String sql = "CREATE TABLE IF NOT EXISTS " + TABLE_CAPSULES + " (" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_SENDER_ID + " INTEGER NOT NULL, " +
                COLUMN_RECIPIENT_ID + " INTEGER NOT NULL, " +
                COLUMN_TITLE + " TEXT NOT NULL, " +
                COLUMN_MESSAGE + " TEXT NOT NULL, " +
                COLUMN_FORMAT + " TEXT NOT NULL, " +
                COLUMN_FILE_PATH + " TEXT NOT NULL, " +
                COLUMN_OPEN_DATE + " INTEGER NOT NULL, " +
                COLUMN_CREATED_AT + " INTEGER NOT NULL, " +
                COLUMN_OPENED_AT + " INTEGER, " +
                "FOREIGN KEY(" + COLUMN_SENDER_ID + ") REFERENCES " + TABLE_USERS + "(" + COLUMN_ID + ") ON DELETE RESTRICT, " +
                "FOREIGN KEY(" + COLUMN_RECIPIENT_ID + ") REFERENCES " + TABLE_USERS + "(" + COLUMN_ID + ") ON DELETE RESTRICT)";
        database.execSQL(sql);
        database.execSQL("CREATE INDEX IF NOT EXISTS idx_capsulas_destinatario ON " + TABLE_CAPSULES + "(" + COLUMN_RECIPIENT_ID + ")");
    }

    private void createQuotesTable(SQLiteDatabase database) {
        String sql = "CREATE TABLE IF NOT EXISTS " + TABLE_QUOTES + " (" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_USER_ID + " INTEGER NOT NULL, " +
                COLUMN_QUOTE + " TEXT NOT NULL, " +
                COLUMN_CREATED_AT + " INTEGER NOT NULL, " +
                "FOREIGN KEY(" + COLUMN_USER_ID + ") REFERENCES " +
                TABLE_USERS + "(" + COLUMN_ID + ") ON DELETE RESTRICT)";
        database.execSQL(sql);
        database.execSQL("CREATE INDEX IF NOT EXISTS idx_frases_usuario ON " + TABLE_QUOTES + "(" + COLUMN_USER_ID + ")");
    }

    // Las bases de datos antiguas pueden tener versiones iguales con esquemas distintos.
    // Se comprueba la columna antes de agregarla para conservar los datos existentes.
    private boolean hasColumn(SQLiteDatabase database, String table, String column) {
        try (Cursor cursor = database.rawQuery("PRAGMA table_info(" + table + ")", null)) {
            int nameIndex = cursor.getColumnIndexOrThrow("name");
            while (cursor.moveToNext()) {
                if (column.equals(cursor.getString(nameIndex))) return true;
            }
        }
        return false;
    }

    @Override
    public void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion) {
        if (newVersion > DATABASE_VERSION) {
            throw new IllegalStateException("Database migration is not implemented");
        }
        if (oldVersion < 4) {
            createCapsulesTable(database);
            if (!hasColumn(database, TABLE_CAPSULES, COLUMN_OPENED_AT)) {
                database.execSQL("ALTER TABLE " + TABLE_CAPSULES +
                        " ADD COLUMN " + COLUMN_OPENED_AT + " INTEGER");
            }
            createQuotesTable(database);
        }
    }

    public boolean registerUser(String name, String email, String password) throws GeneralSecurityException {
        String salt = PasswordSecurity.generateSalt();
        String hash = PasswordSecurity.hashPassword(password, salt);
        ContentValues values = new ContentValues();
        values.put(COLUMN_NAME, name.trim());
        values.put(COLUMN_EMAIL, email.trim().toLowerCase(Locale.ROOT));
        values.put(COLUMN_PASSWORD_HASH, hash);
        values.put(COLUMN_PASSWORD_SALT, salt);
        try (SQLiteDatabase database = getWritableDatabase()) {
            database.insertOrThrow(TABLE_USERS, null, values);
            return true;
        } catch (SQLiteConstraintException e) {
            return false;
        }
    }

    public String authenticateUser(String email, String password) throws GeneralSecurityException {
        try (SQLiteDatabase database = getReadableDatabase(); Cursor cursor = database.query(TABLE_USERS, new String[]{COLUMN_NAME, COLUMN_PASSWORD_HASH, COLUMN_PASSWORD_SALT}, COLUMN_EMAIL + " = ?", new String[]{email.trim().toLowerCase(Locale.ROOT)}, null, null, null)) {
            if (!cursor.moveToFirst()) return null;
            String name = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NAME));
            String hash = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PASSWORD_HASH));
            String salt = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PASSWORD_SALT));
            return PasswordSecurity.verifyPassword(password, salt, hash) ? name : null;
        }
    }

    private long findUserIdByEmail(SQLiteDatabase database, String email) {
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        try (Cursor cursor = database.query(TABLE_USERS, new String[]{COLUMN_ID}, COLUMN_EMAIL + " = ?", new String[]{normalizedEmail}, null, null, null)) {
            if (cursor.moveToFirst()) return cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID));
            return -1;
        }
    }

    public boolean userExists(String email) {
        try (SQLiteDatabase database = getReadableDatabase()) {
            return findUserIdByEmail(database, email) != -1;
        }
    }

    public long saveCapsule(String senderEmail, String recipientEmail, String title, String message, String format, String filePath, long openDate) {
        SQLiteDatabase database = getWritableDatabase();
        try {
            database.beginTransaction();
            long senderId = findUserIdByEmail(database, senderEmail);
            long recipientId = findUserIdByEmail(database, recipientEmail);
            if (senderId == -1) return -2;
            if (recipientId == -1) return -1;
            ContentValues values = new ContentValues();
            values.put(COLUMN_SENDER_ID, senderId);
            values.put(COLUMN_RECIPIENT_ID, recipientId);
            values.put(COLUMN_TITLE, title);
            values.put(COLUMN_MESSAGE, message);
            values.put(COLUMN_FORMAT, format);
            values.put(COLUMN_FILE_PATH, filePath);
            values.put(COLUMN_OPEN_DATE, openDate);
            values.put(COLUMN_CREATED_AT, System.currentTimeMillis());
            long capsuleId = database.insertOrThrow(TABLE_CAPSULES, null, values);
            database.setTransactionSuccessful();
            return capsuleId;
        } finally {
            if (database.inTransaction()) database.endTransaction();
            database.close();
        }
    }

    public List<Capsule> getReceivedCapsules(String recipientEmail) {
        List<Capsule> capsules = new ArrayList<>();
        SQLiteDatabase database = getReadableDatabase();
        long recipientId = findUserIdByEmail(database, recipientEmail);
        if (recipientId == -1) return capsules;
        try (Cursor cursor = database.query(TABLE_CAPSULES, new String[]{COLUMN_ID, COLUMN_TITLE, COLUMN_FORMAT, COLUMN_OPEN_DATE, COLUMN_OPENED_AT}, COLUMN_RECIPIENT_ID + " = ?", new String[]{String.valueOf(recipientId)}, null, null, COLUMN_OPEN_DATE + " ASC")) {
            while (cursor.moveToNext()) {
                long id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID));
                String format = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_FORMAT));
                long openingDate = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_OPEN_DATE));
                boolean opened = !cursor.isNull(cursor.getColumnIndexOrThrow(COLUMN_OPENED_AT));
                String title = opened ? cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TITLE)) : null;
                capsules.add(new Capsule(id, title, format, openingDate, opened));
            }
        }
        return capsules;
    }

    public Capsule getReceivedCapsule(long capsuleId, String recipientEmail) {
        SQLiteDatabase database = getReadableDatabase();
        long recipientId = findUserIdByEmail(database, recipientEmail);
        if (recipientId == -1) return null;
        String selection = COLUMN_ID + " = ? AND " + COLUMN_RECIPIENT_ID + " = ?";
        String[] selectionArgs = {String.valueOf(capsuleId), String.valueOf(recipientId)};
        try (Cursor cursor = database.query(TABLE_CAPSULES, new String[]{COLUMN_ID, COLUMN_TITLE, COLUMN_FORMAT, COLUMN_OPEN_DATE, COLUMN_OPENED_AT}, selection, selectionArgs, null, null, null)) {
            if (!cursor.moveToFirst()) return null;
            long id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID));
            String format = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_FORMAT));
            long openingDate = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_OPEN_DATE));
            boolean opened = !cursor.isNull(cursor.getColumnIndexOrThrow(COLUMN_OPENED_AT));
            String title = opened ? cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TITLE)) : null;
            return new Capsule(id, title, format, openingDate, opened);
        }
    }

    public long getReceivedCapsuleCreationDate(long capsuleId, String recipientEmail) {
        if (recipientEmail == null || recipientEmail.trim().isEmpty()) return -1L;
        try (SQLiteDatabase database = getReadableDatabase()) {
            long recipientId = findUserIdByEmail(database, recipientEmail);
            if (recipientId == -1L) return -1L;
            String selection = COLUMN_ID + " = ? AND " + COLUMN_RECIPIENT_ID + " = ?";
            String[] arguments = {String.valueOf(capsuleId), String.valueOf(recipientId)};
            try (Cursor cursor = database.query(TABLE_CAPSULES, new String[]{COLUMN_CREATED_AT}, selection, arguments, null, null, null)) {
                if (!cursor.moveToFirst()) return -1L;
                return cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_CREATED_AT));
            }
        }
    }

    public UnlockedCapsule getUnlockedCapsule(long capsuleId, String recipientEmail) {
        if (recipientEmail == null || recipientEmail.trim().isEmpty()) return null;
        SQLiteDatabase database = getReadableDatabase();
        long recipientId = findUserIdByEmail(database, recipientEmail);
        if (recipientId == -1) return null;
        String selection = COLUMN_ID + " = ? AND " + COLUMN_RECIPIENT_ID + " = ? AND " + COLUMN_OPEN_DATE + " <= ?";
        String[] arguments = {
                String.valueOf(capsuleId),
                String.valueOf(recipientId),
                String.valueOf(System.currentTimeMillis())
        };
        try (Cursor cursor = database.query(TABLE_CAPSULES, null, selection, arguments, null, null, null)) {
            if (!cursor.moveToFirst()) return null;
            long id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID));
            String title = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TITLE));
            String message = cursor.getString(cursor.getColumnIndexOrThrow("mensaje"));
            String format = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_FORMAT));
            String filePath = cursor.getString(cursor.getColumnIndexOrThrow("ruta_archivo"));
            long openingDate = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_OPEN_DATE));
            long createdAt = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_CREATED_AT));
            return new UnlockedCapsule(id, title, message, format, filePath, openingDate, createdAt);
        }
    }

    public void markCapsuleOpened(long capsuleId, String recipientEmail) {
        if (recipientEmail == null || recipientEmail.trim().isEmpty()) return;
        try (SQLiteDatabase database = getWritableDatabase()) {
            long recipientId = findUserIdByEmail(database, recipientEmail);
            if (recipientId == -1L) return;
            long now = System.currentTimeMillis();
            ContentValues values = new ContentValues();
            values.put(COLUMN_OPENED_AT, now);
            String selection = COLUMN_ID + " = ? AND " +
                    COLUMN_RECIPIENT_ID + " = ? AND " +
                    COLUMN_OPEN_DATE + " <= ? AND " +
                    COLUMN_OPENED_AT + " IS NULL";
            String[] arguments = {
                    String.valueOf(capsuleId),
                    String.valueOf(recipientId),
                    String.valueOf(now)
            };
            database.update(TABLE_CAPSULES, values, selection, arguments);
        }
    }

    public boolean updateUserName(String email, String name) {
        if (email == null || name == null) return false;
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        if (normalizedEmail.isEmpty() || name.length() < 2 || name.length() > 20 || name.matches(".*\\s.*")) return false;
        ContentValues values = new ContentValues();
        values.put(COLUMN_NAME, name);
        try (SQLiteDatabase database = getWritableDatabase()) {
            return database.update(TABLE_USERS, values, COLUMN_EMAIL + " = ?", new String[]{normalizedEmail}) == 1;
        }
    }

    public long saveQuote(String email, String quote) {
        SQLiteDatabase database = getWritableDatabase();
        long userId = findUserIdByEmail(database, email);
        if (userId == -1 || quote == null || quote.trim().isEmpty()) {return -1;}
        ContentValues values = new ContentValues();
        values.put(COLUMN_USER_ID, userId);
        values.put(COLUMN_QUOTE, quote.trim());
        values.put(COLUMN_CREATED_AT, System.currentTimeMillis());
        return database.insertOrThrow(TABLE_QUOTES, null, values);
    }

    public List<Quote> getSavedQuotes(String email) {
        List<Quote> quotes = new ArrayList<>();
        SQLiteDatabase database = getReadableDatabase();
        long userId = findUserIdByEmail(database, email);
        if (userId == -1) {throw new IllegalStateException("Usuario no encontrado");}
        try (Cursor cursor = database.query(
                TABLE_QUOTES,
                new String[]{COLUMN_ID, COLUMN_QUOTE},
                COLUMN_USER_ID + " = ?",
                new String[]{String.valueOf(userId)},
                null,
                null,
                COLUMN_CREATED_AT + " DESC, " + COLUMN_ID + " DESC"
        )) {
            while (cursor.moveToNext()) {
                long id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID));
                String text = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_QUOTE));
                quotes.add(new Quote(id, text));
            }
        }
        return quotes;
    }

    public boolean updateQuote(long quoteId, String email, String newText) {
        SQLiteDatabase database = getWritableDatabase();
        long userId = findUserIdByEmail(database, email);
        if (userId == -1 || newText == null || newText.trim().isEmpty()) {return false;}
        ContentValues values = new ContentValues();
        values.put(COLUMN_QUOTE, newText.trim());
        int updated = database.update(TABLE_QUOTES, values, COLUMN_ID + " = ? AND " + COLUMN_USER_ID + " = ?",
                new String[]{
                        String.valueOf(quoteId),
                        String.valueOf(userId)
                }
        );
        return updated > 0;
    }

    public boolean deleteQuote(long quoteId, String email) {
        SQLiteDatabase database = getWritableDatabase();
        long userId = findUserIdByEmail(database, email);
        if (userId == -1) {return false;}
        int deleted = database.delete(TABLE_QUOTES, COLUMN_ID + " = ? AND " + COLUMN_USER_ID + " = ?",
                new String[]{
                        String.valueOf(quoteId),
                        String.valueOf(userId)
                }
        );
        return deleted > 0;
    }
}
