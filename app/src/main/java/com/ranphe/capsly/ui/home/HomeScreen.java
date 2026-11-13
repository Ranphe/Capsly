package com.ranphe.capsly.ui.home;

import android.app.Activity;
import android.view.View;
import androidx.viewpager2.widget.ViewPager2;
import com.ranphe.capsly.R;

public final class HomeScreen {
    private HomeScreen() {}

    public static void bind(Activity activity, String userName, String recipientEmail, Runnable onCreateCapsule, Runnable onSignOut) {
        ViewPager2 pager = activity.findViewById(R.id.pagerPrincipal);
        View homeButton = activity.findViewById(R.id.navInicio);
        View createButton = activity.findViewById(R.id.navCrear);
        View vaultButton = activity.findViewById(R.id.navBoveda);
        Runnable onOpenVault = () -> pager.setCurrentItem(1, false);
        Runnable onOpenProfile = () -> pager.setCurrentItem(2, false);
        pager.setAdapter(new HomeAdapter(activity, userName, recipientEmail, onCreateCapsule, onOpenVault, onOpenProfile, onSignOut));
        pager.setUserInputEnabled(false);
        homeButton.setSelected(true);
        vaultButton.setSelected(false);
        pager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                homeButton.setSelected(position == 0);
                vaultButton.setSelected(position == 1);
            }
        });
        homeButton.setOnClickListener(view -> pager.setCurrentItem(0, false));
        vaultButton.setOnClickListener(view -> pager.setCurrentItem(1, false));
        createButton.setOnClickListener(view -> onCreateCapsule.run());
    }
}