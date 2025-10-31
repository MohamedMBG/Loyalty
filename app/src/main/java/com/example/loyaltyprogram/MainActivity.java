package com.example.loyaltyprogram;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        new Thread(() -> {
            try {
                Thread.sleep(600); // simulate loading time
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            runOnUiThread(() -> {
                // Decide where to go next (sign-in or main)
                boolean isSignedIn = UserPreferences.isUserSignedIn(this);
                Intent next;
                if (isSignedIn) {
                    next = new Intent(this, LoyaltyActivity.class);
                } else {
                    next = new Intent(this, SignInActivity.class);
                }

                next.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(next);
                finish();
            });
        }).start();
    }
}