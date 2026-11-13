package com.ranphe.capsly.config;

import com.ranphe.capsly.BuildConfig;

public final class DebugConfig {
    public static final boolean ALLOW_SELF_SEND_CAPSULES = true;

    private DebugConfig() {}

    public static boolean canSendToSelf() {
        return BuildConfig.DEBUG && ALLOW_SELF_SEND_CAPSULES;
    }
}