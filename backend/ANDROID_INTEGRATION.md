# Android integration guide

Follow these steps in your Android (Java) app after a user signs up through your own backend. The email verification flow is powered entirely by this service and an SMTP server—Firebase is **not** required.

1. **Create the user on your backend and request a verification email.**
   ```java
   private void registerAndSendVerification(String uid, String emailAddress) {
       String endpoint = "https://your-backend-domain.com/auth/send-verification";

       JSONObject payload = new JSONObject();
       try {
           payload.put("uid", uid);             // Unique identifier from your auth system
           payload.put("email", emailAddress);   // Address that should receive the verification email
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

2. **Prompt the user to check their inbox and poll verification status.**
   ```java
   private void refreshVerificationStatus(String uid) {
       String statusUrl = "https://your-backend-domain.com/auth/status/" + uid;

       RequestQueue queue = Volley.newRequestQueue(this);
       JsonObjectRequest request = new JsonObjectRequest(
           Request.Method.GET,
           statusUrl,
           null,
           response -> {
               boolean verified = response.optBoolean("verified", false);
               if (verified) {
                   // Navigate to the verified experience.
               } else {
                   // Stay on the waiting screen and inform the user.
               }
           },
           error -> {
               // Handle errors (e.g. user not found or server issue).
           }
       );

       queue.add(request);
   }
   ```

   You can call `refreshVerificationStatus` on demand (e.g., pull-to-refresh) or schedule it with a `Handler`, `Coroutine`, or `WorkManager` to poll periodically. Avoid aggressive polling—every few minutes is sufficient.

3. **Gate protected features by checking the verification status you retrieved.**
   Store the `verified` boolean for the logged-in user and confirm it before allowing access to loyalty features.

4. **Handle success redirect.**
   - When the user taps the link in the email, the backend will redirect them to `APP_SUCCESS_URL`.
   - Host a lightweight web page at that URL instructing users to return to the app and tap **Refresh**.

Ensure the Android app enforces HTTPS when calling the backend and consider attaching an Authorization header or signed token from your authentication system if you want to restrict access to authenticated clients only.
