package com.invex.employee.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.invex.employee.enums.Sex;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class EmployeeResponse {

    @Schema(description = "Automatically generated employee ID.", accessMode = Schema.AccessMode.READ_ONLY)
    private Long id;
    private String firstName;
    private String middleName;
    private String lastName;
    private String secondLastName;
    private Integer age;
    private Sex sex;

    @Schema(description = "Birth date in dd-MM-yyyy format.", example = "08-06-1994")
    @JsonFormat(pattern = "dd-MM-yyyy")
    private LocalDate birthDate;

    private String position;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    @Schema(description = "Automatically generated creation timestamp.", accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime createdAt;

    private Boolean active;
}
