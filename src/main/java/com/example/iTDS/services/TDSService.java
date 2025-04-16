package com.example.iTDS.services;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.example.iTDS.entities.Project;
import com.example.iTDS.entities.Role;
import com.example.iTDS.entities.TDS;
import com.example.iTDS.entities.User;
import com.example.iTDS.repositories.ProjectRepository;
import com.example.iTDS.repositories.TDSRepository;
import com.example.iTDS.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class TDSService {

    @Autowired
    private TDSRepository tdsRepository;
    private final AmazonS3 amazonS3;

    @Autowired
    private ProjectRepository projectRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ProjectService projectService;
    public TDSService(AmazonS3 amazonS3) {
        this.amazonS3 = amazonS3;
    }
    public TDS findByDocumentPathContaining(String fileName) {
        List<TDS> tdsList = tdsRepository.findByDocumentPathContaining(fileName);
        if (tdsList.isEmpty()) {
            throw new RuntimeException("TDS not found for file: " + fileName);
        }
        if (tdsList.size() > 1) {
            throw new RuntimeException("Multiple TDS records found for file: " + fileName);
        }
        return tdsList.get(0); // Return the first (and only) result
    }
    public boolean isUsernameAssignedToProjectAsSME(Long projectId, String username) {
        return projectRepository.isUserAssignedAsSME(projectId, username);
    }
    // Create TDS
    public TDS createTDS(String tdsName, String documentPath, String status, Long projectId, String currentUserRole) {
        if (!currentUserRole.equals(Role.SME.toString())) {
            throw new RuntimeException("Only SMEs are allowed to create TDS.");
        }

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        TDS tds = new TDS();
        tds.setTdsName(tdsName);
        tds.setDocumentPath(documentPath);
        tds.setStatus(status);
        tds.setCurrentStep("PM Approval");
        tds.setRemarks("TDS created by SME and awaiting PM approval.");
        tds.setApproved(false);
        tds.setProject(project);

        return tdsRepository.save(tds);
    }



    // Get all TDS for a project
    public List<TDS> getTDSByProject(Long projectId, String username) {
        // Validate user exists
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Verify user is assigned to this project in any role
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        boolean isAssigned = project.getRoleAssignments().entrySet().stream()
                .anyMatch(entry -> entry.getValue().contains(user.getId()));

        if (!isAssigned) {
            throw new RuntimeException("User not assigned to this project");
        }

        return tdsRepository.findByProject_ProjectId(projectId);
    }


    public List<TDS> getAllNeedToBeRecheckedTDSBySME(String username) {
        // Validate user
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getRole() != Role.SME) {
            throw new RuntimeException("Only SMEs can fetch TDS needing recheck");
        }

        // Get all rejected TDS
        List<TDS> allRejected = tdsRepository.findByStatusStartingWith("Rejected");

        // Filter TDS from projects the SME is assigned to
        return allRejected.stream()
                .filter(tds -> projectService.isUsernameAssignedToProjectAsSME(
                        tds.getProject().getProjectId(),
                        username
                ))
                .toList();
    }

    public TDS recheckRejectedTDSBySME(Long tdsId, String documentPath, String remarks, String username) {
        // Validate user
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getRole() != Role.SME) {
            throw new RuntimeException("Only SMEs can recheck and resubmit TDS");
        }

        TDS tds = tdsRepository.findById(tdsId)
                .orElseThrow(() -> new RuntimeException("TDS not found"));

        // Verify SME is assigned to the project
        if (!projectService.isUsernameAssignedToProjectAsSME(tds.getProject().getProjectId(), username)) {
            throw new RuntimeException("SME not assigned to this project");
        }

        if (!tds.getStatus().startsWith("Rejected")) {
            throw new RuntimeException("TDS is not in a rejected state. Current status: " + tds.getStatus());
        }

        // Update TDS
        tds.setDocumentPath(documentPath); // comma-separated paths
        tds.setRemarks(remarks);
        tds.setStatus("Draft");
        tds.setCurrentStep("PM Approval");
//    tds.setUpdatedAt(LocalDateTime.now());

        return tdsRepository.save(tds);
    }
