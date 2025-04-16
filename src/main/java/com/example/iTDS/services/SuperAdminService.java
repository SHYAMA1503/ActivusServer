package com.example.iTDS.services;

import com.example.iTDS.dto.UserDTO;
import com.example.iTDS.dto.UsersDto;
import com.example.iTDS.entities.Role;
import com.example.iTDS.entities.User;
import com.example.iTDS.repositories.UserRepository;
import jakarta.annotation.PostConstruct;
import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class SuperAdminService {
    @Autowired
    private UserRepository userRepository;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder(); // Add this

    // Hardcoded Super Admin credentials
    private final String SUPER_ADMIN_USERNAME = "superadmin";
    private final String SUPER_ADMIN_PASSWORD = "admin@123";
    private final String SUPER_ADMIN_EMAIL = "superadmin@yourdomain.com"; // Add this

    @PostConstruct
    public void initSuperAdmin() {
        if (!userRepository.existsByUsername(SUPER_ADMIN_USERNAME)) {
            User superAdmin = new User();
            superAdmin.setUsername(SUPER_ADMIN_USERNAME);
            superAdmin.setPassword(passwordEncoder.encode(SUPER_ADMIN_PASSWORD));
            superAdmin.setRole(Role.SUPER_ADMIN);
            superAdmin.setApproved(true);
            superAdmin.setEmailId(SUPER_ADMIN_EMAIL); // Add this line
            userRepository.save(superAdmin);
        }
    }

    public List<UsersDto> getPendingUsers() {
        return userRepository.findByApprovedFalse().stream()
                .map(user -> new UsersDto(
                        user.getId(),
                        user.getUsername(),
                        user.getEmailId(),
                        user.isApproved(),
                        user.getRole()
                ))
                .collect(Collectors.toList());
    }

    public User approveUser(Long userId, boolean approve) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setApproved(approve);
        return userRepository.save(user);
    }
}