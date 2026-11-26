package com.invex.employee.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.invex.employee.enums.Sex;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.Past;
import javax.validation.constraints.PositiveOrZero;
import javax.validation.constraints.Size;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
public class EmployeeUpdateRequest {

    @Size(max = 255, message = "must not exceed 255 characters")
    private String firstName;

    @Size(max = 255, message = "must not exceed 255 characters")
    private String middleName;

    @Size(max = 255, message = "must not exceed 255 characters")
    private String lastName;

    @Size(max = 255, message = "must not exceed 255 characters")
    private String secondLastName;

    @PositiveOrZero(message = "must be greater than or equal to zero")
    private Integer age;

    private Sex sex;

    @Past(message = "must be a past date")
    @JsonFormat(pattern = "dd-MM-yyyy")
    private LocalDate birthDate;

    @Size(max = 255, message = "must not exceed 255 characters")
    private String position;

    private Boolean active;
}
