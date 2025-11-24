package com.example.iTDS.controllers;

import com.example.iTDS.dto.ApiResponse;
import com.example.iTDS.dto.TDSApprovalRequestDTO;
import com.example.iTDS.dto.TDSDTO;
import com.example.iTDS.entities.Role;
import com.example.iTDS.entities.TDS;
import com.example.iTDS.repositories.TDSRepository;
import com.example.iTDS.services.ProjectService;
import com.example.iTDS.services.TDSService;
import com.example.iTDS.utils.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/tds")
public class TDSController {
    @Autowired
    private TDSService tdsService;
    private final JwtUtil jwtUtil;

    public TDSController(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }
    @Autowired
    private TDSRepository tdsRepository;
    @Autowired
    private ProjectService projectService;


    @Value("${file.storage.path}")
    private String fileStoragePath;
    private static final Logger log = LoggerFactory.getLogger(TDSController.class);

    @GetMapping("/download/{fileName}")
    public ResponseEntity<Resource> downloadFile(
            @PathVariable String fileName,
            @RequestHeader("Authorization") String token) {
        try {
            // Extract role
            String currentUserRole = jwtUtil.extractRole(token.substring(7));
            if (currentUserRole == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
            }

            // Find TDS entry
            TDS tds = tdsService.findByDocumentPathContaining(fileName);
            if (tds == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
            }

            // Get the correct file path from multiple comma-separated paths
            String[] paths = tds.getDocumentPath().split(",");
            Optional<String> matchedPath = Arrays.stream(paths)
                    .map(String::trim)
                    .filter(path -> path.contains(fileName))
                    .findFirst();

            if (matchedPath.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
            }

            Path filePath = Paths.get(matchedPath.get()).normalize();
            Resource resource = new UrlResource(filePath.toUri());

            if (!resource.exists() || !resource.isReadable()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(null);
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_PDF)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileName + "\"")
                    .body(resource);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @PostMapping("/create")
    public ResponseEntity<?> createTDSWithFiles(
            @RequestPart("tdsDTO") String tdsDTOString,
            @RequestPart("files") MultipartFile[] files,
            @RequestHeader("Authorization") String token,
            @RequestParam String username) {

        try {
            ObjectMapper objectMapper = new ObjectMapper();
            TDSDTO tdsDTO = objectMapper.readValue(tdsDTOString, TDSDTO.class);

            // ✅ Verify username is assigned as SME to this project
            if (!projectService.isUsernameAssignedToProjectAsSME(tdsDTO.getProjectId(), username)) {
                throw new RuntimeException("User is not assigned as SME to this project");
            }

            // ✅ Ensure directory exists
            ensureDirectoryExists(fileStoragePath);

            // ✅ Save multiple files and collect their paths
            List<String> savedPaths = new ArrayList<>();
            for (MultipartFile file : files) {
                String fileName = file.getOriginalFilename();
                String filePath = Paths.get(fileStoragePath, fileName).toString();
                File targetFile = new File(filePath);
                file.transferTo(targetFile);
                savedPaths.add(filePath);
            }

            // ✅ Join paths and set in DTO
            String combinedPaths = String.join(",", savedPaths);
            tdsDTO.setDocumentPath(combinedPaths);

            // ✅ Create TDS
            TDS tds = tdsService.createTDS(
                    tdsDTO.getTdsName(),
                    tdsDTO.getDocumentPath(),
                    tdsDTO.getStatus(),
                    tdsDTO.getProjectId(),
                    jwtUtil.extractRole(token.substring(7))
            );

            return ResponseEntity.ok(new ApiResponse(tds, "TDS created successfully", 200));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(null, "TDS creation failed: " + e.getMessage(), 500));
        }
    }
    private void ensureDirectoryExists(String directoryPath) {
        File folder = new File(directoryPath);
        if (!folder.exists()) {
            boolean created = folder.mkdirs();
            if (!created) {
                throw new RuntimeException("Failed to create directory: " + directoryPath);
            }
        }
    }

