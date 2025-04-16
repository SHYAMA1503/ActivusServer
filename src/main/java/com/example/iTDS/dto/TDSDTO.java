package com.example.iTDS.dto;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class TDSDTO {
    private String tdsName;
    private String documentPath;
    private String status;
    private Long projectId; // Must be Long to match API requirements
}

