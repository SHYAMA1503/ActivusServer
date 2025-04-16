package com.example.iTDS.dto;

// Add this new class to your dtos package
public class ApproveUserRequest {
    private Long userId;
    private boolean approve;

    // Getters and setters
    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public boolean isApprove() {
        return approve;
    }

    public void setApprove(boolean approve) {
        this.approve = approve;
    }
}