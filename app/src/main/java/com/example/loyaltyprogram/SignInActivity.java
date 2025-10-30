package com.example.loyaltyprogram;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.InputType;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import android.view.LayoutInflater;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.concurrent.TimeUnit;

public class SignInActivity extends AppCompatActivity {

    EditText phoneNumberEditText;
    Button guestSignInButton , signInButton;

    private static final String PREFS_NAME = "loyalty_prefs";
    private static final String KEY_USER_NAME = "user_name";

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
        phoneNumberEditText = findViewById(R.id.phoneNumberInput);
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
        if (getStoredUserName().isEmpty()) {
            promptForDisplayName();
        } else {
            goToMain();
        }
    }

    private void enableUi(boolean enabled) {
        signInButton.setEnabled(enabled);
        guestSignInButton.setEnabled(enabled);
        phoneNumberEditText.setEnabled(enabled);
    }

    private void promptForDisplayName() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_name_input, null);
        final TextInputLayout inputLayout = dialogView.findViewById(R.id.inputLayoutName);
        final TextInputEditText inputEditText = dialogView.findViewById(R.id.inputName);

        final AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.profile_edit_name_title)
                .setView(dialogView)
                .setCancelable(false)
                .setPositiveButton(R.string.profile_edit_name_save, null)
                .create();

        dialog.setOnShowListener(d -> {
            Button saveButton = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
            saveButton.setOnClickListener(v -> {
                String enteredName = inputEditText.getText() != null
                        ? inputEditText.getText().toString().trim()
                        : "";
                if (enteredName.isEmpty()) {
                    inputLayout.setError(getString(R.string.profile_edit_name_error));
                } else {
                    inputLayout.setError(null);
                    storeUserName(enteredName);
                    dialog.dismiss();
                    goToMain();
                }
            });
        });

        dialog.show();
    }

    private String getStoredUserName() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        return prefs.getString(KEY_USER_NAME, "");
    }

    private void storeUserName(String name) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        prefs.edit().putString(KEY_USER_NAME, name).apply();
    }

    private void goToMain() {
        Intent i = new Intent(this, LoyaltyActivity.class); // change if your main is elsewhere
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
        finish();
    }
}