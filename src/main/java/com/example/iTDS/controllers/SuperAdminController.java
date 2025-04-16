package com.example.iTDS.controllers;

import com.example.iTDS.dto.ApiResponse;
import com.example.iTDS.dto.ApproveUserRequest;
import com.example.iTDS.dto.UserDTO;
import com.example.iTDS.dto.UsersDto;
import com.example.iTDS.entities.User;
import com.example.iTDS.services.SuperAdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/superadmin")
public class SuperAdminController {
    @Autowired
    private SuperAdminService superAdminService;

    @GetMapping("/pending-users")
    public List<UsersDto> getPendingUsers() {
        return superAdminService.getPendingUsers();
    }

    @PostMapping("/approve-user")
    public ResponseEntity<?> approveUser(@RequestBody ApproveUserRequest request) {
        User user = superAdminService.approveUser(request.getUserId(), request.isApprove());
        String message = request.isApprove() ? "User approved!" : "User rejected!";
        return ResponseEntity.ok(new ApiResponse(200, message, user));
    }
}