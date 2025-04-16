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
    private String projectDescription;
    private boolean projectStatus;
    private LocalDateTime createdAt;
    private User stakeholder;
    private Map<Role, List<User>> roleUsers;
}