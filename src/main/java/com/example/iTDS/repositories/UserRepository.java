package com.example.iTDS.repositories;

import com.example.iTDS.entities.Role;
import com.example.iTDS.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    boolean existsByUsername(String username);
    boolean existsByEmailId(String emailId);
//    User findByUsername(String username);
    Optional<User> findByUsername(String username);  // Now returns Optional
    // Add these methods to your UserRepository interface
    List<User> findByApprovedFalse();
    List<User> findByRoleAndApprovedTrue(Role role);
//    List<User> findByRole(Role role);
//    List<User> findByApprovedTrueAndRole(Role role);

    List<User> findByApprovedTrue();
}
