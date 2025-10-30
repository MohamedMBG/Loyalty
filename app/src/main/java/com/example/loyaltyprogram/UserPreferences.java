package com.example.loyaltyprogram;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

/**
 * Simple helper around SharedPreferences for storing profile fields that are
 * required across multiple screens.
 */
public final class UserPreferences {

    private static final String PREFS_NAME = "loyalty_prefs";
    private static final String KEY_USER_NAME = "user_name";
    private static final String KEY_USER_EMAIL = "user_email";
    private static final String KEY_USER_BIRTHDAY = "user_birthday";

    private UserPreferences() {
        // Utility class
    }

    private static SharedPreferences getPrefs(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static String getUserName(Context context) {
        return getPrefs(context).getString(KEY_USER_NAME, "");
    }

    public static void setUserName(Context context, String name) {
        getPrefs(context).edit().putString(KEY_USER_NAME, name).apply();
    }

    public static String getUserEmail(Context context) {
        return getPrefs(context).getString(KEY_USER_EMAIL, "");
    }

    public static void setUserEmail(Context context, String email) {
        getPrefs(context).edit().putString(KEY_USER_EMAIL, email).apply();
    }

    public static String getUserBirthday(Context context) {
        return getPrefs(context).getString(KEY_USER_BIRTHDAY, "");
    }

    public static void setUserBirthday(Context context, String birthdayIsoDate) {
        getPrefs(context).edit().putString(KEY_USER_BIRTHDAY, birthdayIsoDate).apply();
    }

    public static boolean isProfileComplete(Context context) {
        return !TextUtils.isEmpty(getUserName(context))
                && !TextUtils.isEmpty(getUserEmail(context))
                && !TextUtils.isEmpty(getUserBirthday(context));
    }
}
