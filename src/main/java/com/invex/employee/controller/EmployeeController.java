package com.invex.employee.controller;

import com.invex.employee.config.OpenApiConfig;
import com.invex.employee.dto.EmployeeCreateRequest;
import com.invex.employee.dto.EmployeeResponse;
import com.invex.employee.dto.EmployeeUpdateRequest;
import com.invex.employee.dto.ErrorResponse;
import com.invex.employee.service.EmployeeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

@RestController
@RequestMapping("/employees")
@RequiredArgsConstructor
@Validated
@Tag(name = "Employees", description = "Manage and search employees.")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
@ApiResponse(responseCode = "401", description = "Bearer token is missing, invalid, or expired.", content = @Content)
public class EmployeeController {

    private static final String EMPLOYEE_EXAMPLE = """
            {"firstName":"Alex","lastName":"Morgan","age":32,"sex":"FEMALE",
             "birthDate":"08-06-1994","position":"Engineer","active":true}
            """;

    private final EmployeeService employeeService;

    // Returns the employee list as JSON.
    @Operation(summary = "List employees", description = "Returns all employees or an empty array.")
    @ApiResponse(responseCode = "200", description = "Employee list.",
            content = @Content(mediaType = "application/json",
                    array = @ArraySchema(schema = @Schema(implementation = EmployeeResponse.class))))
    @GetMapping
    public ResponseEntity<List<EmployeeResponse>> findAll() {
        return ResponseEntity.ok(employeeService.findAll());
    }

    // Returns one employee by ID.
    @Operation(summary = "Get an employee", description = "Returns the employee with the specified ID.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Employee found.",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = EmployeeResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid employee ID.",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "Employee not found.",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<EmployeeResponse> findById(@Parameter(description = "Employee ID", example = "42") @PathVariable Long id) {
        return ResponseEntity.ok(employeeService.findById(id));
    }

    // Accepts one or more employees and returns the created records.
    @Operation(summary = "Create employees",
            description = "Accepts one employee or a non-empty array. The batch is saved atomically. "
                    + "Adjust the example age to match the birth date when submitting a request.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true,
                    description = "A single employee object or a non-empty array of employee objects.",
                    content = @Content(mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = EmployeeCreateRequest.class)),
                            examples = {
                                @ExampleObject(name = "Single employee", value = EMPLOYEE_EXAMPLE),
                                @ExampleObject(name = "Employee array", value = "[" + EMPLOYEE_EXAMPLE + "]")
                            })))
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Created employees, always returned as an array.",
                content = @Content(mediaType = "application/json",
                        array = @ArraySchema(schema = @Schema(implementation = EmployeeResponse.class)))),
        @ApiResponse(responseCode = "400", description = "Malformed body, invalid employee data, or age mismatch.",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "415", description = "Unsupported request media type.", content = @Content)
    })
    @PostMapping
    public ResponseEntity<List<EmployeeResponse>> create(
            @RequestBody @NotEmpty(message = "must not be empty")
            List<@NotNull(message = "must not be null") @Valid EmployeeCreateRequest> requests) {
        return ResponseEntity.status(HttpStatus.CREATED).body(employeeService.create(requests));
    }

    // Applies the requested fields and returns the updated employee.
    @Operation(summary = "Update an employee",
            description = "Applies only non-null fields. If age or birth date changes, the resulting values must agree.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Updated employee.",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = EmployeeResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid ID, malformed body, invalid data, or age mismatch.",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "Employee not found.",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "415", description = "Unsupported request media type.", content = @Content)
    })
    @PutMapping("/{id}")
    public ResponseEntity<EmployeeResponse> update(
            @Parameter(description = "Employee ID", example = "42") @PathVariable Long id, @Valid @RequestBody EmployeeUpdateRequest request) {
        return ResponseEntity.ok(employeeService.update(id, request));
    }

    // Deletes the employee and returns an empty response.
    @Operation(summary = "Delete an employee", description = "Permanently removes the employee with the specified ID.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Employee deleted.", content = @Content),
        @ApiResponse(responseCode = "400", description = "Invalid employee ID.",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "Employee not found.",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@Parameter(description = "Employee ID", example = "42") @PathVariable Long id) {
        employeeService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // Returns employees that match the supplied name fragment.
    @Operation(summary = "Search employees by name",
            description = "Matches fragments of first, middle, or last names without regard to case.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Matches or an empty array.",
                content = @Content(mediaType = "application/json",
                        array = @ArraySchema(schema = @Schema(implementation = EmployeeResponse.class)))),
        @ApiResponse(responseCode = "400", description = "Missing or blank name.",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/search")
    public ResponseEntity<List<EmployeeResponse>> searchByName(@Parameter(description = "Name fragment", example = "mor") @RequestParam String name) {
        return ResponseEntity.ok(employeeService.searchByName(name));
    }
}
