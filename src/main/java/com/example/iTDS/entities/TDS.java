package com.example.iTDS.entities;

import jakarta.persistence.*;
import lombok.*;


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

    public TDS(Long tdsId, String tdsName, String documentPath, String status, boolean approved, String currentStep, String remarks, Project project) {
        this.tdsId = tdsId;
        this.tdsName = tdsName;
        this.documentPath = documentPath;
        this.status = status;
        this.approved = approved;
        this.currentStep = currentStep;
        this.remarks = remarks;
        this.project = project;
    }

    public TDS() {

    }

    public Long getTdsId() {
        return tdsId;
    }

    public void setTdsId(Long tdsId) {
        this.tdsId = tdsId;
    }

    public String getTdsName() {
        return tdsName;
    }

    public void setTdsName(String tdsName) {
        this.tdsName = tdsName;
    }

    public String getDocumentPath() {
        return documentPath;
    }

    public void setDocumentPath(String documentPath) {
        this.documentPath = documentPath;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public boolean isApproved() {
        return approved;
    }

    public void setApproved(boolean approved) {
        this.approved = approved;
    }

    public String getCurrentStep() {
        return currentStep;
    }

    public void setCurrentStep(String currentStep) {
        this.currentStep = currentStep;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    public Project getProject() {
        return project;
    }

    public void setProject(Project project) {
        this.project = project;
    }

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
