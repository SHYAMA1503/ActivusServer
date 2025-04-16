    package com.example.iTDS.entities;

    import com.fasterxml.jackson.annotation.JsonIgnore;
    import jakarta.persistence.*;

    import java.util.HashSet;
    import java.util.List;
    import java.util.Set;

    import jakarta.persistence.*;
    import lombok.*;

    @AllArgsConstructor
    @NoArgsConstructor
    @Getter
    @Setter
    @Entity
    @Data
    @Table(name = "users") // Table name in your database
    public class User {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

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


