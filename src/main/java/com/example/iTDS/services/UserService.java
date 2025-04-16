package com.example.iTDS.services;

import com.example.iTDS.dto.UserDTO;
import com.example.iTDS.dto.UserResponse;
import com.example.iTDS.entities.User;
import com.example.iTDS.repositories.UserRepository;
import com.example.iTDS.utils.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;
    private final JwtUtil jwtUtil;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public UserService(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    public User registerUser(UserDTO userDTO) {
        // Check for duplicate username
        if (userRepository.existsByUsername(userDTO.getUsername())) {
            throw new RuntimeException("Username already exists");
        }

        // Check for duplicate email
        if (userRepository.existsByEmailId(userDTO.getEmailId())) {
            throw new RuntimeException("Email already exists");
        }

        // Create and save the user
        User user = new User();
        user.setUsername(userDTO.getUsername());
        user.setPassword(passwordEncoder.encode(userDTO.getPassword())); // Hashing the password
        user.setEmailId(userDTO.getEmailId());
        user.setRole(userDTO.getRole());

        return userRepository.save(user);
    }
    public String login(String username, String password, String role) {
        Optional<User> user = userRepository.findByUsername(username);

        // 1. Check if user exists and password matches
        if (user.isEmpty() || !passwordEncoder.matches(password, user.get().getPassword())) {
            throw new RuntimeException("Invalid username or password!");
        }

        // 2. NEW: Check if user is approved
        if (!user.get().isApproved()) {
            throw new RuntimeException("Account not approved. Please contact Super Admin.");
        }

        // 3. Check role match
        if (!user.get().getRole().toString().equals(role)) {
            throw new RuntimeException("Invalid role! The role provided does not match the user's role.");
        }

        return jwtUtil.generateToken(username, role);
    }
    public List<UserResponse> getAllUsers() {
        // Fetch all users from the repository
        List<User> users = userRepository.findAll();

        // Convert the list of User entities to UserResponse DTOs
        return users.stream()
                .map(user -> new UserResponse(user.getId(), user.getUsername(), user.getEmailId(), user.getRole()))
                .collect(Collectors.toList());
    }

}