// In TDSService.java

    public List<TDS> getTDSForPMApproval(String username) {
        // Fetch the user to validate role
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getRole() != Role.PM) {
            throw new RuntimeException("Only PMs can access this endpoint");
        }

        // Get all projects where the user is assigned as PM
        List<Project> pmProjects = projectRepository.findAll().stream()
                .filter(project -> project.getRoleAssignments()
                        .getOrDefault(Role.PM, Collections.emptyList())
                        .contains(user.getId()))
                .toList();

        if (pmProjects.isEmpty()) {
            return Collections.emptyList();
        }

        // Extract project IDs
        List<Long> projectIds = pmProjects.stream()
                .map(Project::getProjectId)
                .toList();

        // Return TDS in "PM Approval" step for these projects
        return tdsRepository.findByCurrentStepAndProject_ProjectIdIn("PM Approval", projectIds);
    }



    public TDS processPMApproval(Long tdsId, boolean approved, String username) {
        // Fetch user and validate role
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getRole() != Role.PM) {
            throw new RuntimeException("Invalid role for approval");
        }

        TDS tds = tdsRepository.findById(tdsId)
                .orElseThrow(() -> new RuntimeException("TDS not found"));

        // Check if the user is PM for the TDS's project
        if (!isUsernameAssignedToProjectAsPM(tds.getProject().getProjectId(), username)) {
            throw new RuntimeException("PM not assigned to project");
        }

        // State machine logic (unchanged)
        if ("PM Approval".equals(tds.getCurrentStep())) {
            if (approved) {
                tds.setStatus("Validated");
                tds.setCurrentStep("BU Approval");
                tds.setRemarks("Approved by PM on " + LocalDateTime.now());
            } else {
                tds.setStatus("Rejected");
                tds.setCurrentStep("SME Prepares TDS");
                tds.setRemarks("Rejected by PM on " + LocalDateTime.now());
            }
            return tdsRepository.save(tds);
        } else {
            throw new RuntimeException("Invalid workflow state");
        }
    }
    public boolean isUsernameAssignedToProjectAsPM(Long projectId, String username) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        // Get all PM user IDs assigned to this project
        List<Long> pmUserIds = project.getRoleAssignments()
                .getOrDefault(Role.PM, Collections.emptyList());

        // Check if any of these users match the username
        return userRepository.findAllById(pmUserIds).stream()
                .anyMatch(user -> user.getUsername().equals(username));
    }
    public boolean isUsernameAssignedToProjectAsBU(Long projectId, String username) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        // Get all BU user IDs assigned to this project
        List<Long> buUserIds = project.getRoleAssignments()
                .getOrDefault(Role.BU, Collections.emptyList());

        // Check if any of these users match the username
        return userRepository.findAllById(buUserIds).stream()
                .anyMatch(user -> user.getUsername().equals(username));
    }
    public List<TDS> getAllNeedToBeApprovedTDSByBU(String username) {
        // Fetch user and validate role
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getRole() != Role.BU) {
            throw new RuntimeException("Only BUs can access this endpoint");
        }

        // Get all projects where the user is assigned as BU
        List<Project> buProjects = projectRepository.findAll().stream()
                .filter(project -> project.getRoleAssignments()
                        .getOrDefault(Role.BU, Collections.emptyList())
                        .contains(user.getId()))
                .toList();

        if (buProjects.isEmpty()) {
            return Collections.emptyList();
        }

        // Extract project IDs
        List<Long> projectIds = buProjects.stream()
                .map(Project::getProjectId)
                .toList();

        // Return TDS in "BU Approval" step for these projects
        return tdsRepository.findByCurrentStepAndProject_ProjectIdIn("BU Approval", projectIds);
    }
    public TDS approveTDSByBU(Long tdsId, boolean approved, String username) {
        // Fetch user and validate role
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getRole() != Role.BU) {
            throw new RuntimeException("Only BUs can approve TDS");
        }

        TDS tds = tdsRepository.findById(tdsId)
                .orElseThrow(() -> new RuntimeException("TDS not found"));

        // Verify BU is assigned to the TDS's project
        if (!isUsernameAssignedToProjectAsBU(tds.getProject().getProjectId(), username)) {
            throw new RuntimeException("BU not assigned to project");
        }

        // State machine logic
        if ("BU Approval".equals(tds.getCurrentStep())) {
            if (approved) {
                tds.setStatus("Approved by BU");
                tds.setCurrentStep("L1 Validation");
                tds.setRemarks("Approved by BU on " + LocalDateTime.now());
            } else {
                tds.setStatus("Rejected by BU");
                tds.setCurrentStep("SME Prepares TDS");
                tds.setRemarks("Rejected by BU on " + LocalDateTime.now());
            }
            return tdsRepository.save(tds);
        } else {
            throw new RuntimeException("Invalid workflow state");
        }
    }



    public boolean isUsernameAssignedToProjectAsL1(Long projectId, String username) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        // Get all L1 user IDs assigned to this project
        List<Long> l1UserIds = project.getRoleAssignments()
                .getOrDefault(Role.L1, Collections.emptyList());

        // Check if username exists in these assigned users
        return userRepository.findAllById(l1UserIds).stream()
                .anyMatch(user -> user.getUsername().equals(username));
    }
    public List<TDS> getAllNeedToBeApprovedTDSByL1(String username) {
        // Fetch user and validate role
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getRole() != Role.L1) {
            throw new RuntimeException("Only L1 users can access this endpoint");
        }

        // Get projects where user is assigned as L1
        List<Long> projectIds = projectRepository.findAll().stream()
                .filter(project -> project.getRoleAssignments()
                        .getOrDefault(Role.L1, Collections.emptyList())
                        .contains(user.getId()))
                .map(Project::getProjectId)
                .toList();

        if (projectIds.isEmpty()) {
            return Collections.emptyList();
        }

        // Fetch TDS in "L1 Validation" step for these projects
        return tdsRepository.findByCurrentStepAndProject_ProjectIdIn("L1 Validation", projectIds);
    }

    public TDS approveTDSByL1(Long tdsId, boolean approved, String username) {
        // Validate user
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getRole() != Role.L1) {
            throw new RuntimeException("Only L1 can approve TDS");
        }

        TDS tds = tdsRepository.findById(tdsId)
                .orElseThrow(() -> new RuntimeException("TDS not found"));

        // Verify L1 is assigned to the project
        if (!isUsernameAssignedToProjectAsL1(tds.getProject().getProjectId(), username)) {
            throw new RuntimeException("L1 not assigned to this project");
        }

        // State machine logic
        if ("L1 Validation".equals(tds.getCurrentStep())) {
            if (approved) {
                tds.setStatus("Approved by L1");
                tds.setCurrentStep("L2 Validation");
                tds.setRemarks("Approved by L1 on " + LocalDateTime.now());
            } else {
                tds.setStatus("Rejected by L1");
                tds.setCurrentStep("SME Prepares TDS");
                tds.setRemarks("Rejected by L1 on " + LocalDateTime.now());
            }
            return tdsRepository.save(tds);
        } else {
            throw new RuntimeException("TDS must be in 'L1 Validation' step");
        }
    }



    public boolean isUsernameAssignedToProjectAsL2(Long projectId, String username) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        // Get all L2 user IDs assigned to this project
        List<Long> l2UserIds = project.getRoleAssignments()
                .getOrDefault(Role.L2, Collections.emptyList());

        // Check if username exists in these assigned users
        return userRepository.findAllById(l2UserIds).stream()
                .anyMatch(user -> user.getUsername().equals(username));
    }
    public List<TDS> getAllNeedToBeApprovedTDSByL2(String username) {
        // Fetch user and validate role
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getRole() != Role.L2) {
            throw new RuntimeException("Only L2 users can access this endpoint");
        }

        // Get projects where user is assigned as L2
        List<Long> projectIds = projectRepository.findAll().stream()
                .filter(project -> project.getRoleAssignments()
                        .getOrDefault(Role.L2, Collections.emptyList())
                        .contains(user.getId()))
                .map(Project::getProjectId)
                .toList();

        if (projectIds.isEmpty()) {
            return Collections.emptyList();
        }

        // Fetch TDS in "L2 Validation" step for these projects
        return tdsRepository.findByCurrentStepAndProject_ProjectIdIn("L2 Validation", projectIds);
    }

    public TDS approveTDSByL2(Long tdsId, boolean approved, String username) {
        // Validate user
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getRole() != Role.L2) {
            throw new RuntimeException("Only L2 can approve TDS");
        }

        TDS tds = tdsRepository.findById(tdsId)
                .orElseThrow(() -> new RuntimeException("TDS not found"));

        // Verify L2 is assigned to the project
        if (!isUsernameAssignedToProjectAsL2(tds.getProject().getProjectId(), username)) {
            throw new RuntimeException("L2 not assigned to this project");
        }

        // State machine logic
        if ("L2 Validation".equals(tds.getCurrentStep()) && "Approved by L1".equals(tds.getStatus())) {
            if (approved) {
                tds.setStatus("Approved by L2");
                tds.setCurrentStep("L3 Validation");
                tds.setRemarks("Approved by L2 on " + LocalDateTime.now());
            } else {
                tds.setStatus("Rejected by L2");
                tds.setCurrentStep("SME Prepares TDS");
                tds.setRemarks("Rejected by L2 on " + LocalDateTime.now());
            }
            return tdsRepository.save(tds);
        } else {
            throw new RuntimeException(
                    "TDS must be in 'L2 Validation' step with 'Approved by L1' status. " +
                            "Current state: " + tds.getCurrentStep() + "/" + tds.getStatus()
            );
        }
    }





    public boolean isUsernameAssignedToProjectAsL3(Long projectId, String username) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        // Get all L3 user IDs assigned to this project
        List<Long> l3UserIds = project.getRoleAssignments()
                .getOrDefault(Role.L3, Collections.emptyList());

        // Check if username exists in these assigned users
        return userRepository.findAllById(l3UserIds).stream()
                .anyMatch(user -> user.getUsername().equals(username));
    }

    public List<TDS> getAllNeedToBeApprovedTDSByL3(String username) {
        // Fetch user and validate role
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getRole() != Role.L3) {
            throw new RuntimeException("Only L3 users can access this endpoint");
        }

        // Get projects where user is assigned as L3
        List<Long> projectIds = projectRepository.findAll().stream()
                .filter(project -> project.getRoleAssignments()
                        .getOrDefault(Role.L3, Collections.emptyList())
                        .contains(user.getId()))
                .map(Project::getProjectId)
                .toList();

        if (projectIds.isEmpty()) {
            return Collections.emptyList();
        }

        // Fetch TDS in "L3 Validation" step for these projects
        return tdsRepository.findByCurrentStepAndProject_ProjectIdIn("L3 Validation", projectIds);
    }

    public TDS approveTDSByL3(Long tdsId, boolean approved, String username) {
        // Validate user
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getRole() != Role.L3) {
            throw new RuntimeException("Only L3 can approve TDS");
        }

        TDS tds = tdsRepository.findById(tdsId)
                .orElseThrow(() -> new RuntimeException("TDS not found"));

        // Verify L3 is assigned to the project
        if (!isUsernameAssignedToProjectAsL3(tds.getProject().getProjectId(), username)) {
            throw new RuntimeException("L3 not assigned to this project");
        }

        // State machine logic
        if ("L3 Validation".equals(tds.getCurrentStep()) && "Approved by L2".equals(tds.getStatus())) {
            if (approved) {
                tds.setStatus("Approved by L3");
                tds.setCurrentStep("L2 Acceptance");
                tds.setRemarks("Approved by L3 on " + LocalDateTime.now());
            } else {
                tds.setStatus("Rejected by L3");
                tds.setCurrentStep("SME Prepares TDS");
                tds.setRemarks("Rejected by L3 on " + LocalDateTime.now());
            }
            return tdsRepository.save(tds);
        } else {
            throw new RuntimeException(
                    "TDS must be in 'L3 Validation' step with 'Approved by L2' status. " +
                            "Current state: " + tds.getCurrentStep() + "/" + tds.getStatus()
            );
        }
    }

    public List<TDS> getAllNeedToBeApprovedTDSByL2AfterL3(String username) {
        // Fetch user and validate role
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getRole() != Role.L2) {
            throw new RuntimeException("Only L2 users can access this endpoint");
        }

        // Get projects where user is assigned as L2
        List<Long> projectIds = projectRepository.findAll().stream()
                .filter(project -> project.getRoleAssignments()
                        .getOrDefault(Role.L2, Collections.emptyList())
                        .contains(user.getId()))
                .map(Project::getProjectId)
                .toList();

        if (projectIds.isEmpty()) {
            return Collections.emptyList();
        }

        // Fetch TDS in "L2 Acceptance" step for these projects
        return tdsRepository.findByCurrentStepAndProject_ProjectIdIn("L2 Acceptance", projectIds);
    }

    public TDS approveTDSByL2Acceptance(Long tdsId, String username) {
        // Validate user
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getRole() != Role.L2) {
            throw new RuntimeException("Only L2 can perform final approval");
        }

        TDS tds = tdsRepository.findById(tdsId)
                .orElseThrow(() -> new RuntimeException("TDS not found"));

        // Verify L2 is assigned to the project
        if (!isUsernameAssignedToProjectAsL2(tds.getProject().getProjectId(), username)) {
            throw new RuntimeException("L2 not assigned to this project");
        }

        // State machine logic
        if ("L2 Acceptance".equals(tds.getCurrentStep()) && "Approved by L3".equals(tds.getStatus())) {
            tds.setStatus("Approved by L2 (Final)");
            tds.setApproved(true);
            tds.setCurrentStep("Completed");
            tds.setRemarks("Final approval by L2 on " + LocalDateTime.now());
            return tdsRepository.save(tds);
        } else {
            throw new RuntimeException(
                    "TDS must be in 'L2 Acceptance' step with 'Approved by L3' status. " +
                            "Current state: " + tds.getCurrentStep() + "/" + tds.getStatus()
            );
        }
    }

    public boolean isUsernameAssignedToProjectAsContractor(Long projectId, String username) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        List<Long> contractorUserIds = project.getRoleAssignments()
                .getOrDefault(Role.Contractor, Collections.emptyList());

        return userRepository.findAllById(contractorUserIds).stream()
                .anyMatch(user -> user.getUsername().equals(username));
    }
    public List<TDS> getAllApprovedTDS(String username) {
        // Fetch user and validate role
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getRole() != Role.PM && user.getRole() != Role.Contractor) {
            throw new RuntimeException("Only PMs and Contractors can access approved TDS");
        }

        // Get all approved TDS
        List<TDS> allApproved = tdsRepository.findByStatus("Approved by L2 (Final)");

        // Filter based on project assignments
        return allApproved.stream()
                .filter(tds -> {
                    if (user.getRole() == Role.PM) {
                        return isUsernameAssignedToProjectAsPM(tds.getProject().getProjectId(), username);
                    } else {
                        return isUsernameAssignedToProjectAsContractor(tds.getProject().getProjectId(), username);
                    }
                })
                .toList();
    }

    public String uploadApprovedTDSDocumentToS3(Long tdsId, String username) {
        try {
            // Validate user
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            if (user.getRole() != Role.Contractor) {
                throw new RuntimeException("Only Contractors can upload to S3");
            }

            TDS tds = tdsRepository.findById(tdsId)
                    .orElseThrow(() -> new RuntimeException("TDS not found"));

            // Verify contractor is assigned to the project
            if (!isUsernameAssignedToProjectAsContractor(tds.getProject().getProjectId(), username)) {
                throw new RuntimeException("Contractor not assigned to this project");
            }

            // Verify TDS is in correct state
            if (!"Approved by L2 (Final)".equals(tds.getStatus())) {
                throw new RuntimeException("TDS must be in 'Approved by L2 (Final)' status");
            }

            // File handling
            String documentPaths = tds.getDocumentPath();
            if (documentPaths == null || documentPaths.isEmpty()) {
                throw new RuntimeException("No documents found for this TDS");
            }

            // Split multiple file paths
            String[] filesToUpload = documentPaths.split(",");
            List<String> uploadedFiles = new ArrayList<>();
            AmazonS3 s3Client = configureS3Client();

            // Get stakeholder username from project
            String stakeholderUsername = tds.getProject().getStakeholder().getUsername();

            for (String filePath : filesToUpload) {
                File file = new File(filePath.trim());
                if (!file.exists()) {
                    System.out.println("Document not found, skipping: " + filePath);
                    continue;
                }

                // Create S3 key with stakeholder username and original filename
                String fileName = file.getName();
                String s3Key = String.format("approved-tds/%s/%s/%s",
                        stakeholderUsername,
                        username,
                        fileName);

                // Upload to S3
                s3Client.putObject("activus-itds", s3Key, file);
                uploadedFiles.add(s3Key);

                // Delete local file
                if (file.delete()) {
                    System.out.println("Local file deleted: " + filePath);
                } else {
                    System.out.println("Failed to delete local file: " + filePath);
                }
            }

            if (uploadedFiles.isEmpty()) {
                throw new RuntimeException("No documents were successfully uploaded");
            }

            // Update TDS with comma-separated S3 paths
            tds.setDocumentPath(String.join(",", uploadedFiles));
            tds.setStatus("Uploaded to S3");
            tds.setCurrentStep("SME Validation");
            tdsRepository.save(tds);

            return "Files uploaded to S3: " + String.join(", ", uploadedFiles);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Upload failed: " + e.getMessage());
        }
    }

    private AmazonS3 configureS3Client() {
        try {
            // IMPORTANT: Store these credentials securely, not hardcoded
            BasicAWSCredentials awsCreds = new BasicAWSCredentials(
                    "AKIAXZEFIEVLRE2GPXH3",
                    "egfgZraJ9CyPUe8UqEGkkWsB+Cwm8UUut66HA7bT"
            );

            return AmazonS3ClientBuilder.standard()
                    .withRegion("eu-north-1")
                    .withCredentials(new AWSStaticCredentialsProvider(awsCreds))
                    .build();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to configure S3 client: " + e.getMessage());
        }
    }
    public void validateDocumentBySME(Long tdsId, boolean isApproved, String username) {
        // Validate user
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getRole() != Role.SME) {
            throw new RuntimeException("Only SMEs are allowed to validate documents");
        }

        TDS tds = tdsRepository.findById(tdsId)
                .orElseThrow(() -> new RuntimeException("TDS not found"));

        // Verify SME is assigned to the project
        if (!projectService.isUsernameAssignedToProjectAsSME(tds.getProject().getProjectId(), username)) {
            throw new RuntimeException("SME not assigned to this project");
        }

        // Get the full S3 path from TDS (which was stored during upload)
        String s3Key = tds.getDocumentPath(); // This now contains the full path(s)

        // For multiple files, you might want to handle them all or just the first one
        // Here we'll take the first file if there are multiple
        String firstDocumentPath = s3Key.split(",")[0].trim();

        System.out.println("S3 Key: " + firstDocumentPath);

        // Download the document from S3
        File document = downloadDocumentFromS3("activus-itds", firstDocumentPath, "local-path-to-temp-file");

        if (!isApproved) {
            tds.setRemarks("Document rejected by SME. Contractor must re-upload.");
            // You might want to revert the status if rejected
            tds.setStatus("Rejected by SME");
            tds.setCurrentStep("Contractor Re-upload");
        } else {
            tds.setRemarks("Document approved by SME and awaiting PM validation.");
            tds.setCurrentStep("PM Validation");
            tds.setStatus("Approved by SME");
        }

        tdsRepository.save(tds);

        // Clean up the temporary file
        if (document != null && document.exists()) {
            document.delete();
        }
    }


    public void validateDocumentByPM(Long tdsId, boolean isApproved, String username) {
        // Validate user
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getRole() != Role.PM) {
            throw new RuntimeException("Only PMs are allowed to validate documents");
        }

        TDS tds = tdsRepository.findById(tdsId)
                .orElseThrow(() -> new RuntimeException("TDS not found"));

        // Verify PM is assigned to the project
        if (!projectService.isUsernameAssignedToProjectAsPM(tds.getProject().getProjectId(), username)) {
            throw new RuntimeException("PM not assigned to this project");
        }

        // Get the full S3 path from TDS (which was stored during upload)
        String s3Key = tds.getDocumentPath(); // This contains the full path(s)

        // Take the first file if there are multiple (consistent with SME validation)
        String firstDocumentPath = s3Key.split(",")[0].trim();

        System.out.println("S3 Key: " + firstDocumentPath);

        try {
            // Download the document from S3
            File document = downloadDocumentFromS3("activus-itds", firstDocumentPath, "local-path-to-temp-file");

            if (!isApproved) {
                tds.setRemarks("Document rejected by PM and sent back to SME for re-validation.");
                tds.setStatus("Rejected by PM");
                tds.setCurrentStep("SME Validation");
            } else {
                tds.setRemarks("Document approved by PM and sent to Contractor for purchase.");
                tds.setStatus("Approved by PM");
                tds.setCurrentStep("Contractor Approval");
            }

            tdsRepository.save(tds);

            // Clean up the temporary file
            if (document != null && document.exists()) {
                document.delete();
            }
        } catch (RuntimeException e) {
            throw new RuntimeException("Validation by PM failed: " + e.getMessage());
        }
    }

    public File downloadDocumentFromS3(String bucketName, String keyName, String downloadPath) {
        try {
            File file = new File(downloadPath);
            amazonS3.getObject(new GetObjectRequest(bucketName, keyName), file);
            return file;
        } catch (AmazonS3Exception e) {
            if (e.getStatusCode() == 404) {
                throw new RuntimeException("Document not found in S3: " + keyName);
            } else {
                throw new RuntimeException("Failed to download document from S3: " + e.getMessage());
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to download document from S3: " + e.getMessage());
        }
    }
    public List<TDS> getRejectedDocumentsBySME(String username) {
        // Validate user
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getRole() != Role.Contractor) {
            throw new RuntimeException("Only Contractors can access rejected documents");
        }

        // Get all rejected documents
        List<TDS> allRejected = tdsRepository.findByRemarksContaining("Document rejected by SME");

        // Filter documents from projects the contractor is assigned to
        return allRejected.stream()
                .filter(tds -> isUsernameAssignedToProjectAsContractor(
                        tds.getProject().getProjectId(),
                        username
                ))
                .toList();
    }

    public String reuploadDocument(
            Long tdsId,
            MultipartFile file,
            String username,
            boolean keepExisting,
            List<Integer> indicesToRemove) {

        try {
            // Validate user
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            if (user.getRole() != Role.Contractor) {
                throw new RuntimeException("Only Contractors can re-upload documents");
            }

            TDS tds = tdsRepository.findById(tdsId)
                    .orElseThrow(() -> new RuntimeException("TDS not found"));

            // Verify contractor is assigned to the project
            if (!isUsernameAssignedToProjectAsContractor(tds.getProject().getProjectId(), username)) {
                throw new RuntimeException("Contractor not assigned to this project");
            }

            // Get stakeholder username from project
            String stakeholderUsername = tds.getProject().getStakeholder().getUsername();

            // Handle existing documents
            List<String> existingDocuments = new ArrayList<>();
            if (keepExisting && tds.getDocumentPath() != null) {
                String[] docs = tds.getDocumentPath().split(",");
                for (int i = 0; i < docs.length; i++) {
                    if (!indicesToRemove.contains(i)) {
                        existingDocuments.add(docs[i].trim());
                    }
                }
            }

            // Handle new file upload
            String newFileKey = null;
            if (file != null && !file.isEmpty()) {
                String fileName = file.getOriginalFilename();
                newFileKey = String.format("approved-tds/%s/%s/%s",
                        stakeholderUsername,
                        username,
                        fileName);

                // Upload to S3
                amazonS3.putObject("activus-itds", newFileKey, file.getInputStream(), new ObjectMetadata());
            }

            // Combine document paths
            List<String> allDocuments = new ArrayList<>(existingDocuments);
            if (newFileKey != null) {
                allDocuments.add(newFileKey);
            }

            if (allDocuments.isEmpty()) {
                throw new RuntimeException("No documents to submit");
            }

            // Update TDS record
            tds.setDocumentPath(String.join(",", allDocuments));
            tds.setStatus("Re-uploaded by Contractor");
            tds.setRemarks("Document re-uploaded by Contractor and awaiting SME validation.");
            tds.setCurrentStep("SME Validation");
            tdsRepository.save(tds);

            return "Document resubmitted successfully";
        } catch (Exception e) {
            throw new RuntimeException("Re-upload failed: " + e.getMessage());
        }
    }
    public List<TDS> getRejectedDocumentsByPM(String username) {
        // Validate user
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getRole() != Role.SME) {
            throw new RuntimeException("Only SMEs can access PM-rejected documents");
        }

        // Get all PM-rejected documents
        List<TDS> allRejected = tdsRepository.findByRemarksContaining("Document rejected by PM");

        // Filter documents from projects the SME is assigned to
        return allRejected.stream()
                .filter(tds -> projectService.isUsernameAssignedToProjectAsSME(
                        tds.getProject().getProjectId(),
                        username
                ))
                .toList();
    }
