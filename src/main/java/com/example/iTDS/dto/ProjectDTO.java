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
}
