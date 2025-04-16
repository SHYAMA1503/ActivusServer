package com.example.iTDS.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import java.time.LocalDateTime;
import java.util.*;

@AllArgsConstructor
@NoArgsConstructor
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