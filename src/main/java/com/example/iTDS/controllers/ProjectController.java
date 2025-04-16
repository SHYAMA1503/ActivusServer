package com.example.iTDS.controllers;

import com.example.iTDS.dto.ApiResponse;
import com.example.iTDS.dto.ProjectWithRolesDTO;
import com.example.iTDS.dto.ProjectWithUsersDTO;
import com.example.iTDS.entities.Project;
import com.example.iTDS.entities.Role;
import com.example.iTDS.entities.User;
import com.example.iTDS.repositories.ProjectRepository;
import com.example.iTDS.repositories.UserRepository;
import com.example.iTDS.services.ProjectService;
import com.example.iTDS.utils.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/projects")
@CrossOrigin(origins = { "http://localhost:3000", "https://activustdstest1-kappa.vercel.app/login",
        "https://activustdstest1-shyamyobels-projects.vercel.app/login" })
public class ProjectController {
    private final JwtUtil jwtUtil;

    @Autowired
    private ProjectService projectService;
    @Autowired
    private ProjectRepository projectRepository;
    @Autowired
    private UserRepository userRepository;

    public ProjectController(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/create")
    public ResponseEntity<?> createProjectWithRoles(
            @RequestBody ProjectWithRolesDTO projectDTO,
            @RequestHeader("Authorization") String token) {
        String currentUserRole = jwtUtil.extractRole(token.substring(7));
        Project project = projectService.createProjectWithRoles(projectDTO, currentUserRole);
        return ResponseEntity.ok(new ApiResponse(200, "Project created successfully", project));
    }

    @GetMapping("/all")
    public ResponseEntity<?> getAllProjectsWithUserDetails() {
        List<ProjectWithUsersDTO> projects = projectService.getAllProjectsWithUserDetails();
        return ResponseEntity.ok(new ApiResponse(200, "Projects retrieved successfully", projects));
    }

    @GetMapping("/assigned")
    public ResponseEntity<?> getAssignedProjects(
            @RequestHeader("Authorization") String token) {
        try {
            String username = jwtUtil.extractUsername(token.substring(7));
            String role = jwtUtil.extractRole(token.substring(7));

            if (!role.equals(Role.SME.toString())) {
                return ResponseEntity.ok(new ApiResponse(200, "No projects assigned", Collections.emptyList()));
            }

            // Find user by username to get ID
            Optional<User> user = userRepository.findByUsername(username);
            if (user.isEmpty()) {
                return ResponseEntity.ok(new ApiResponse(200, "User not found", Collections.emptyList()));
            }

            // Get all projects where this user is assigned as SME
            List<Project> projects = projectRepository.findAll().stream()
                    .filter(project -> {
                        List<Long> smeIds = project.getRoleAssignments().get(Role.SME);
                        return smeIds != null && smeIds.contains(user.get().getId());
                    })
                    .collect(Collectors.toList());

            // Convert to DTO
            List<ProjectWithUsersDTO> dtos = projects.stream()
                    .map(project -> {
                        ProjectWithUsersDTO dto = new ProjectWithUsersDTO();
                        dto.setProjectId(project.getProjectId());
                        dto.setProjectName(project.getProjectName());
                        dto.setProjectDescription(project.getProjectDescription());
                        return dto;
                    })
                    .collect(Collectors.toList());

            return ResponseEntity.ok(new ApiResponse(200, "Projects retrieved successfully", dtos));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(500, "Error fetching projects: " + e.getMessage(), null));
        }
    }

    @GetMapping("/assigned-to-user/{username}")
    public ResponseEntity<?> getProjectsAssignedToUser(
            @PathVariable String username,
            @RequestParam Role role) {
        try {
            // Find user by username first
            Optional<User> user = userRepository.findByUsername(username);
            if (user.isEmpty()) {
                return ResponseEntity.ok(new ApiResponse(200, "User not found", Collections.emptyList()));
            }

            List<ProjectWithUsersDTO> projects = projectService.getProjectsAssignedToUser(user.get().getId(), role);
            return ResponseEntity.ok(new ApiResponse(200, "Projects retrieved successfully", projects));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(500, "Error fetching projects: " + e.getMessage(), null));
        }
    }

    // In ProjectController.java
    @PutMapping("/update/{projectId}")
    public ResponseEntity<?> updateProjectRoles(
            @PathVariable Long projectId,
            @RequestBody Map<Role, List<Long>> roleUpdates,
            @RequestHeader("Authorization") String token) {
        try {
            String username = jwtUtil.extractUsername(token.substring(7));
            String currentUserRole = jwtUtil.extractRole(token.substring(7));

            Project updatedProject = projectService.updateProjectRolesByUsername(
                    projectId,
                    roleUpdates,
                    username,
                    currentUserRole
            );
            Map<String, String> response = new HashMap<>();
            response.put("status", "200");
            response.put("message", "Project updated successfully");
            response.put("projectId", updatedProject.getProjectId().toString());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse(400, e.getMessage(), null));
        }
    }
}