//    public String reuploadDocument(Long tdsId, MultipartFile file, String username) {
//        try {
//            // Validate user
//            User user = userRepository.findByUsername(username)
//                    .orElseThrow(() -> new RuntimeException("User not found"));
//
//            if (user.getRole() != Role.Contractor) {
//                throw new RuntimeException("Only Contractors can re-upload documents");
//            }
//
//            TDS tds = tdsRepository.findById(tdsId)
//                    .orElseThrow(() -> new RuntimeException("TDS not found"));
//
//            // Verify contractor is assigned to the project
//            if (!isUsernameAssignedToProjectAsContractor(tds.getProject().getProjectId(), username)) {
//                throw new RuntimeException("Contractor not assigned to this project");
//            }
//
//            // Get stakeholder username from project
//            String stakeholderUsername = tds.getProject().getStakeholder().getUsername();
//
//            // Create S3 key with the same folder structure as upload
//            String fileName = file.getOriginalFilename();
//            String s3Key = String.format("approved-tds/%s/%s/%s",
//                    stakeholderUsername,
//                    username,
//                    fileName);
//
//            // Upload to S3
//            try {
//                amazonS3.putObject("activus-itds", s3Key, file.getInputStream(), new ObjectMetadata());
//            } catch (IOException e) {
//                throw new RuntimeException("Failed to upload file to S3", e);
//            }
//
//            // Update TDS record
//            tds.setDocumentPath(s3Key);
//            tds.setStatus("Re-uploaded by Contractor");
//            tds.setRemarks("Document re-uploaded by Contractor and awaiting SME validation.");
//            tds.setCurrentStep("SME Validation");
//            tdsRepository.save(tds);
//
//            return "Document re-uploaded successfully to S3: " + s3Key;
//        } catch (Exception e) {
//            throw new RuntimeException("Re-upload failed: " + e.getMessage());
//        }
//    }
    public List<TDS> getDocumentsForSMEValidation(String username) {
        // Validate user
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getRole() != Role.SME) {
            throw new RuntimeException("Only SMEs can fetch documents for validation");
        }

        // Get all documents in SME Validation step
        List<TDS> allPendingValidation = tdsRepository.findByCurrentStep("SME Validation");

        // Filter documents from projects the SME is assigned to
        return allPendingValidation.stream()
                .filter(tds -> projectService.isUsernameAssignedToProjectAsSME(
                        tds.getProject().getProjectId(),
                        username
                ))
                .toList();
    }

    public List<TDS> getDocumentsForPMValidation(String username) {
        // Validate user
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getRole() != Role.PM) {
            throw new RuntimeException("Only PMs can fetch documents for validation");
        }

        // Get all documents in PM Validation step
        List<TDS> allPendingValidation = tdsRepository.findByCurrentStep("PM Validation");

        // Filter documents from projects the PM is assigned to
        return allPendingValidation.stream()
                .filter(tds -> projectService.isUsernameAssignedToProjectAsPM(
                        tds.getProject().getProjectId(),
                        username
                ))
                .toList();
    }

    public String finalizePurchaseAndUploadDocuments(Long tdsId, MultipartFile orderConfirmation, MultipartFile lrCopy, String username) {
        try {
            // Validate user
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            if (user.getRole() != Role.Contractor) {
                throw new RuntimeException("Only Contractors can finalize purchase");
            }

            TDS tds = tdsRepository.findById(tdsId)
                    .orElseThrow(() -> new RuntimeException("TDS not found"));

            // Verify contractor is assigned to the project
            if (!isUsernameAssignedToProjectAsContractor(tds.getProject().getProjectId(), username)) {
                throw new RuntimeException("Contractor not assigned to this project");
            }

            // Verify TDS is in correct state
            if (!"Approved by PM".equals(tds.getStatus())) {
                throw new RuntimeException("TDS must be in 'Approved by PM' status");
            }

            // Get stakeholder username from project
            String stakeholderUsername = tds.getProject().getStakeholder().getUsername();

            // Generate timestamps for unique filenames
            String timestamp = String.valueOf(System.currentTimeMillis());

            // Upload Order Confirmation
            String orderConfirmationKey = String.format("purchase-docs/%s/%s/order-confirmation-%d-%s-%s",
                    stakeholderUsername,
                    username,
                    tdsId,
                    timestamp,
                    orderConfirmation.getOriginalFilename());

            // Upload LR Copy
            String lrCopyKey = String.format("purchase-docs/%s/%s/lr-copy-%d-%s-%s",
                    stakeholderUsername,
                    username,
                    tdsId,
                    timestamp,
                    lrCopy.getOriginalFilename());

            // Upload both documents to S3
            AmazonS3 s3Client = configureS3Client();
            try {
                // Upload Order Confirmation
                s3Client.putObject(new PutObjectRequest("activus-itds", orderConfirmationKey,
                        orderConfirmation.getInputStream(), new ObjectMetadata()));

                // Upload LR Copy
                s3Client.putObject(new PutObjectRequest("activus-itds", lrCopyKey,
                        lrCopy.getInputStream(), new ObjectMetadata()));
            } catch (IOException e) {
                throw new RuntimeException("Failed to upload documents to S3", e);
            }

            // Store both document paths in the existing documentPath field as comma-separated
            String combinedPaths = orderConfirmationKey + "," + lrCopyKey;

            // Update TDS
            tds.setDocumentPath(combinedPaths);
            tds.setStatus("Purchase Finalized");
            tds.setCurrentStep("Completed");
            tds.setRemarks("Purchase finalized with order confirmation and LR copy uploaded");
            tdsRepository.save(tds);

            return "Purchase finalized successfully. Documents uploaded to S3";
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Purchase finalization failed: " + e.getMessage());
        }
    }
    @Transactional
    public List<TDS> getPmApprovedTDSForContractor(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getRole() != Role.Contractor) {
            throw new RuntimeException("Only Contractors can fetch PM-approved TDS");
        }

        return tdsRepository.findByCurrentStep("Contractor Approval");
    }

}
