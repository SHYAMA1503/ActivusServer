package com.example.iTDS.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import java.time.LocalDateTime;
import java.util.*;



@Getter
@Setter
@Entity
@Data
@Table(name = "projects", schema = "tds")  // Changed from "users" to "projects"
public class Project {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long projectId;

    @Column(nullable = false)
    private String projectName;

    @Column(nullable = false)
    private String projectDescription;

    @Column(nullable = false)
    private boolean projectStatus = false; // Active (true) or Inactive (false)

    @Column(nullable = true)
    private String remarks; // Stores messages like "Validated by L2", "Activated by PM", etc.

    @ManyToOne
    @JoinColumn(name = "stakeholder_id", nullable = false)
    private User stakeholder; // Maps to the Stakeholder

    @ElementCollection
    @CollectionTable(name = "project_role_assignments",
            joinColumns = @JoinColumn(name = "project_id"),
            schema = "tds")
    @MapKeyEnumerated(EnumType.STRING)
    @Column(name = "user_ids")
    @Fetch(FetchMode.JOIN) // Add this annotation
    private Map<Role, List<Long>> roleAssignments = new HashMap<>();

    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<TDS> tdsList = new ArrayList<>();
    private LocalDateTime createdAt;

    public Project() {

    }

    public Long getProjectId() {
        return projectId;
    }

    public Project(Long projectId, String projectName, String projectDescription, boolean projectStatus, String remarks, User stakeholder, Map<Role, List<Long>> roleAssignments, List<TDS> tdsList, LocalDateTime createdAt, LocalDateTime updatedAt, Set<User> contractors) {
        this.projectId = projectId;
        this.projectName = projectName;
        this.projectDescription = projectDescription;
        this.projectStatus = projectStatus;
        this.remarks = remarks;
        this.stakeholder = stakeholder;
        this.roleAssignments = roleAssignments;
        this.tdsList = tdsList;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.contractors = contractors;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public String getProjectDescription() {
        return projectDescription;
    }

    public void setProjectDescription(String projectDescription) {
        this.projectDescription = projectDescription;
    }

    public boolean isProjectStatus() {
        return projectStatus;
    }

    public void setProjectStatus(boolean projectStatus) {
        this.projectStatus = projectStatus;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    public User getStakeholder() {
        return stakeholder;
    }

    public void setStakeholder(User stakeholder) {
        this.stakeholder = stakeholder;
    }

    public Map<Role, List<Long>> getRoleAssignments() {
        return roleAssignments;
    }

    public void setRoleAssignments(Map<Role, List<Long>> roleAssignments) {
        this.roleAssignments = roleAssignments;
    }

    public List<TDS> getTdsList() {
        return tdsList;
    }

    public void setTdsList(List<TDS> tdsList) {
        this.tdsList = tdsList;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Set<User> getContractors() {
        return contractors;
    }

    public void setContractors(Set<User> contractors) {
        this.contractors = contractors;
    }

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    @ManyToMany
    @JsonIgnore
    @JoinTable(
            name = "project_contractor",
            joinColumns = @JoinColumn(name = "project_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private Set<User> contractors = new HashSet<>();

}