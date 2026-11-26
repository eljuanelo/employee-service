package com.invex.employee.dto;

import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "Partial update. Omitted and null fields retain their current values.")
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
    @Schema(description = "Birth date in dd-MM-yyyy format.", example = "08-06-1994")
    @JsonFormat(pattern = "dd-MM-yyyy")
    private LocalDate birthDate;

    @Size(max = 255, message = "must not exceed 255 characters")
    private String position;

    private Boolean active;
}
