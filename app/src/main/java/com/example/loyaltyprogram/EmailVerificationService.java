package com.example.loyaltyprogram;

import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Sends email verification requests to the backend service.
 */
public final class EmailVerificationService {

    private static final String TAG = "EmailVerificationSvc";
    private static final MediaType MEDIA_TYPE_JSON = MediaType.get("application/json; charset=utf-8");

    private static EmailVerificationService instance;

    private final OkHttpClient httpClient;
    private final android.os.Handler mainHandler;

    private EmailVerificationService() {
        httpClient = new OkHttpClient.Builder()
                .callTimeout(20, TimeUnit.SECONDS)
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .writeTimeout(20, TimeUnit.SECONDS)
                .build();
        mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    }

    public static synchronized EmailVerificationService getInstance() {
        if (instance == null) {
            instance = new EmailVerificationService();
        }
        return instance;
    }

    public void sendVerificationEmail(@Nullable String email, @Nullable VerificationCallback callback) {
        if (TextUtils.isEmpty(email)) {
            dispatchFailure(callback, "email-empty", null);
            return;
        }

        String trimmedEmail = email.trim();
        if (TextUtils.isEmpty(trimmedEmail)) {
            dispatchFailure(callback, "email-empty", null);
            return;
        }

        String normalizedEmail = trimmedEmail.toLowerCase(Locale.US);
        String uid = normalizedEmail;

        HttpUrl baseUrl = HttpUrl.parse(BuildConfig.EMAIL_VERIFICATION_BASE_URL);
        if (baseUrl == null) {
            dispatchFailure(callback, "invalid-base-url", new IllegalStateException("Invalid verification base URL"));
            return;
        }

        HttpUrl requestUrl = baseUrl.newBuilder()
                .addPathSegment("auth")
                .addPathSegment("send-verification")
                .build();

        JSONObject payload = new JSONObject();
        try {
            payload.put("uid", uid);
            payload.put("email", trimmedEmail);
        } catch (JSONException e) {
            dispatchFailure(callback, "payload-error", e);
            return;
        }

        RequestBody requestBody = RequestBody.create(payload.toString(), MEDIA_TYPE_JSON);

        Request request = new Request.Builder()
                .url(requestUrl)
                .post(requestBody)
                .header("Content-Type", "application/json")
                .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.w(TAG, "Failed to send verification email", e);
                dispatchFailure(callback, "network-failure", e);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                if (response.isSuccessful()) {
                    dispatchSuccess(callback);
                } else {
                    String message = "HTTP " + response.code();
                    Log.w(TAG, "Verification email request failed: " + message);
                    dispatchFailure(callback, message, null);
                }
                response.close();
            }
        });
    }

    private void dispatchSuccess(@Nullable VerificationCallback callback) {
        if (callback == null) {
            return;
        }
        mainHandler.post(callback::onSuccess);
    }

    private void dispatchFailure(@Nullable VerificationCallback callback, @NonNull String reason, @Nullable Throwable throwable) {
        if (callback == null) {
            return;
        }
        mainHandler.post(() -> callback.onFailure(reason, throwable));
    }

    public interface VerificationCallback {
        @MainThread
        void onSuccess();

        @MainThread
        void onFailure(@NonNull String reason, @Nullable Throwable throwable);
    }
}
