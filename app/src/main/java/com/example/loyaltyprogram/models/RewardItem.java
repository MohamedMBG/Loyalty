package com.example.loyaltyprogram.models;

public class RewardItem {
    private final String title;
    private final String description;
    private final int pointsCost;
    private final int imageRes;
    private final String category;

    public RewardItem(String title, String description, int pointsCost, int imageRes, String category) {
        this.title = title;
        this.description = description;
        this.pointsCost = pointsCost;
        this.imageRes = imageRes;
        this.category = category;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public int getPointsCost() {
        return pointsCost;
    }

    public int getImageRes() {
        return imageRes;
    }

    public String getCategory() {
        return category;
    }
}
