package com.example.loyaltyprogram;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class PointsRepository {
    private static PointsRepository INSTANCE;
    private final SharedPreferences prefs;
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private static final String KEY_POINTS = "points";
    private static final String KEY_BIRTHDAY_BONUS_YEAR = "birthday_bonus_year";
    private static final int BIRTHDAY_BONUS_POINTS = 50;
    private static final String BIRTHDAY_STORAGE_PATTERN = "yyyy-MM-dd";

    private PointsRepository(Context ctx) {
        prefs = ctx.getSharedPreferences("loyalty_prefs", Context.MODE_PRIVATE);
    }

    public static synchronized PointsRepository getInstance(Context ctx) {
        if (INSTANCE == null) INSTANCE = new PointsRepository(ctx.getApplicationContext());
        return INSTANCE;
    }

    public int getPoints() {
        return prefs.getInt(KEY_POINTS, 0);
    }

    public void addPointsAsync(int delta) {
        io.execute(() -> {
            int current = prefs.getInt(KEY_POINTS, 0);
            prefs.edit().putInt(KEY_POINTS, current + delta).apply();
        });
    }

    public boolean applyBirthdayBonusIfEligible(String birthdayIsoDate) {
        if (birthdayIsoDate == null || birthdayIsoDate.isEmpty()) {
            return false;
        }

        SimpleDateFormat storageFormat = new SimpleDateFormat(BIRTHDAY_STORAGE_PATTERN, Locale.US);
        storageFormat.setLenient(false);

        Date birthdayDate;
        try {
            birthdayDate = storageFormat.parse(birthdayIsoDate);
        } catch (ParseException e) {
            return false;
        }

        Calendar today = Calendar.getInstance();
        Calendar birthday = Calendar.getInstance();
        birthday.setTime(birthdayDate);

        if (today.get(Calendar.MONTH) == birthday.get(Calendar.MONTH)
                && today.get(Calendar.DAY_OF_MONTH) == birthday.get(Calendar.DAY_OF_MONTH)) {
            int currentYear = today.get(Calendar.YEAR);
            int lastAwardedYear = prefs.getInt(KEY_BIRTHDAY_BONUS_YEAR, -1);
            if (currentYear != lastAwardedYear) {
                int currentPoints = prefs.getInt(KEY_POINTS, 0);
                prefs.edit()
                        .putInt(KEY_POINTS, currentPoints + BIRTHDAY_BONUS_POINTS)
                        .putInt(KEY_BIRTHDAY_BONUS_YEAR, currentYear)
                        .apply();
                return true;
            }
        }

        return false;
    }

    public void resetBirthdayBonusTracking() {
        prefs.edit().remove(KEY_BIRTHDAY_BONUS_YEAR).apply();
    }
}
