package com.example.loyaltyprogram;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PointsRepository {
    private static PointsRepository INSTANCE;
    private final SharedPreferences prefs;
    private final ExecutorService io = Executors.newSingleThreadExecutor();

    private PointsRepository(Context ctx) {
        prefs = ctx.getSharedPreferences("loyalty_prefs", Context.MODE_PRIVATE);
    }

    public static synchronized PointsRepository getInstance(Context ctx) {
        if (INSTANCE == null) INSTANCE = new PointsRepository(ctx.getApplicationContext());
        return INSTANCE;
    }

    public int getPoints() {
        return prefs.getInt("points", 0);
    }

    public void addPointsAsync(int delta) {
        io.execute(() -> {
            int current = prefs.getInt("points", 0);
            prefs.edit().putInt("points", current + delta).apply();
        });
    }
}
