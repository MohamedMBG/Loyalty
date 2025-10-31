# Android integration guide

Follow these steps in your Android (Java) app after a user signs up with Firebase Authentication using an email address.

1. **Create the Firebase user via email/password sign-up.**
   ```java
   FirebaseAuth auth = FirebaseAuth.getInstance();
   auth.createUserWithEmailAndPassword(email, password)
       .addOnCompleteListener(task -> {
           if (task.isSuccessful()) {
               FirebaseUser firebaseUser = auth.getCurrentUser();
               if (firebaseUser != null) {
                   sendCustomVerification(firebaseUser);
               }
           } else {
               // Handle errors (e.g. email already in use).
           }
       });
   ```

2. **Call the backend to send the verification email.**
   ```java
   private void sendCustomVerification(FirebaseUser firebaseUser) {
       String endpoint = "https://your-backend-domain.com/auth/send-verification";

       JSONObject payload = new JSONObject();
       try {
           payload.put("uid", firebaseUser.getUid());
           payload.put("email", firebaseUser.getEmail());
       } catch (JSONException e) {
           // Handle JSON error.
           return;
       }

       RequestQueue queue = Volley.newRequestQueue(this);
       JsonObjectRequest request = new JsonObjectRequest(
           Request.Method.POST,
           endpoint,
           payload,
           response -> {
               // Optionally show UI feedback (e.g. Snackbar).
           },
           error -> {
               // Inform the user the verification email could not be sent.
           }
       );

       // (Optional) Add headers if you protect the endpoint with auth.
       queue.add(request);
   }
   ```

3. **Prompt the user to check their inbox and refresh verification status.**
   ```java
   Button refreshButton = findViewById(R.id.refreshButton);
   refreshButton.setOnClickListener(v -> {
       FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
       if (user != null) {
           user.reload()
               .addOnCompleteListener(reloadTask -> {
                   FirebaseUser reloadedUser = FirebaseAuth.getInstance().getCurrentUser();
                   if (reloadedUser != null && reloadedUser.isEmailVerified()) {
                       // Navigate to the verified experience.
                   } else {
                       // Stay on the waiting screen and inform the user.
                   }
               });
       }
   });
   ```

4. **Gate protected features by checking `FirebaseUser.isEmailVerified()`.**
   ```java
   FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
   if (currentUser != null && currentUser.isEmailVerified()) {
       // Allow access to loyalty rewards features.
   } else {
       // Redirect to verification prompt screen.
   }
   ```

5. **Handle success redirect.**
   - When the user taps the link in the email, the backend will redirect them to `APP_SUCCESS_URL`.
   - You can host a lightweight web page at that URL instructing users to return to the app and tap **Refresh**.

6. **Optional: Poll for verification.**
   - Use `Handler`/`Coroutine`/`WorkManager` to periodically call `firebaseUser.reload()` until verified, or prompt manual refresh to avoid aggressive polling.

Ensure the Android app enforces HTTPS when calling the backend and consider attaching an Authorization header (e.g., Firebase ID token) if you want to restrict access to authenticated clients only.
