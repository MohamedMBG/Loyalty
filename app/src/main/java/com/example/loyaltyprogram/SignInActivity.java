package com.example.loyaltyprogram;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.InputType;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;

import java.util.concurrent.TimeUnit;

public class SignInActivity extends AppCompatActivity {

    EditText phoneNumberEditText;
    Button guestSignInButton , signInButton;

    // Firebase
    private FirebaseAuth auth;
    private String verificationId;
    private PhoneAuthProvider.ForceResendingToken resendToken;

    // Callbacks for phone auth
    private final PhoneAuthProvider.OnVerificationStateChangedCallbacks callbacks =
            new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

                @Override
                public void onVerificationCompleted(@NonNull PhoneAuthCredential credential) {
                    // Auto-retrieval or instant verification
                    signInWithPhoneAuthCredential(credential);
                }

                @Override
                public void onVerificationFailed(@NonNull FirebaseException e) {
                    enableUi(true);
                    Toast.makeText(SignInActivity.this,
                            "Verification failed: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                }

                @Override
                public void onCodeSent(@NonNull String verifId,
                                       @NonNull PhoneAuthProvider.ForceResendingToken token) {
                    verificationId = verifId;
                    resendToken = token;
                    enableUi(true);
                    showOtpDialog(); // Ask user to type the 6-digit code
                }
            };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_sign_in);

        // Views
        phoneNumberEditText = findViewById(R.id.EmailInput);
        guestSignInButton = findViewById(R.id.continueGuest);
        signInButton = findViewById(R.id.continueButton);

        // Firebase
        auth = FirebaseAuth.getInstance();

        // Continue as Guest → go to main (no auth)
        guestSignInButton.setOnClickListener(v -> goToMain());

        // Send OTP
        signInButton.setOnClickListener(v -> {
            String raw = phoneNumberEditText.getText().toString().trim();
            if (raw.isEmpty()) {
                phoneNumberEditText.setError("Enter phone number");
                return;
            }
            String phone = normalizeToE164(raw);
            if (phone == null) {
                phoneNumberEditText.setError("Use full number, e.g. +2126XXXXXXXX");
                return;
            }
            startPhoneNumberVerification(phone);
        });
    }

    /** Example normalizer: requires phone already in E.164 like +2126xxxxxx.
     *  Adapt this to your UX (e.g., add a country picker). */
    private String normalizeToE164(String input) {
        // Accepts +… format. Very basic check; replace with your own validation.
        if (!input.startsWith("+") || input.length() < 10) return null;
        return input.replaceAll("\\s+", "");
    }

    private void startPhoneNumberVerification(String phoneNumber) {
        enableUi(false);

        PhoneAuthOptions options =
                PhoneAuthOptions.newBuilder(auth)
                        .setPhoneNumber(phoneNumber)
                        .setTimeout(60L, TimeUnit.SECONDS)
                        .setActivity(this)
                        .setCallbacks(callbacks)
                        .build();

        PhoneAuthProvider.verifyPhoneNumber(options);
    }

    private void resendCode(String phoneNumber) {
        if (resendToken == null) {
            Toast.makeText(this, "Wait before resending code.", Toast.LENGTH_SHORT).show();
            return;
        }
        enableUi(false);

        PhoneAuthOptions options =
                PhoneAuthOptions.newBuilder(auth)
                        .setPhoneNumber(phoneNumber)
                        .setTimeout(60L, TimeUnit.SECONDS)
                        .setActivity(this)
                        .setCallbacks(callbacks)
                        .setForceResendingToken(resendToken)
                        .build();

        PhoneAuthProvider.verifyPhoneNumber(options);
    }

    private void showOtpDialog() {
        // Simple alert dialog with a single 6-digit input
        final EditText input = new EditText(this);
        input.setHint("Enter 6-digit code");
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setFilters(new InputFilter[]{new InputFilter.LengthFilter(6)});
        input.setPadding(32, 32, 32, 16);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Verify code")
                .setView(input)
                .setCancelable(false)
                .setPositiveButton("Verify", (d, which) -> {
                    String code = input.getText().toString().trim();
                    if (code.length() < 6) {
                        Toast.makeText(this, "Invalid code.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    verifyPhoneNumberWithCode(code);
                })
                .setNegativeButton("Cancel", (d, which) -> enableUi(true))
                .setNeutralButton("Resend", (d, which) -> {
                    String phone = normalizeToE164(phoneNumberEditText.getText().toString().trim());
                    if (phone != null) {
                        resendCode(phone);
                    }
                })
                .create();

        dialog.show();
    }

    private void verifyPhoneNumberWithCode(String code) {
        if (verificationId == null) {
            Toast.makeText(this, "No verification in progress.", Toast.LENGTH_SHORT).show();
            enableUi(true);
            return;
        }
        enableUi(false);
        PhoneAuthCredential credential = PhoneAuthProvider.getCredential(verificationId, code);
        signInWithPhoneAuthCredential(credential);
    }

    private void signInWithPhoneAuthCredential(PhoneAuthCredential credential) {
        auth.signInWithCredential(credential)
                .addOnCompleteListener(this, (Task<AuthResult> task) -> {
                    enableUi(true);
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "Signed in!", Toast.LENGTH_SHORT).show();
                        handlePostSignIn();
                    } else {
                        String msg = (task.getException() != null)
                                ? task.getException().getMessage()
                                : "Verification failed.";
                        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void handlePostSignIn() {
        boolean profileIncomplete = !UserPreferences.isProfileComplete(this);
        goToMain(profileIncomplete);
    }

    private void enableUi(boolean enabled) {
        signInButton.setEnabled(enabled);
        guestSignInButton.setEnabled(enabled);
        phoneNumberEditText.setEnabled(enabled);
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