package com.example.iTDS.dto;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class TDSDTO {
    private String tdsName;

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

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    private String documentPath;
    private String status;
    private Long projectId; // Must be Long to match API requirements
}

