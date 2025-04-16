package com.example.iTDS.repositories;

import com.example.iTDS.entities.Project;
import com.example.iTDS.entities.TDS;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TDSRepository extends JpaRepository<TDS, Long> {
    List<TDS> findByProject_ProjectId(Long projectId);

    List<TDS> findByStatus(String s);

    List<TDS> findByStatusStartingWith(String rejected);

    List<TDS> findByCurrentStep(String pmApproval);
    List<TDS> findByRemarksContaining(String remarks);


    List<TDS> findByDocumentPathContaining(String fileName);
//    @Query("SELECT t FROM TDS t WHERE t.currentStep = :currentStep AND t.project.projectId IN :projectIds")
//    List<TDS> findByCurrentStepAndProjectIds(
//            @Param("currentStep") String currentStep,
//            @Param("projectIds") List<Long> projectIds
//    );
//    List<TDS> findByCurrentStepAndProject_ProjectIdIn(String step, List<Long> projectIds);
//
//
//    List<TDS> findByCurrentStepAndProjectIn(String pmApproval, List<Project> pmProjects);

    List<TDS> findByCurrentStepAndProject_ProjectIdIn(String pmApproval, List<Long> projectIds);



        @Query("SELECT DISTINCT t FROM TDS t " +
                "JOIN FETCH t.project p " +
                "JOIN FETCH p.contractors " +
                "JOIN FETCH p.stakeholder " +
                "WHERE t.status = 'Approved by PM' " +
                "AND EXISTS (SELECT 1 FROM p.contractors c WHERE c.username = :username)")
        List<TDS> findPmApprovedTDSForContractor(@Param("username") String username);


        @Query("SELECT DISTINCT t FROM TDS t " +
                "LEFT JOIN FETCH t.project p " +
                "LEFT JOIN FETCH p.contractors " +
                "LEFT JOIN FETCH p.stakeholder " +
                "WHERE t.status = 'Approved by PM'")
        List<TDS> findByStatusWithRelations(String status);



}
