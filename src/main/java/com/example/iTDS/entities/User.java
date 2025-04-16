    package com.example.iTDS.entities;

    import com.fasterxml.jackson.annotation.JsonIgnore;
    import jakarta.persistence.*;

    import java.util.HashSet;
    import java.util.List;
    import java.util.Set;

    import jakarta.persistence.*;
    import lombok.*;



    @Getter
    @Setter
    @Entity
    @Data
    @Table(name = "users") // Table name in your database
    public class User {
        public User(Long id, String username, String password, String emailId, boolean approved, Role role, Set<Project> projects) {
            this.id = id;
            this.username = username;
            this.password = password;
            this.emailId = emailId;
            this.approved = approved;
            this.role = role;
            this.projects = projects;
        }

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        public User() {

        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getEmailId() {
            return emailId;
        }

        public void setEmailId(String emailId) {
            this.emailId = emailId;
        }

        public boolean isApproved() {
            return approved;
        }

        public void setApproved(boolean approved) {
            this.approved = approved;
        }

        public Role getRole() {
            return role;
        }

        public void setRole(Role role) {
            this.role = role;
        }

        public Set<Project> getProjects() {
            return projects;
        }

        public void setProjects(Set<Project> projects) {
            this.projects = projects;
        }

        @Column(nullable = false, unique = true)
        private String username;

        @Column(nullable = false)
        private String password;

        @Column(nullable = false, unique = true)
        private String emailId;
        @Column(nullable = false, columnDefinition = "boolean default false")
        private boolean approved = false;

        @Enumerated(EnumType.STRING)
        @Column(nullable = false)
        private Role role; // Enum to define roles (PM, SME, Stakeholder, etc.)
        @JsonIgnore
        @ManyToMany(mappedBy = "contractors")
        private Set<Project> projects = new HashSet<>();
    }


