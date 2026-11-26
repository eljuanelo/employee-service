package com.invex.employee.service;

import com.invex.employee.dto.EmployeeCreateRequest;
import com.invex.employee.dto.EmployeeResponse;
import com.invex.employee.dto.EmployeeUpdateRequest;

import java.util.List;

public interface EmployeeService {

    // Lists all employees.
    List<EmployeeResponse> findAll();

    // Retrieves an employee by ID.
    EmployeeResponse findById(Long id);

    // Creates the requested employees as one batch.
    List<EmployeeResponse> create(List<EmployeeCreateRequest> requests);

    // Applies a partial update to an existing employee.
    EmployeeResponse update(Long id, EmployeeUpdateRequest request);

    // Permanently removes an employee by ID.
    void delete(Long id);

    // Finds employees by a partial name match.
    List<EmployeeResponse> searchByName(String name);
}
