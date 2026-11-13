package com.ranphe.capsly.ui.vault;

import com.ranphe.capsly.R;

public final class CapsuleAppearance {
    private static final long DAY_MILLISECONDS = 86400000L;
    private static final long UPCOMING_WINDOW_MILLISECONDS = 7L * DAY_MILLISECONDS;

    private CapsuleAppearance() {}

    public static int getImageResource(long openingDate) {
        long remainingTime = openingDate - System.currentTimeMillis();
        if (remainingTime <= 0) return R.drawable.screenpill_y;
        if (remainingTime <= UPCOMING_WINDOW_MILLISECONDS) return R.drawable.screenpill_b;
        return R.drawable.screenpill_p;
    }
}