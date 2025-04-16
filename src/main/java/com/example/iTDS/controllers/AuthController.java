package com.example.iTDS.controllers;

import com.example.iTDS.dto.ApiResponse;
import com.example.iTDS.dto.LoginDTO;
import com.example.iTDS.dto.UserDTO;
import com.example.iTDS.dto.UserResponse;
import com.example.iTDS.entities.Role;
import com.example.iTDS.entities.User;
import com.example.iTDS.repositories.UserRepository;
import com.example.iTDS.services.UserService;
import com.example.iTDS.utils.JwtUtil;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;
    private final JwtUtil jwtUtil;

    @Autowired
    private AuthenticationManager authenticationManager;

    public AuthController(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/register")
        public ResponseEntity<?> registerUser(@RequestBody UserDTO userDTO) {
            User user = userService.registerUser(userDTO);
            return ResponseEntity.ok(new ApiResponse(200, "User created successfully", new UserResponse(user.getId(), user.getUsername(), user.getEmailId(), user.getRole())));
        }

        @PostMapping("/login")
        public ResponseEntity<?> loginUser(@RequestBody LoginDTO loginDTO) {
            String token = userService.login(loginDTO.getUsername(), loginDTO.getPassword(), loginDTO.getRole());
            return ResponseEntity.ok(new ApiResponse(200, "User login successfully", token));
        }
    @GetMapping("/all")
    public ResponseEntity<?> getAllUsers() {
        List<UserResponse> users = userService.getAllUsers();
        return ResponseEntity.ok(new ApiResponse(200, "Users retrieved successfully", users));
    }
    @GetMapping("/role")
    public ResponseEntity<?> getAllRoles() {
        return ResponseEntity.ok(Role.values());
    }
    @GetMapping("/approved-users")
    public ResponseEntity<?> getApprovedUsers() {
        List<UserResponse> approvedUsers = userRepository.findByApprovedTrue()
                .stream()
                .map(user -> new UserResponse(
                        user.getId(),
                        user.getUsername(),
                        user.getEmailId(),
                        user.getRole()
                ))
                .collect(Collectors.toList());

        return ResponseEntity.ok(new ApiResponse(200, "Approved users retrieved successfully", approvedUsers));
    }

    @GetMapping("/{role}/users")
    public ResponseEntity<?> getUsersByRole(
            @PathVariable Role role,
            @RequestHeader("Authorization") String token) {

        // Use the injected jwtUtil instance
        String currentUserRole = jwtUtil.extractRole(token.substring(7));

        List<UserResponse> users = userRepository.findByRoleAndApprovedTrue(role)
                .stream()
                .map(user -> new UserResponse(
                        user.getId(),
                        user.getUsername(),
                        user.getEmailId(),
                        user.getRole()
                ))
                .collect(Collectors.toList());

        return ResponseEntity.ok(new ApiResponse(200, "Users for role " + role, users));
    }


}
