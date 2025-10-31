package com.example.loyaltyprogram;

import android.app.Application;
import android.util.Log;

import com.google.firebase.FirebaseApp;

/**
 * Application class responsible for bootstrapping Firebase before any Activity runs.
 */
public class LoyaltyApplication extends Application {

    private static final String TAG = "LoyaltyApplication";

    @Override
    public void onCreate() {
        super.onCreate();

        try {
            FirebaseApp.initializeApp(this);
        } catch (IllegalStateException e) {
            // Firebase may already be initialised when the process is restarted.
            Log.w(TAG, "Firebase already initialised", e);
        }
    }
}
