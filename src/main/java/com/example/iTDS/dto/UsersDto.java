package com.example.iTDS.dto;

import com.example.iTDS.entities.Role;
import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class UsersDto {
    private Long id;
    private String username;
    private String emailId;
    private boolean approved;
    private Role role;
}
