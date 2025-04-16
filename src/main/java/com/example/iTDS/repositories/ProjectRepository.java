package com.example.iTDS.repositories;

import com.example.iTDS.entities.Project;
import com.example.iTDS.entities.Role;
import com.example.iTDS.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProjectRepository extends JpaRepository<Project, Long> {
    List<Project> findByRemarksIn(List<String> remarks);
    @Query(value = "SELECT COUNT(*) > 0 FROM projects p " +
            "JOIN project_role_assignments pra ON p.project_id = pra.project_id " +
            "JOIN users u ON u.user_id = pra.user_id " +
            "WHERE p.project_id = :projectId AND u.username = :username " +
            "AND pra.role_type = 'SME'",
            nativeQuery = true)
    boolean isUserAssignedAsSME(@Param("projectId") Long projectId,
                                @Param("username") String username);
//    @Query("SELECT p.id FROM Project p WHERE p.role = :role AND p.user.id = :userId")
//    List<Long> findProjectIdsByRoleAndUserId(@Param("role") Role role, @Param("userId") Long userId);


    // EXISTS check using same approach
//    @Query("SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END " +
//            "FROM Project p JOIN p.roleAssignments ra " +
//            "WHERE p.projectId = :projectId AND KEY(ra) = :role AND :userId MEMBER OF VALUE(ra)")
//    boolean existsByProjectIdAndRoleAndUserId(
//            @Param("projectId") Long projectId,
//            @Param("role") Role role,
//            @Param("userId") Long userId);

}
