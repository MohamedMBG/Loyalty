package com.example.loyaltyprogram.models;

import java.util.Date;

public class User {
    private String uid;
    private String displayName;
    private String email;
    private String phone;
    private long points;
    private String tier;
    private Date lastScanAt;
    private Date createdAt;

    // Required empty constructor for Firestore
    public User() {}

    // Full constructor
    public User(String uid, String displayName, String email, String phone, long points, String tier) {
        this.uid = uid;
        this.displayName = displayName;
        this.email = email;
        this.phone = phone;
        this.points = points;
        this.tier = tier;
    }

    // Getters & Setters
    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public long getPoints() { return points; }
    public void setPoints(long points) { this.points = points; }

    public String getTier() { return tier; }
    public void setTier(String tier) { this.tier = tier; }

    public Date getLastScanAt() { return lastScanAt; }
    public void setLastScanAt(Date lastScanAt) { this.lastScanAt = lastScanAt; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
}