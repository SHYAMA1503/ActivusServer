package com.example.iTDS.services;

import com.example.iTDS.dto.ProjectWithRolesDTO;
import com.example.iTDS.dto.ProjectWithUsersDTO;
import com.example.iTDS.entities.Project;
import com.example.iTDS.entities.Role;
import com.example.iTDS.entities.User;
import com.example.iTDS.repositories.ProjectRepository;
import com.example.iTDS.repositories.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class ProjectService {

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private UserRepository userRepository;

    public Project createProjectWithRoles(ProjectWithRolesDTO projectDTO, String currentUserRole) {
        if (!currentUserRole.equals("SUPER_ADMIN")) {
            throw new RuntimeException("Only Super Admin can create projects");
        }

        // Validate stakeholder ID
        if (projectDTO.getStakeholderId() == null) {
            throw new RuntimeException("Stakeholder ID must not be null");
        }

        User stakeholder = userRepository.findById(projectDTO.getStakeholderId())
                .orElseThrow(() -> new RuntimeException("Stakeholder not found"));

        if (!stakeholder.getRole().equals(Role.Stakeholder)) {
            throw new RuntimeException("User is not a valid Stakeholder");
        }

        // Validate all role assignments
        if (projectDTO.getRoleAssignments() != null) {
            projectDTO.getRoleAssignments().forEach((role, userIds) -> {
                if (userIds != null) {
                    userIds.forEach(userId -> {
                        if (userId == null) {
                            throw new RuntimeException("User ID must not be null for role " + role);
                        }
                        User user = userRepository.findById(userId)
                                .orElseThrow(() -> new RuntimeException(
                                        String.format("User %d not found for role %s", userId, role)));
                        if (!user.getRole().equals(role)) {
                            throw new RuntimeException(
                                    String.format("User %d is not a %s (actual role: %s)",
                                            userId, role, user.getRole()));
                        }
                    });
                }
            });
        }

        Project project = new Project();
        project.setProjectName(projectDTO.getProjectName());
        project.setProjectDescription(projectDTO.getProjectDescription());
        project.setStakeholder(stakeholder);
        project.setProjectStatus(true);
        project.setRemarks("Project created by Super Admin");
        project.setRoleAssignments(projectDTO.getRoleAssignments());

        return projectRepository.save(project);
    }

    public boolean isUserAssignedToProject(Long userId, Long projectId, Role role) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        List<Long> assignedUserIds = project.getRoleAssignments().get(role);
        return assignedUserIds != null && assignedUserIds.contains(userId);
    }

    public List<ProjectWithUsersDTO> getAllProjectsWithUserDetails() {
        List<Project> projects = projectRepository.findAll();
        List<ProjectWithUsersDTO> result = new ArrayList<>();

        for (Project project : projects) {
            ProjectWithUsersDTO dto = new ProjectWithUsersDTO();
            dto.setProjectId(project.getProjectId());
            dto.setProjectName(project.getProjectName());
            dto.setProjectDescription(project.getProjectDescription());
            dto.setCreatedAt(project.getCreatedAt());
            dto.setStakeholder(project.getStakeholder());

            Map<Role, List<User>> roleUsersMap = new HashMap<>();
            if (project.getRoleAssignments() != null) {
                for (Map.Entry<Role, List<Long>> entry : project.getRoleAssignments().entrySet()) {
                    List<User> users = userRepository.findAllById(entry.getValue());
                    roleUsersMap.put(entry.getKey(), users);
                }
            }
            dto.setRoleUsers(roleUsersMap);

            result.add(dto);
        }

        return result;
    }
    public List<ProjectWithUsersDTO> getProjectsAssignedToUserByUsername(String username, Role role) {
        // First find user by username
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Then proceed with existing logic using the user ID
        List<Project> allProjects = projectRepository.findAll();
        return allProjects.stream()
                .filter(project -> {
                    List<Long> userIds = project.getRoleAssignments().get(role);
                    return userIds != null && userIds.contains(user.getId());
                })
                .map(this::convertToProjectWithUsersDTO)
                .collect(Collectors.toList());
    }

    public List<ProjectWithUsersDTO> getProjectsAssignedToUser(Long userId, Role role) {
        List<Project> allProjects = projectRepository.findAll();
        return allProjects.stream()
                .filter(project -> {
                    List<Long> userIds = project.getRoleAssignments().get(role);
                    return userIds != null && userIds.contains(userId);
                })
                .map(this::convertToProjectWithUsersDTO)
                .collect(Collectors.toList());
    }

    public Project updateProjectRolesByUsername(Long projectId, Map<Role, List<Long>> roleUpdates,
                                                String username, String currentUserRole) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        // Find user by username
        User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Verify L1 is assigned to this project using username
        List<Long> l1UserIds = project.getRoleAssignments().getOrDefault(Role.L1, Collections.emptyList());
        List<User> l1Users = userRepository.findAllById(l1UserIds);

        boolean isAssigned = l1Users.stream()
                .anyMatch(user -> user.getUsername().equals(username));

        if (!isAssigned) {
            throw new RuntimeException("You are not authorized to update this project");
        }

        // Only allow updates to L2 and L3
        Map<Role, List<Long>> updatedRoles = new HashMap<>(project.getRoleAssignments());
        roleUpdates.forEach((role, userIds) -> {
            if (role == Role.L2 || role == Role.L3) {
                // Validate users belong to the correct role
                List<User> users = userRepository.findAllById(userIds);
                if (users.stream().anyMatch(u -> u.getRole() != role)) {
                    throw new RuntimeException("Invalid users for role " + role);
                }
                updatedRoles.put(role, userIds);
            }
        });

        project.setRoleAssignments(updatedRoles);
        return projectRepository.save(project);
    }

    private ProjectWithUsersDTO convertToProjectWithUsersDTO(Project project) {
        ProjectWithUsersDTO dto = new ProjectWithUsersDTO();
        dto.setProjectId(project.getProjectId());
        dto.setProjectName(project.getProjectName());
        dto.setProjectDescription(project.getProjectDescription());
        dto.setCreatedAt(project.getCreatedAt());
        dto.setStakeholder(project.getStakeholder());

        Map<Role, List<User>> roleUsersMap = new HashMap<>();
        if (project.getRoleAssignments() != null) {
            for (Map.Entry<Role, List<Long>> entry : project.getRoleAssignments().entrySet()) {
                List<User> users = userRepository.findAllById(entry.getValue());
                roleUsersMap.put(entry.getKey(), users);
            }
        }
        dto.setRoleUsers(roleUsersMap);

        return dto;
    }

    public boolean isUsernameAssignedToProjectAsSME(Long projectId, String username) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));
        List<Long> smeUserIds = project.getRoleAssignments().getOrDefault(Role.SME, Collections.emptyList());
        return userRepository.findAllById(smeUserIds).stream()
                .anyMatch(user -> user.getUsername().equals(username));
    }

    public boolean isUsernameAssignedToProjectAsPM(Long projectId, String username) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));
        List<Long> pmUserIds = project.getRoleAssignments()
                .getOrDefault(Role.PM, Collections.emptyList());
        return userRepository.findAllById(pmUserIds).stream()
                .anyMatch(user -> user.getUsername().equals(username));
    }
}