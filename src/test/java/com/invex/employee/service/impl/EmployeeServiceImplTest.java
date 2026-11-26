package com.invex.employee.service.impl;

import com.invex.employee.dto.EmployeeCreateRequest;
import com.invex.employee.dto.EmployeeResponse;
import com.invex.employee.dto.EmployeeUpdateRequest;
import com.invex.employee.entity.Employee;
import com.invex.employee.exception.EmployeeNotFoundException;
import com.invex.employee.exception.InvalidEmployeeDataException;
import com.invex.employee.mapper.EmployeeMapper;
import com.invex.employee.repository.EmployeeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceImplTest {

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private EmployeeMapper employeeMapper;

    @InjectMocks
    private EmployeeServiceImpl employeeService;

    @Test
    void findAllReturnsMappedEmployees() {
        Employee first = new Employee();
        Employee second = new Employee();
        EmployeeResponse firstResponse = new EmployeeResponse();
        EmployeeResponse secondResponse = new EmployeeResponse();
        when(employeeRepository.findAll()).thenReturn(List.of(first, second));
        when(employeeMapper.toResponse(first)).thenReturn(firstResponse);
        when(employeeMapper.toResponse(second)).thenReturn(secondResponse);

        List<EmployeeResponse> result = employeeService.findAll();

        assertEquals(List.of(firstResponse, secondResponse), result);
    }

    @Test
    void findAllReturnsEmptyListWhenNoEmployeesExist() {
        when(employeeRepository.findAll()).thenReturn(List.of());

        List<EmployeeResponse> result = employeeService.findAll();

        assertTrue(result.isEmpty());
        verifyNoInteractions(employeeMapper);
    }

    @Test
    void findByIdReturnsMappedEmployee() {
        Employee employee = new Employee();
        EmployeeResponse response = new EmployeeResponse();
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(employeeMapper.toResponse(employee)).thenReturn(response);

        EmployeeResponse result = employeeService.findById(1L);

        assertSame(response, result);
    }

    @Test
    void findByIdRejectsMissingEmployee() {
        when(employeeRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(EmployeeNotFoundException.class, () -> employeeService.findById(1L));

        verifyNoInteractions(employeeMapper);
    }

    @Test
    void createSavesAndReturnsOneEmployee() {
        EmployeeCreateRequest request = createRequest(30);
        Employee employee = new Employee();
        Employee savedEmployee = new Employee();
        EmployeeResponse response = new EmployeeResponse();
        when(employeeMapper.toEntity(request)).thenReturn(employee);
        when(employeeRepository.saveAll(List.of(employee))).thenReturn(List.of(savedEmployee));
        when(employeeMapper.toResponse(savedEmployee)).thenReturn(response);

        List<EmployeeResponse> result = employeeService.create(List.of(request));

        assertEquals(List.of(response), result);
        verify(employeeRepository).saveAll(List.of(employee));
    }

    @Test
    void createSavesMultipleEmployeesInOneBatch() {
        EmployeeCreateRequest firstRequest = createRequest(30);
        EmployeeCreateRequest secondRequest = createRequest(40);
        Employee first = new Employee();
        Employee second = new Employee();
        EmployeeResponse firstResponse = new EmployeeResponse();
        EmployeeResponse secondResponse = new EmployeeResponse();
        List<Employee> batch = List.of(first, second);
        when(employeeMapper.toEntity(firstRequest)).thenReturn(first);
        when(employeeMapper.toEntity(secondRequest)).thenReturn(second);
        when(employeeRepository.saveAll(batch)).thenReturn(batch);
        when(employeeMapper.toResponse(first)).thenReturn(firstResponse);
        when(employeeMapper.toResponse(second)).thenReturn(secondResponse);

        List<EmployeeResponse> result = employeeService.create(List.of(firstRequest, secondRequest));

        assertEquals(List.of(firstResponse, secondResponse), result);
        verify(employeeRepository).saveAll(batch);
    }

    @Test
    void createRejectsEmptyList() {
        List<EmployeeCreateRequest> requests = List.of();

        assertThrows(InvalidEmployeeDataException.class, () -> employeeService.create(requests));

        verify(employeeRepository, never()).saveAll(any());
    }

    @Test
    void createRejectsNullListElement() {
        List<EmployeeCreateRequest> requests = Collections.singletonList(null);

        assertThrows(InvalidEmployeeDataException.class, () -> employeeService.create(requests));

        verify(employeeRepository, never()).saveAll(any());
    }

    @Test
    void createRejectsAgeThatDoesNotMatchBirthDate() {
        EmployeeCreateRequest request = createRequest(30);
        request.setAge(31);

        assertThrows(InvalidEmployeeDataException.class, () -> employeeService.create(List.of(request)));

        verify(employeeRepository, never()).saveAll(any());
    }

    @Test
    void createRejectsEntireBatchWhenOneEmployeeIsInvalid() {
        EmployeeCreateRequest validRequest = createRequest(30);
        EmployeeCreateRequest invalidRequest = createRequest(40);
        invalidRequest.setAge(41);

        assertThrows(InvalidEmployeeDataException.class,
                () -> employeeService.create(List.of(validRequest, invalidRequest)));

        verify(employeeRepository, never()).saveAll(any());
        verifyNoInteractions(employeeMapper);
    }

    @Test
    void updateMapsChangesAndReturnsSavedEmployee() {
        EmployeeUpdateRequest request = new EmployeeUpdateRequest();
        request.setFirstName("Alex");
        Employee employee = new Employee();
        Employee savedEmployee = new Employee();
        EmployeeResponse response = new EmployeeResponse();
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(employeeRepository.save(employee)).thenReturn(savedEmployee);
        when(employeeMapper.toResponse(savedEmployee)).thenReturn(response);

        EmployeeResponse result = employeeService.update(1L, request);

        assertSame(response, result);
        verify(employeeMapper).updateEntity(request, employee);
        verify(employeeRepository).save(employee);
    }

    @Test
    void updateRejectsMissingEmployee() {
        EmployeeUpdateRequest request = new EmployeeUpdateRequest();
        when(employeeRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(EmployeeNotFoundException.class, () -> employeeService.update(1L, request));

        verify(employeeRepository, never()).save(any());
        verifyNoInteractions(employeeMapper);
    }

    @Test
    void updateRejectsBlankFirstName() {
        EmployeeUpdateRequest request = new EmployeeUpdateRequest();
        request.setFirstName("   ");
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(new Employee()));

        assertThrows(InvalidEmployeeDataException.class, () -> employeeService.update(1L, request));

        verify(employeeRepository, never()).save(any());
        verifyNoInteractions(employeeMapper);
    }

    @Test
    void updateRejectsAgeThatDoesNotMatchExistingBirthDate() {
        EmployeeUpdateRequest request = new EmployeeUpdateRequest();
        request.setAge(31);
        Employee employee = new Employee();
        employee.setAge(30);
        employee.setBirthDate(LocalDate.now().minusYears(30).minusWeeks(1));
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
        doAnswer(invocation -> {
            employee.setAge(request.getAge());
            return null;
        }).when(employeeMapper).updateEntity(request, employee);

        assertThrows(InvalidEmployeeDataException.class, () -> employeeService.update(1L, request));

        verify(employeeRepository, never()).save(any());
    }

    @Test
    void deleteRemovesExistingEmployee() {
        Employee employee = new Employee();
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));

        employeeService.delete(1L);

        verify(employeeRepository).delete(employee);
    }

    @Test
    void deleteRejectsMissingEmployee() {
        when(employeeRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(EmployeeNotFoundException.class, () -> employeeService.delete(1L));

        verify(employeeRepository, never()).delete(any());
    }

    @Test
    void searchByNameTrimsInputAndReturnsMappedMatches() {
        Employee employee = new Employee();
        EmployeeResponse response = new EmployeeResponse();
        when(employeeRepository.searchByName("Alex")).thenReturn(List.of(employee));
        when(employeeMapper.toResponse(employee)).thenReturn(response);

        List<EmployeeResponse> result = employeeService.searchByName("  Alex  ");

        assertEquals(List.of(response), result);
        verify(employeeRepository).searchByName("Alex");
    }

    @Test
    void searchByNameReturnsEmptyListWhenNoEmployeesMatch() {
        when(employeeRepository.searchByName("Alex")).thenReturn(List.of());

        List<EmployeeResponse> result = employeeService.searchByName("Alex");

        assertTrue(result.isEmpty());
        verifyNoInteractions(employeeMapper);
    }

    @Test
    void searchByNameRejectsBlankName() {
        String name = "   ";

        assertThrows(InvalidEmployeeDataException.class, () -> employeeService.searchByName(name));

        verify(employeeRepository, never()).searchByName(anyString());
    }

    private EmployeeCreateRequest createRequest(int age) {
        EmployeeCreateRequest request = new EmployeeCreateRequest();
        request.setAge(age);
        request.setBirthDate(LocalDate.now().minusYears(age).minusWeeks(1));
        return request;
    }
}