    // Get all TDS for a project
    @GetMapping("/project/{projectId}")
    public ResponseEntity<?> getTDSByProject(
            @PathVariable Long projectId,
            @RequestHeader("Authorization") String token,
            @RequestParam String username) {

        try {
            List<TDS> tdsList = tdsService.getTDSByProject(projectId, username);
            return ResponseEntity.ok(
                    new ApiResponse(tdsList, "TDS retrieved successfully", 200)
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ApiResponse(null, "Access denied: " + e.getMessage(), 403));
        }
    }

    @GetMapping("/need-to-recheck")
    public ResponseEntity<?> getAllNeedToBeRecheckedTDSBySME(
            @RequestHeader("Authorization") String token,
            @RequestParam String username) {

        try {
            List<TDS> result = tdsService.getAllNeedToBeRecheckedTDSBySME(username);
            return ResponseEntity.ok(
                    new ApiResponse(result, "TDS needing recheck retrieved successfully", 200)
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ApiResponse(null, "Access denied: " + e.getMessage(), 403));
        }
    }

    @PutMapping("/recheck/{tdsId}")
    public ResponseEntity<?> recheckRejectedTDSBySME(
            @PathVariable Long tdsId,
            @RequestPart(value = "files", required = false) MultipartFile[] files,
            @RequestParam String remarks,
            @RequestHeader("Authorization") String token,
            @RequestParam String username,
            @RequestParam(required = false) String filesToKeep) {  // Add this parameter

        try {
            ensureDirectoryExists(fileStoragePath);

            // Start with files to keep (if any)
            List<String> savedPaths = new ArrayList<>();
            if (filesToKeep != null && !filesToKeep.isEmpty()) {
                savedPaths.addAll(Arrays.asList(filesToKeep.split(",")));
            }

            // Add new files
            if (files != null && files.length > 0) {
                for (MultipartFile file : files) {
                    String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
                    String filePath = Paths.get(fileStoragePath, fileName).toString();
                    file.transferTo(new File(filePath));
                    savedPaths.add(filePath);
                }
            }

            String combinedPaths = String.join(",", savedPaths);
            log.info("Files saved to: {}", combinedPaths);

            TDS updatedTDS = tdsService.recheckRejectedTDSBySME(tdsId, combinedPaths, remarks, username);

            return ResponseEntity.ok(
                    new ApiResponse(updatedTDS, "TDS rechecked successfully", 200)
            );
        } catch (Exception e) {
            log.error("Recheck failed", e);
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ApiResponse(null, "Recheck failed: " + e.getMessage(), 403));
        }
    }
    // In TDSController.java

    @GetMapping("/need-to-approve/pm")
    public ResponseEntity<?> getPMApprovalList(
            @RequestHeader("Authorization") String token,
            @RequestParam String username) {  // Take username from frontend
        try {
            List<TDS> result = tdsService.getTDSForPMApproval(username);
            return ResponseEntity.ok(new ApiResponse(result, "Success", 200));
        } catch (Exception e) {
            return errorResponse(e);
        }
    }


    @PutMapping("/approve/pm/{tdsId}")
    public ResponseEntity<?> processPMApproval(
            @PathVariable Long tdsId,
            @RequestParam boolean approved,
            @RequestHeader("Authorization") String token,
            @RequestParam String username) {  // Take username from frontend
        try {
            TDS updatedTDS = tdsService.processPMApproval(tdsId, approved, username);
            return ResponseEntity.ok(new ApiResponse(updatedTDS, "Action successful", 200));
        } catch (Exception e) {
            return errorResponse(e);
        }
    }


    private ResponseEntity<?> errorResponse(Exception e) {
        return ResponseEntity.status(500)
                .body(new ApiResponse(null, "TDS Error: " + e.getMessage(), 500));
    }
    @GetMapping("/need-to-approve/bu")
    public ResponseEntity<?> getAllNeedToBeApprovedTDSByBU(
            @RequestHeader("Authorization") String token,
            @RequestParam String username) {  // Take username from frontend
        try {
            List<TDS> result = tdsService.getAllNeedToBeApprovedTDSByBU(username);
            return ResponseEntity.ok(new ApiResponse(result, "Success", 200));
        } catch (Exception e) {
            return ResponseEntity.status(403).body(e.getMessage());
        }
    }

    @PutMapping("/approve/bu/{tdsId}")
    public ResponseEntity<?> approveTDSByBU(
            @PathVariable Long tdsId,
            @RequestParam boolean approved,
            @RequestHeader("Authorization") String token,
            @RequestParam String username) {  // Take username from frontend
        try {
            TDS updatedTDS = tdsService.approveTDSByBU(tdsId, approved, username);
            return ResponseEntity.ok(new ApiResponse(updatedTDS, "Action successful", 200));
        } catch (Exception e) {
            return ResponseEntity.status(403).body(e.getMessage());
        }
    }

    @GetMapping("/need-to-approve/l1")
    public ResponseEntity<?> getAllNeedToBeApprovedTDSByL1(
            @RequestHeader("Authorization") String token,
            @RequestParam String username) {  // Get username from frontend

        try {
            List<TDS> result = tdsService.getAllNeedToBeApprovedTDSByL1(username);
            return ResponseEntity.ok(new ApiResponse(result, "Success", 200));
        } catch (Exception e) {
            return ResponseEntity.status(403).body(e.getMessage());
        }
    }

    @PutMapping("/approve/l1/{tdsId}")
    public ResponseEntity<?> approveTDSByL1(
            @PathVariable Long tdsId,
            @RequestParam boolean approved,
            @RequestHeader("Authorization") String token,
            @RequestParam String username) {

        try {
            TDS updatedTDS = tdsService.approveTDSByL1(tdsId, approved, username);
            return ResponseEntity.ok(new ApiResponse(updatedTDS, "Action successful", 200));
        } catch (Exception e) {
            return ResponseEntity.status(403).body(e.getMessage());
        }
    }


    @GetMapping("/need-to-approve/l2")
    public ResponseEntity<?> getAllNeedToBeApprovedTDSByL2(
            @RequestHeader("Authorization") String token,
            @RequestParam String username) {

        try {
            List<TDS> result = tdsService.getAllNeedToBeApprovedTDSByL2(username);
            return ResponseEntity.ok(new ApiResponse(result, "Success", 200));
        } catch (Exception e) {
            return ResponseEntity.status(403).body(e.getMessage());
        }
    }

    @PutMapping("/approve/l2/{tdsId}")
    public ResponseEntity<?> approveTDSByL2(
            @PathVariable Long tdsId,
            @RequestParam boolean approved,
            @RequestHeader("Authorization") String token,
            @RequestParam String username) {

        try {
            TDS updatedTDS = tdsService.approveTDSByL2(tdsId, approved, username);
            return ResponseEntity.ok(new ApiResponse(updatedTDS, "Action successful", 200));
        } catch (Exception e) {
            return ResponseEntity.status(403).body(e.getMessage());
        }
    }

    @GetMapping("/need-to-approve/l3")
    public ResponseEntity<?> getAllNeedToBeApprovedTDSByL3(
            @RequestHeader("Authorization") String token,
            @RequestParam String username) {

        try {
            List<TDS> result = tdsService.getAllNeedToBeApprovedTDSByL3(username);
            return ResponseEntity.ok(new ApiResponse(result, "Success", 200));
        } catch (Exception e) {
            return ResponseEntity.status(403).body(e.getMessage());
        }
    }

    @PutMapping("/approve/l3/{tdsId}")
    public ResponseEntity<?> approveTDSByL3(
            @PathVariable Long tdsId,
            @RequestParam boolean approved,
            @RequestHeader("Authorization") String token,
            @RequestParam String username) {

        try {
            TDS updatedTDS = tdsService.approveTDSByL3(tdsId, approved, username);
            return ResponseEntity.ok(new ApiResponse(updatedTDS, "Action successful", 200));
        } catch (Exception e) {
            return ResponseEntity.status(403).body(e.getMessage());
        }
    }



    @GetMapping("/need-to-approve/l2-after-l3")
    public ResponseEntity<?> getAllNeedToBeApprovedTDSByL2AfterL3(
            @RequestHeader("Authorization") String token,
            @RequestParam String username) {

        try {
            List<TDS> result = tdsService.getAllNeedToBeApprovedTDSByL2AfterL3(username);
            return ResponseEntity.ok(new ApiResponse(result, "Success", 200));
        } catch (Exception e) {
            return ResponseEntity.status(403).body(e.getMessage());
        }
    }

    @PutMapping("/approve/l2-after-l3/{tdsId}")
    public ResponseEntity<?> approveTDSByL2AfterL3(
            @PathVariable Long tdsId,
            @RequestHeader("Authorization") String token,
            @RequestParam String username) {

        try {
            TDS updatedTDS = tdsService.approveTDSByL2Acceptance(tdsId, username);
            return ResponseEntity.ok(new ApiResponse(updatedTDS, "Final approval successful", 200));
        } catch (Exception e) {
            return ResponseEntity.status(403).body(e.getMessage());
        }
    }



    @GetMapping("/approved")
    public ResponseEntity<?> getAllApprovedTDS(
            @RequestHeader("Authorization") String token,
            @RequestParam String username) {  // Get username from frontend

        try {
            List<TDS> result = tdsService.getAllApprovedTDS(username);
            return ResponseEntity.ok(new ApiResponse(result, "Success", 200));
        } catch (Exception e) {
            return ResponseEntity.status(403).body(e.getMessage());
        }
    }






    @PostMapping("/uploadToS3/{tdsId}")
    public ResponseEntity<?> uploadApprovedTDSDocumentToS3(
            @PathVariable Long tdsId,
            @RequestHeader("Authorization") String token,
            @RequestParam String username) {

        try {
            String responseMessage = tdsService.uploadApprovedTDSDocumentToAzure(tdsId, username);
            return ResponseEntity.ok(new ApiResponse(null, responseMessage, 200));
        } catch (Exception e) {
            return ResponseEntity.status(403).body(new ApiResponse(null, e.getMessage(), 403));
        }
    }
    // SME validates the document
    @PostMapping("/validateDocumentBySME/{tdsId}")
    public ResponseEntity<?> validateDocumentBySME(
            @PathVariable Long tdsId,
            @RequestParam boolean isApproved,
            @RequestHeader("Authorization") String token,
            @RequestParam String username) {  // Added username param

        try {
            tdsService.validateDocumentBySME(tdsId, isApproved, username);  // Pass username instead of role
            String message = isApproved ?
                    "Document approved by SME." :
                    "Document rejected by SME. Contractor must re-upload.";
            return ResponseEntity.ok(new ApiResponse(null, message, 200));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(null, "Validation failed: " + e.getMessage(), 500));
        }
    }

    // PM validates the document
    @PostMapping("/validateDocumentByPM/{tdsId}")
    public ResponseEntity<?> validateDocumentByPM(
            @PathVariable Long tdsId,
            @RequestParam boolean isApproved,
            @RequestHeader("Authorization") String token,
            @RequestParam String username) {  // Added username param

        try {
            tdsService.validateDocumentByPM(tdsId, isApproved, username);  // Pass username instead of role
            String message = isApproved ?
                    "Document approved by PM and sent to Contractor for purchase." :
                    "Document rejected by PM and sent back to SME.";
            return ResponseEntity.ok(new ApiResponse(null, message, 200));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(null, "Validation failed: " + e.getMessage(), 500));
        }
    }
    @GetMapping("/rejectedBySME")
    public ResponseEntity<?> getRejectedDocumentsBySME(
            @RequestHeader("Authorization") String token,
            @RequestParam String username) {  // Get username from frontend

        try {
            List<TDS> result = tdsService.getRejectedDocumentsBySME(username);
            return ResponseEntity.ok(
                    new ApiResponse(result, "Rejected documents retrieved successfully", 200)
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(null, "Failed to fetch documents: " + e.getMessage(), 500));
        }
    }
    @GetMapping("/rejectedByPM")
    public ResponseEntity<?> getRejectedDocumentsByPM(
            @RequestHeader("Authorization") String token,
            @RequestParam String username) {

        try {
            List<TDS> result = tdsService.getRejectedDocumentsByPM(username);
            return ResponseEntity.ok(
                    new ApiResponse(result, "PM-rejected documents retrieved successfully", 200)
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(null, "Failed to fetch documents: " + e.getMessage(), 500));
        }
    }
    @PostMapping("/reupload/{tdsId}")
    public ResponseEntity<?> reuploadDocument(
            @PathVariable Long tdsId,
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestHeader("Authorization") String token,
            @RequestParam String username,
            @RequestParam(required = false, defaultValue = "true") boolean keepExisting,
            @RequestParam(required = false) String removeIndices) {

        try {
            List<Integer> indicesToRemove = new ArrayList<>();
            if (removeIndices != null && !removeIndices.isEmpty()) {
                indicesToRemove = Arrays.stream(removeIndices.split(","))
                        .map(Integer::parseInt)
                        .collect(Collectors.toList());
            }

            String responseMessage = tdsService.reuploadDocument(
                    tdsId,
                    file,
                    username,
                    keepExisting,
                    indicesToRemove
            );
            return ResponseEntity.ok(new ApiResponse(null, responseMessage, 200));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(null, "Re-upload failed: " + e.getMessage(), 500));
        }
    }
    @GetMapping("/pendingSMEValidation")
    public ResponseEntity<?> getDocumentsForSMEValidation(
            @RequestHeader("Authorization") String token,
            @RequestParam String username) {

        try {
            List<TDS> documents = tdsService.getDocumentsForSMEValidation(username);
            return ResponseEntity.ok(
                    new ApiResponse(documents, "Documents fetched successfully", 200)
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(null, "Failed to fetch documents: " + e.getMessage(), 500));
        }
    }

    @GetMapping("/pendingPMValidation")
    public ResponseEntity<?> getDocumentsForPMValidation(
            @RequestHeader("Authorization") String token,
            @RequestParam String username) {

        try {
            List<TDS> documents = tdsService.getDocumentsForPMValidation(username);
            return ResponseEntity.ok(
                    new ApiResponse(documents, "Documents pending PM validation fetched successfully", 200)
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(null, "Failed to fetch documents: " + e.getMessage(), 500));
        }
    }


    @PostMapping("/finalizePurchase/{tdsId}")
    public ResponseEntity<?> finalizePurchaseAndUploadDocuments(
            @PathVariable Long tdsId,
            @RequestParam MultipartFile orderConfirmation,
            @RequestParam MultipartFile lrCopy,
            @RequestHeader("Authorization") String token,
            @RequestParam String username) {

        try {
            String responseMessage = tdsService.finalizePurchaseAndUploadDocuments(tdsId, orderConfirmation, lrCopy, username);
            return ResponseEntity.ok(new ApiResponse(null, responseMessage, 200));
        } catch (Exception e) {
            return ResponseEntity.status(403).body(new ApiResponse(null, e.getMessage(), 403));
        }
    }
    @GetMapping("/pmApproved")
    public ResponseEntity<?> getPmApprovedTDSForContractor(
            @RequestHeader("Authorization") String token,
            @RequestParam String username) {

        try {
            List<TDS> documents = tdsService.getPmApprovedTDSForContractor(username);
            return ResponseEntity.ok(
                    new ApiResponse(documents, "PM-approved TDS fetched successfully", 200)
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(null, "Failed to fetch PM-approved TDS: " + e.getMessage(), 500));
        }
    }



}
