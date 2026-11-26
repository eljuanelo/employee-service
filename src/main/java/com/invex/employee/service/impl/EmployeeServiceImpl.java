package com.invex.employee.service.impl;

import com.invex.employee.dto.EmployeeCreateRequest;
import com.invex.employee.dto.EmployeeResponse;
import com.invex.employee.dto.EmployeeUpdateRequest;
import com.invex.employee.entity.Employee;
import com.invex.employee.exception.EmployeeNotFoundException;
import com.invex.employee.exception.InvalidEmployeeDataException;
import com.invex.employee.mapper.EmployeeMapper;
import com.invex.employee.repository.EmployeeRepository;
import com.invex.employee.service.EmployeeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmployeeServiceImpl implements EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final EmployeeMapper employeeMapper;

    @Override
    @Transactional(readOnly = true)
    public List<EmployeeResponse> findAll() {
        return toResponses(employeeRepository.findAll());
    }

    @Override
    @Transactional(readOnly = true)
    public EmployeeResponse findById(Long id) {
        return employeeMapper.toResponse(getEmployee(id));
    }

    @Override
    @Transactional
    public List<EmployeeResponse> create(List<EmployeeCreateRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            throw new InvalidEmployeeDataException("At least one employee is required.");
        }

        for (EmployeeCreateRequest request : requests) {
            if (request == null) {
                throw new InvalidEmployeeDataException("Employee requests must not contain null elements.");
            }
            validateAge(request.getAge(), request.getBirthDate());
        }

        log.debug("Creating a batch of {} employees", requests.size());
        List<Employee> employees = requests.stream().map(employeeMapper::toEntity).toList();
        return toResponses(employeeRepository.saveAll(employees));
    }

    @Override
    @Transactional
    public EmployeeResponse update(Long id, EmployeeUpdateRequest request) {
        Employee employee = getEmployee(id);
        if (request == null) {
            throw new InvalidEmployeeDataException("An employee update request is required.");
        }

        validateNonBlank("firstName", request.getFirstName());
        validateNonBlank("lastName", request.getLastName());
        validateNonBlank("position", request.getPosition());

        employeeMapper.updateEntity(request, employee);
        if (request.getAge() != null || request.getBirthDate() != null) {
            validateAge(employee.getAge(), employee.getBirthDate());
        }

        log.debug("Updating employee with ID: {}", id);
        return employeeMapper.toResponse(employeeRepository.save(employee));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Employee employee = getEmployee(id);
        log.debug("Deleting employee with ID: {}", id);
        employeeRepository.delete(employee);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EmployeeResponse> searchByName(String name) {
        if (name == null || name.isBlank()) {
            throw new InvalidEmployeeDataException("A name is required for the search.");
        }

        List<Employee> employees = employeeRepository.searchByName(name.trim());
        log.debug("Employee search returned {} results", employees.size());
        return toResponses(employees);
    }

    private Employee getEmployee(Long id) {
        if (id == null) {
            throw new InvalidEmployeeDataException("An employee ID is required.");
        }
        return employeeRepository.findById(id)
                .orElseThrow(() ->
                new EmployeeNotFoundException(
                    "Employee not found with ID: " + id));
    }

    private List<EmployeeResponse> toResponses(List<Employee> employees) {
        return employees.stream().map(employeeMapper::toResponse).toList();
    }

    private void validateNonBlank(String fieldName, String value) {
        if (value != null && value.isBlank()) {
            throw new InvalidEmployeeDataException(fieldName + " must not be blank.");
        }
    }

    private void validateAge(Integer age, LocalDate birthDate) {
        if (age == null || birthDate == null) {
            throw new InvalidEmployeeDataException("Age and birth date are required.");
        }

        LocalDate today = LocalDate.now();
        if (!birthDate.isBefore(today)) {
            throw new InvalidEmployeeDataException("Birth date must be in the past.");
        }

        if (age != Period.between(birthDate, today).getYears()) {
            throw new InvalidEmployeeDataException("Age does not match the birth date.");
        }
    }
}
