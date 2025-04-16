package com.example.iTDS.dto;

import com.example.iTDS.entities.Role;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@Data
public class ProjectDTO {
    private String projectName;
    private String projectDescription;
    private String projectStatus;
    private Long stakeholderId;
    private Map<Role, List<Long>> roleAssignments;

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

    public String getProjectStatus() {
        return projectStatus;
    }

    public void setProjectStatus(String projectStatus) {
        this.projectStatus = projectStatus;
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
}
