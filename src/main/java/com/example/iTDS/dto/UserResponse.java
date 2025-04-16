package com.example.iTDS.dto;

import com.example.iTDS.entities.Role;
import lombok.*;


@Data
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
public class UserResponse {
    private Long id;
    private String username;
    private String emailId;
    private Role role;
}