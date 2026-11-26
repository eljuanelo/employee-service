package com.invex.employee.mapper;

import com.invex.employee.dto.EmployeeCreateRequest;
import com.invex.employee.dto.EmployeeResponse;
import com.invex.employee.dto.EmployeeUpdateRequest;
import com.invex.employee.entity.Employee;
import org.springframework.stereotype.Component;

@Component
public class EmployeeMapper {

    // Maps creation fields and preserves the default active status when omitted.
    public Employee toEntity(EmployeeCreateRequest request) {
        Employee employee = new Employee();
        employee.setFirstName(request.getFirstName());
        employee.setMiddleName(request.getMiddleName());
        employee.setLastName(request.getLastName());
        employee.setSecondLastName(request.getSecondLastName());
        employee.setAge(request.getAge());
        employee.setSex(request.getSex());
        employee.setBirthDate(request.getBirthDate());
        employee.setPosition(request.getPosition());
        if (request.getActive() != null) {
            employee.setActive(request.getActive());
        }
        return employee;
    }

    // Copies employee fields, including generated values, to the response.
    public EmployeeResponse toResponse(Employee employee) {
        EmployeeResponse response = new EmployeeResponse();
        response.setId(employee.getId());
        response.setFirstName(employee.getFirstName());
        response.setMiddleName(employee.getMiddleName());
        response.setLastName(employee.getLastName());
        response.setSecondLastName(employee.getSecondLastName());
        response.setAge(employee.getAge());
        response.setSex(employee.getSex());
        response.setBirthDate(employee.getBirthDate());
        response.setPosition(employee.getPosition());
        response.setCreatedAt(employee.getCreatedAt());
        response.setActive(employee.getActive());
        return response;
    }

    // Applies only non-null fields to the existing entity.
    public void updateEntity(EmployeeUpdateRequest request, Employee employee) {
        if (request.getFirstName() != null) {
            employee.setFirstName(request.getFirstName());
        }
        if (request.getMiddleName() != null) {
            employee.setMiddleName(request.getMiddleName());
        }
        if (request.getLastName() != null) {
            employee.setLastName(request.getLastName());
        }
        if (request.getSecondLastName() != null) {
            employee.setSecondLastName(request.getSecondLastName());
        }
        if (request.getAge() != null) {
            employee.setAge(request.getAge());
        }
        if (request.getSex() != null) {
            employee.setSex(request.getSex());
        }
        if (request.getBirthDate() != null) {
            employee.setBirthDate(request.getBirthDate());
        }
        if (request.getPosition() != null) {
            employee.setPosition(request.getPosition());
        }
        if (request.getActive() != null) {
            employee.setActive(request.getActive());
        }
    }
}
