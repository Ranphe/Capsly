package com.ranphe.capsly.ui.welcome;

import android.app.Activity;
import com.ranphe.capsly.R;

public final class WelcomeScreen {
    private WelcomeScreen() {}

    public static void bind(Activity activity, Runnable onLogin, Runnable onRegister) {
        activity.findViewById(R.id.button).setOnClickListener(view -> onLogin.run());
        activity.findViewById(R.id.button3).setOnClickListener(view -> onRegister.run());
    }
}
