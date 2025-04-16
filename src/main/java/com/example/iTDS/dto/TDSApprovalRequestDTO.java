package com.example.iTDS.dto;

import lombok.Data;

@Data
public class TDSApprovalRequestDTO {
    private boolean isApproved; // true for approval, false for rejection
    private String remarks; // Comments or remarks for the action
}
