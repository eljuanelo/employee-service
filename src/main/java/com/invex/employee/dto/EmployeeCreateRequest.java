package com.invex.employee.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.invex.employee.enums.Sex;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Past;
import javax.validation.constraints.PositiveOrZero;
import javax.validation.constraints.Size;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
public class EmployeeCreateRequest {

    @NotBlank(message = "must not be blank")
    @Size(max = 255, message = "must not exceed 255 characters")
    private String firstName;

    @Size(max = 255, message = "must not exceed 255 characters")
    private String middleName;

    @NotBlank(message = "must not be blank")
    @Size(max = 255, message = "must not exceed 255 characters")
    private String lastName;

    @Size(max = 255, message = "must not exceed 255 characters")
    private String secondLastName;

    @NotNull(message = "must not be null")
    @PositiveOrZero(message = "must be greater than or equal to zero")
    @Schema(description = "Age in completed years. Must match the birth date.")
    private Integer age;

    @NotNull(message = "must not be null")
    private Sex sex;

    @NotNull(message = "must not be null")
    @Past(message = "must be a past date")
    @Schema(description = "Birth date in dd-MM-yyyy format.", example = "08-06-1994")
    @JsonFormat(pattern = "dd-MM-yyyy")
    private LocalDate birthDate;

    @NotBlank(message = "must not be blank")
    @Size(max = 255, message = "must not exceed 255 characters")
    private String position;

    @Schema(description = "Defaults to true when omitted or null.", defaultValue = "true")
    private Boolean active;
}
