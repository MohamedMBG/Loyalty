package com.example.loyaltyprogram;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

public class SignInActivity extends AppCompatActivity {

    private static final String TAG = "SignInActivity";

    EditText emailEditText;
    Button guestSignInButton , signInButton;
    private FirebaseUserRepository firebaseUserRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_sign_in);

        // Views
        emailEditText = findViewById(R.id.EmailInput);
        guestSignInButton = findViewById(R.id.continueGuest);
        signInButton = findViewById(R.id.continueButton);
        firebaseUserRepository = FirebaseUserRepository.getInstance();

        if (UserPreferences.isUserSignedIn(this)) {
            handlePostSignIn();
            return;
        }

        // Continue as Guest → go to main (no auth)
        guestSignInButton.setOnClickListener(v -> goToMain());

        // Sign in with email
        signInButton.setOnClickListener(v -> {
            attemptEmailOnlyAuthentication();
        });
    }

    private void attemptEmailOnlyAuthentication() {
        String email = emailEditText.getText().toString().trim();

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailEditText.setError("Enter a valid email");
            emailEditText.requestFocus();
            return;
        }

        enableUi(false);
        hideKeyboard();

        UserPreferences.setUserEmail(this, email);
        UserPreferences.setUserSignedIn(this, true);

        firebaseUserRepository.ensureUserDocument(email)
                .addOnSuccessListener(unused -> Log.d(TAG, "User synced with Firestore"))
                .addOnFailureListener(error -> Log.e(TAG, "Failed to sync user with Firestore", error));

        Toast.makeText(this, "Signed in!", Toast.LENGTH_SHORT).show();
        enableUi(true);
        handlePostSignIn();
    }

    private void handlePostSignIn() {
        boolean profileIncomplete = !UserPreferences.isProfileComplete(this);
        goToMain(profileIncomplete);
    }

    private void enableUi(boolean enabled) {
        signInButton.setEnabled(enabled);
        guestSignInButton.setEnabled(enabled);
        emailEditText.setEnabled(enabled);
    }

    private void hideKeyboard() {
        View focusedView = getCurrentFocus();
        if (focusedView == null) {
            focusedView = emailEditText;
        }
        if (focusedView != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(focusedView.getWindowToken(), 0);
            }
        }
    }

    private void goToMain() {
        goToMain(false);
    }

    private void goToMain(boolean openProfile) {
        Intent i = new Intent(this, LoyaltyActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        if (openProfile) {
            i.putExtra(LoyaltyActivity.EXTRA_OPEN_PROFILE, true);
        }
        startActivity(i);
        finish();
    }
}