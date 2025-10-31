package com.example.loyaltyprogram.models;

public class UserActivityItem {
    private final String title;
    private final String dateLabel;
    private final int pointsDelta;
    private final boolean positive;
    private final int iconRes;

    public UserActivityItem(String title, String dateLabel, int pointsDelta, boolean positive, int iconRes) {
        this.title = title;
        this.dateLabel = dateLabel;
        this.pointsDelta = pointsDelta;
        this.positive = positive;
        this.iconRes = iconRes;
    }

    public String getTitle() {
        return title;
    }

    public String getDateLabel() {
        return dateLabel;
    }

    public int getPointsDelta() {
        return pointsDelta;
    }

    public boolean isPositive() {
        return positive;
    }

    public int getIconRes() {
        return iconRes;
    }
}
