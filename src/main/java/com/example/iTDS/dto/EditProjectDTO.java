package com.example.iTDS.dto;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class EditProjectDTO {
    private String projectName;
    private String projectDescription;
    private String projectStatus;
}

