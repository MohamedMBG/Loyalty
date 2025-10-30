package com.example.loyaltyprogram.models;

import com.google.type.Date;

public class QrCode {
    private String codeId;
    private String label;
    private long points;
    private boolean active;
    private boolean singleUsePerUser;
    private int maxScansPerDay;
    private Date createdAt;

    public QrCode(String codeId, String label, long points, boolean active, boolean singleUsePerUser, int maxScansPerDay, Date createdAt) {
        this.codeId = codeId;
        this.label = label;
        this.points = points;
        this.active = active;
        this.singleUsePerUser = singleUsePerUser;
        this.maxScansPerDay = maxScansPerDay;
        this.createdAt = createdAt;
    }

    public QrCode() {
    }

    public String getCodeId() {
        return codeId;
    }

    public void setCodeId(String codeId) {
        this.codeId = codeId;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public long getPoints() {
        return points;
    }

    public void setPoints(long points) {
        this.points = points;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean isSingleUsePerUser() {
        return singleUsePerUser;
    }

    public void setSingleUsePerUser(boolean singleUsePerUser) {
        this.singleUsePerUser = singleUsePerUser;
    }

    public int getMaxScansPerDay() {
        return maxScansPerDay;
    }

    public void setMaxScansPerDay(int maxScansPerDay) {
        this.maxScansPerDay = maxScansPerDay;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }
}
