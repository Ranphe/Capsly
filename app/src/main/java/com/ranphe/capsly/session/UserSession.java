package com.ranphe.capsly.session;

import java.util.Locale;

public final class UserSession {
    private String name = "";
    private String email = "";

    public void signIn(String name, String email) {
        this.name = name;
        this.email = email.trim().toLowerCase(Locale.ROOT);
    }

    public String getName() {
        return name;
    }

    public void updateName(String name) {
        if (isSignedIn()) this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public boolean isSignedIn() {
        return !email.isEmpty();
    }

    public void signOut() {
        name = "";
        email = "";
    }
}