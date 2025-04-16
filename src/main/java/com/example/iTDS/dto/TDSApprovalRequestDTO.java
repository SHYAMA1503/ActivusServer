package com.example.iTDS.dto;

import lombok.Data;

@Data
public class TDSApprovalRequestDTO {
    private boolean isApproved; // true for approval, false for rejection

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    public boolean isApproved() {
        return isApproved;
    }

    public void setApproved(boolean approved) {
        isApproved = approved;
    }

    private String remarks; // Comments or remarks for the action
}
