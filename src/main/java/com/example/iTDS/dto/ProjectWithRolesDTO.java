package com.example.iTDS.dto;

import com.example.iTDS.entities.Role;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class ProjectWithRolesDTO {
    private String projectName;

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

    public Long getStakeholderId() {
        return stakeholderId;
    }

    public void setStakeholderId(Long stakeholderId) {
        this.stakeholderId = stakeholderId;
    }

    public Map<Role, List<Long>> getRoleAssignments() {
        return roleAssignments;
    }

    public void setRoleAssignments(Map<Role, List<Long>> roleAssignments) {
        this.roleAssignments = roleAssignments;
    }

    private String projectDescription;
    private Long stakeholderId;
    private Map<Role, List<Long>> roleAssignments; // Role -> List of User IDs
}