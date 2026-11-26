package com.invex.employee.service;

import com.invex.employee.dto.EmployeeCreateRequest;
import com.invex.employee.dto.EmployeeResponse;
import com.invex.employee.dto.EmployeeUpdateRequest;

import java.util.List;

public interface EmployeeService {

    List<EmployeeResponse> findAll();

    EmployeeResponse findById(Long id);

    List<EmployeeResponse> create(List<EmployeeCreateRequest> requests);

    EmployeeResponse update(Long id, EmployeeUpdateRequest request);

    void delete(Long id);

    List<EmployeeResponse> searchByName(String name);
}
