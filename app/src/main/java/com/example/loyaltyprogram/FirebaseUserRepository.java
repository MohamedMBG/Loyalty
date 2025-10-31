package com.example.loyaltyprogram;

import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Handles basic Firestore persistence for user profile details.
 */
public class FirebaseUserRepository {

    private static FirebaseUserRepository instance;

    private final FirebaseFirestore firestore;

    private FirebaseUserRepository() {
        firestore = FirebaseFirestore.getInstance();
    }

    public static synchronized FirebaseUserRepository getInstance() {
        if (instance == null) {
            instance = new FirebaseUserRepository();
        }
        return instance;
    }

    public Task<Void> ensureUserDocument(@NonNull String email) {
        final String normalizedEmail = normalizeEmail(email);
        if (TextUtils.isEmpty(normalizedEmail)) {
            return Tasks.forException(new IllegalArgumentException("Email cannot be empty"));
        }

        DocumentReference docRef = firestore.collection("users").document(normalizedEmail);
        return docRef.get().continueWithTask(task -> {
            if (!task.isSuccessful()) {
                Exception exception = task.getException();
                return Tasks.forException(exception != null ? exception :
                        new IllegalStateException("Unable to read user document"));
            }

            DocumentSnapshot snapshot = task.getResult();
            Map<String, Object> data = new HashMap<>();
            data.put("email", email.trim());
            data.put("normalizedEmail", normalizedEmail);
            data.put("updatedAt", FieldValue.serverTimestamp());

            if (snapshot == null || !snapshot.exists()) {
                data.put("createdAt", FieldValue.serverTimestamp());
                data.put("profileComplete", false);
            }

            return docRef.set(data, SetOptions.merge());
        });
    }

    public Task<Void> updateUserProfile(@NonNull String email, String displayName, String birthdayIso) {
        final String normalizedEmail = normalizeEmail(email);
        if (TextUtils.isEmpty(normalizedEmail)) {
            return Tasks.forException(new IllegalArgumentException("Email cannot be empty"));
        }

        DocumentReference docRef = firestore.collection("users").document(normalizedEmail);
        return docRef.get().continueWithTask(task -> {
            if (!task.isSuccessful()) {
                Exception exception = task.getException();
                return Tasks.forException(exception != null ? exception :
                        new IllegalStateException("Unable to read user document"));
            }

            DocumentSnapshot snapshot = task.getResult();
            Map<String, Object> data = new HashMap<>();
            data.put("email", email.trim());
            data.put("normalizedEmail", normalizedEmail);
            data.put("updatedAt", FieldValue.serverTimestamp());
            data.put("profileComplete", true);

            if (!TextUtils.isEmpty(displayName)) {
                data.put("displayName", displayName);
            }

            if (!TextUtils.isEmpty(birthdayIso)) {
                data.put("birthday", birthdayIso);
            }

            if (snapshot == null || !snapshot.exists()) {
                data.put("createdAt", FieldValue.serverTimestamp());
            }

            return docRef.set(data, SetOptions.merge());
        });
    }

    private String normalizeEmail(String email) {
        if (email == null) {
            return "";
        }
        return email.trim().toLowerCase(Locale.US);
    }
}
