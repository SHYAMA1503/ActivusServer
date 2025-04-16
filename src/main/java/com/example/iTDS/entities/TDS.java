package com.example.iTDS.entities;

import jakarta.persistence.*;
import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Entity
@Data
@Table(name = "tds")
public class TDS {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long tdsId;

    @Column(nullable = false)
    private String tdsName;

    @Column(nullable = true)
    private String documentPath;

    @Column(nullable = true)
    private String status; // Examples: "Draft", "Approved by PM", "Rejected by BU"

    @Column(nullable = true)
    private boolean approved; // If true, TDS is fully approved; otherwise false

    @Column(nullable = true)
    private String currentStep; // Default step
    // Examples: "PM Approval", "BU Validation", "Fully Approved"

    @Column(nullable = true)
    private String remarks; // Comments/messages regarding the TDS's progress

    @ManyToOne
    @JoinColumn(name = "project_id", nullable = false)
    private Project project; // Link TDS to its associated project
}
