package com.ranphe.capsly.ui.vault;

import com.ranphe.capsly.R;
import com.ranphe.capsly.data.model.Capsule;

public final class CapsuleAppearance {
    private static final long DAY_MILLISECONDS = 86400000L;
    private static final long UPCOMING_WINDOW_MILLISECONDS = 7L * DAY_MILLISECONDS;

    public enum Status {
        OPENED,
        UNLOCKED,
        UPCOMING,
        LOCKED
    }

    private CapsuleAppearance() {}

    public static Status getStatus(Capsule capsule) {
        long remainingTime = capsule.getOpeningDate() - System.currentTimeMillis();
        if (remainingTime <= 0) return capsule.isOpened() ? Status.OPENED : Status.UNLOCKED;
        if (remainingTime <= UPCOMING_WINDOW_MILLISECONDS) return Status.UPCOMING;
        return Status.LOCKED;
    }

    public static int getImageResource(Capsule capsule) {
        switch (getStatus(capsule)) {
            case OPENED:
                return R.drawable.screenpill_o;
            case UNLOCKED:
                return R.drawable.screenpill_u;
            case UPCOMING:
                return R.drawable.screenpill_n;
            default:
                return R.drawable.screenpill_l;
        }
    }
}