package com.example.loyaltyprogram;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import android.view.MenuItem;

public class LoyaltyActivity extends AppCompatActivity {

    public static final String EXTRA_OPEN_PROFILE =
            "com.example.loyaltyprogram.EXTRA_OPEN_PROFILE";

    private BottomNavigationView bottomNavigationView;
    private FragmentManager fragmentManager;

    // Fragment instances
    private HomeFragment homeFragment;
    private RewardsFragment rewardsFragment;
    private ScanFragment scanFragment;
    private ActivityFragment activityFragment;
    private ProfileFragment profileFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_loyalty);

        boolean openProfile = getIntent().getBooleanExtra(EXTRA_OPEN_PROFILE, false);

        // Initialize fragments
        homeFragment = new HomeFragment();
        rewardsFragment = new RewardsFragment();
        scanFragment = new ScanFragment();
        activityFragment = new ActivityFragment();
        profileFragment = new ProfileFragment();

        fragmentManager = getSupportFragmentManager();

        // Load home fragment by default
        if (savedInstanceState == null) {
            if (openProfile) {
                loadFragment(profileFragment);
            } else {
                loadFragment(homeFragment);
            }
        }

        // Setup bottom navigation
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        if (openProfile) {
            bottomNavigationView.setSelectedItemId(R.id.profileFragment);
        }
        bottomNavigationView.setOnItemSelectedListener(new BottomNavigationView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int itemId = item.getItemId();

                if (itemId == R.id.homeFragment) {
                    loadFragment(homeFragment);
                    return true;
                } else if (itemId == R.id.rewardsFragment) {
                    loadFragment(rewardsFragment);
                    return true;
                } else if (itemId == R.id.scanFragment) {
                    loadFragment(scanFragment);
                    return true;
                } else if (itemId == R.id.navigation_activity) {
                    loadFragment(activityFragment);
                    return true;
                } else if (itemId == R.id.profileFragment) {
                    loadFragment(profileFragment);
                    return true;
                }
                return false;
            }
        });
    }

    private void loadFragment(Fragment fragment) {
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.replace(R.id.nav_host_fragment, fragment);
        transaction.commit();
    }
}