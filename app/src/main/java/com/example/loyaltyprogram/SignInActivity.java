package com.example.loyaltyprogram;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseUser;

public class SignInActivity extends AppCompatActivity {

    EditText emailEditText;
    EditText passwordEditText;
    Button guestSignInButton , signInButton;

    // Firebase
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_sign_in);

        // Views
        emailEditText = findViewById(R.id.EmailInput);
        passwordEditText = findViewById(R.id.PasswordInput);
        guestSignInButton = findViewById(R.id.continueGuest);
        signInButton = findViewById(R.id.continueButton);

        // Firebase
        auth = FirebaseAuth.getInstance();

        // Continue as Guest → go to main (no auth)
        guestSignInButton.setOnClickListener(v -> goToMain());

        // Sign in with email
        signInButton.setOnClickListener(v -> {
            attemptEmailAuthentication();
        });
    }

    private void attemptEmailAuthentication() {
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString();

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailEditText.setError("Enter a valid email");
            emailEditText.requestFocus();
            return;
        }

        if (password.length() < 6) {
            passwordEditText.setError("Password must be at least 6 characters");
            passwordEditText.requestFocus();
            return;
        }

        enableUi(false);

        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        handleSignedInUser();
                    } else {
                        Exception ex = task.getException();
                        if (ex instanceof FirebaseAuthInvalidUserException) {
                            registerNewUser(email, password);
                        } else if (ex instanceof FirebaseAuthInvalidCredentialsException) {
                            enableUi(true);
                            passwordEditText.setError("Incorrect password");
                            passwordEditText.requestFocus();
                        } else {
                            enableUi(true);
                            String msg = (ex != null) ? ex.getMessage() : "Authentication failed.";
                            Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    private void handleSignedInUser() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            enableUi(true);
            Toast.makeText(this, "Authentication failed.", Toast.LENGTH_LONG).show();
            return;
        }

        if (user.isEmailVerified()) {
            enableUi(true);
            Toast.makeText(this, "Signed in!", Toast.LENGTH_SHORT).show();
            handlePostSignIn();
        } else {
            sendVerificationEmail(user, true);
        }
    }

    private void registerNewUser(String email, String password) {
        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = auth.getCurrentUser();
                        if (user != null) {
                            sendVerificationEmail(user, false);
                        } else {
                            enableUi(true);
                            Toast.makeText(this, "Could not create user.", Toast.LENGTH_LONG).show();
                        }
                    } else {
                        enableUi(true);
                        Exception ex = task.getException();
                        String msg = (ex != null) ? ex.getMessage() : "Account creation failed.";
                        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void sendVerificationEmail(FirebaseUser user, boolean existingAccount) {
        user.sendEmailVerification()
                .addOnCompleteListener(this, (Task<Void> task) -> {
                    enableUi(true);
                    if (task.isSuccessful()) {
                        String message = existingAccount
                                ? "Please verify your email. A verification link was sent to " + user.getEmail() + "."
                                : "Account created! Check " + user.getEmail() + " to verify your email before signing in.";
                        auth.signOut();
                        showVerificationDialog(message);
                    } else {
                        Exception ex = task.getException();
                        String msg = (ex != null) ? ex.getMessage() : "Failed to send verification email.";
                        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void showVerificationDialog(String message) {
        new AlertDialog.Builder(this)
                .setTitle("Verify your email")
                .setMessage(message)
                .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void handlePostSignIn() {
        boolean profileIncomplete = !UserPreferences.isProfileComplete(this);
        goToMain(profileIncomplete);
    }

    private void enableUi(boolean enabled) {
        signInButton.setEnabled(enabled);
        guestSignInButton.setEnabled(enabled);
        emailEditText.setEnabled(enabled);
        passwordEditText.setEnabled(enabled);
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