package com.example.iTDS.dto;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class LoginDTO {
    private String username;
    private String password;
    private String role;
}
