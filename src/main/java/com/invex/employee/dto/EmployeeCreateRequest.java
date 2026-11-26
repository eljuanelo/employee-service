package com.invex.employee.dto;

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

    @NotBlank
    @Size(max = 255)
    private String firstName;

    @Size(max = 255)
    private String middleName;

    @NotBlank
    @Size(max = 255)
    private String lastName;

    @Size(max = 255)
    private String secondLastName;

    @NotNull
    @PositiveOrZero
    private Integer age;

    @NotNull
    private Sex sex;

    @NotNull
    @Past
    @JsonFormat(pattern = "dd-MM-yyyy")
    private LocalDate birthDate;

    @NotBlank
    @Size(max = 255)
    private String position;

    private Boolean active;
}
