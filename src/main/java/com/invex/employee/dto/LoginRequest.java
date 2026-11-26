package com.invex.employee.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.NotBlank;

@Getter
@Setter
@NoArgsConstructor
public class LoginRequest {

    @NotBlank(message = "must not be blank")
    private String username;

    @NotBlank(message = "must not be blank")
    @Schema(format = "password", accessMode = Schema.AccessMode.WRITE_ONLY)
    private String password;
}
