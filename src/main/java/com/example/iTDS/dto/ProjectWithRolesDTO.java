package com.example.iTDS.dto;

import com.example.iTDS.entities.Role;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class ProjectWithRolesDTO {
    private String projectName;
    private String projectDescription;
    private Long stakeholderId;
    private Map<Role, List<Long>> roleAssignments; // Role -> List of User IDs
}