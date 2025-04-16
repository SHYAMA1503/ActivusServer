package com.example.iTDS.dto;

import com.example.iTDS.entities.Role;
import com.example.iTDS.entities.User;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
public class ProjectWithUsersDTO {
    private Long projectId;
    private String projectName;

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public String getProjectDescription() {
        return projectDescription;
    }

    public void setProjectDescription(String projectDescription) {
        this.projectDescription = projectDescription;
    }

    public boolean isProjectStatus() {
        return projectStatus;
    }

    public void setProjectStatus(boolean projectStatus) {
        this.projectStatus = projectStatus;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public User getStakeholder() {
        return stakeholder;
    }

    public void setStakeholder(User stakeholder) {
        this.stakeholder = stakeholder;
    }

    public Map<Role, List<User>> getRoleUsers() {
        return roleUsers;
    }

    public void setRoleUsers(Map<Role, List<User>> roleUsers) {
        this.roleUsers = roleUsers;
    }

    private String projectDescription;
    private boolean projectStatus;
    private LocalDateTime createdAt;
    private User stakeholder;
    private Map<Role, List<User>> roleUsers;
}