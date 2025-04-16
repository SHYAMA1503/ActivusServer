package com.example.iTDS.dto;

import com.example.iTDS.entities.Role;
import lombok.*;


@Data
public class UserResponse {
    private Long id;

    public UserResponse(Long id, String username, String emailId, Role role) {
        this.id = id;
        this.username = username;
        this.emailId = emailId;
        this.role = role;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmailId() {
        return emailId;
    }

    public void setEmailId(String emailId) {
        this.emailId = emailId;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    private String username;
    private String emailId;
    private Role role;
}