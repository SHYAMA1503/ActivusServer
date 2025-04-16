package com.example.iTDS.dto;

import com.example.iTDS.entities.Role;
import lombok.Getter;
import lombok.Setter;
import lombok.*;
import com.example.iTDS.entities.User;


@Data
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class UserDTO {
    private String username;
    private String password;
    private String emailId;
    private Role role;


}