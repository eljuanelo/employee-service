package com.invex.employee.mapper;

import com.invex.employee.dto.EmployeeCreateRequest;
import com.invex.employee.dto.EmployeeResponse;
import com.invex.employee.dto.EmployeeUpdateRequest;
import com.invex.employee.entity.Employee;
import com.invex.employee.enums.Sex;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class EmployeeMapperTest {

    private static final LocalDate BIRTH_DATE = LocalDate.of(1990, 4, 12);
    private static final LocalDateTime CREATED_AT = LocalDateTime.of(2025, 3, 10, 9, 30);

    private final EmployeeMapper mapper = new EmployeeMapper();

    // Copies creation fields and leaves generated fields unset.
    @Test
    void toEntityMapsAllRequestFields() {
        EmployeeCreateRequest request = new EmployeeCreateRequest();
        request.setFirstName("Alex");
        request.setMiddleName("Taylor");
        request.setLastName("Morgan");
        request.setSecondLastName("Reed");
        request.setAge(35);
        request.setSex(Sex.FEMALE);
        request.setBirthDate(BIRTH_DATE);
        request.setPosition("Engineer");
        request.setActive(true);

        Employee employee = mapper.toEntity(request);

        assertEquals(request.getFirstName(), employee.getFirstName());
        assertEquals(request.getMiddleName(), employee.getMiddleName());
        assertEquals(request.getLastName(), employee.getLastName());
        assertEquals(request.getSecondLastName(), employee.getSecondLastName());
        assertEquals(request.getAge(), employee.getAge());
        assertEquals(request.getSex(), employee.getSex());
        assertEquals(request.getBirthDate(), employee.getBirthDate());
        assertEquals(request.getPosition(), employee.getPosition());
        assertEquals(request.getActive(), employee.getActive());
        assertNull(employee.getId());
        assertNull(employee.getCreatedAt());
    }

    // Keeps optional names null when omitted.
    @Test
    void toEntityLeavesOmittedOptionalNamesNull() {
        EmployeeCreateRequest request = new EmployeeCreateRequest();

        Employee employee = mapper.toEntity(request);

        assertNull(employee.getMiddleName());
        assertNull(employee.getSecondLastName());
    }

    // Keeps an explicit false active value during creation.
    @Test
    void toEntityPreservesExplicitInactiveStatus() {
        EmployeeCreateRequest request = new EmployeeCreateRequest();
        request.setActive(false);

        Employee employee = mapper.toEntity(request);

        assertEquals(Boolean.FALSE, employee.getActive());
    }

    // Keeps the default active status when the request omits it.
    @Test
    void toEntityPreservesDefaultStatusWhenActiveIsOmitted() {
        EmployeeCreateRequest request = new EmployeeCreateRequest();

        Employee employee = mapper.toEntity(request);

        assertEquals(Boolean.TRUE, employee.getActive());
    }

    // Copies all fields, including the ID and creation timestamp.
    @Test
    void toResponseMapsAllEmployeeFields() {
        Employee employee = existingEmployee();
        employee.setActive(false);

        EmployeeResponse response = mapper.toResponse(employee);

        assertEquals(employee.getId(), response.getId());
        assertEquals(employee.getFirstName(), response.getFirstName());
        assertEquals(employee.getMiddleName(), response.getMiddleName());
        assertEquals(employee.getLastName(), response.getLastName());
        assertEquals(employee.getSecondLastName(), response.getSecondLastName());
        assertEquals(employee.getAge(), response.getAge());
        assertEquals(employee.getSex(), response.getSex());
        assertEquals(employee.getBirthDate(), response.getBirthDate());
        assertEquals(employee.getPosition(), response.getPosition());
        assertEquals(CREATED_AT, response.getCreatedAt());
        assertEquals(employee.getActive(), response.getActive());
    }

    // Updates editable fields while preserving the ID and creation timestamp.
    @Test
    void updateEntityUpdatesAllAllowedFieldsAndPreservesIdentityAndCreationDate() {
        Employee employee = existingEmployee();
        EmployeeUpdateRequest request = new EmployeeUpdateRequest();
        request.setFirstName("Sam");
        request.setMiddleName("Lee");
        request.setLastName("Parker");
        request.setSecondLastName("Gray");
        request.setAge(40);
        request.setSex(Sex.MALE);
        request.setBirthDate(BIRTH_DATE.minusYears(5));
        request.setPosition("Manager");
        request.setActive(false);

        mapper.updateEntity(request, employee);

        assertEquals(request.getFirstName(), employee.getFirstName());
        assertEquals(request.getMiddleName(), employee.getMiddleName());
        assertEquals(request.getLastName(), employee.getLastName());
        assertEquals(request.getSecondLastName(), employee.getSecondLastName());
        assertEquals(request.getAge(), employee.getAge());
        assertEquals(request.getSex(), employee.getSex());
        assertEquals(request.getBirthDate(), employee.getBirthDate());
        assertEquals(request.getPosition(), employee.getPosition());
        assertEquals(request.getActive(), employee.getActive());
        assertEquals(42L, employee.getId());
        assertEquals(CREATED_AT, employee.getCreatedAt());
    }

    // Changes supplied fields and preserves the remaining values.
    @Test
    void updateEntityChangesOnlyFieldsPresentInPartialRequest() {
        Employee employee = existingEmployee();
        EmployeeUpdateRequest request = new EmployeeUpdateRequest();
        request.setFirstName("Sam");
        request.setPosition("Manager");

        mapper.updateEntity(request, employee);

        assertEquals("Sam", employee.getFirstName());
        assertEquals("Manager", employee.getPosition());
        assertEquals("Taylor", employee.getMiddleName());
        assertEquals("Morgan", employee.getLastName());
        assertEquals("Reed", employee.getSecondLastName());
        assertEquals(35, employee.getAge());
        assertEquals(Sex.FEMALE, employee.getSex());
        assertEquals(BIRTH_DATE, employee.getBirthDate());
        assertEquals(Boolean.TRUE, employee.getActive());
    }

    // Leaves existing values unchanged when all update fields are null.
    @Test
    void updateEntityPreservesExistingValuesWhenAllRequestFieldsAreNull() {
        Employee employee = existingEmployee();
        employee.setActive(false);
        EmployeeUpdateRequest request = new EmployeeUpdateRequest();

        mapper.updateEntity(request, employee);

        assertEquals("Alex", employee.getFirstName());
        assertEquals("Taylor", employee.getMiddleName());
        assertEquals("Morgan", employee.getLastName());
        assertEquals("Reed", employee.getSecondLastName());
        assertEquals(35, employee.getAge());
        assertEquals(Sex.FEMALE, employee.getSex());
        assertEquals(BIRTH_DATE, employee.getBirthDate());
        assertEquals("Engineer", employee.getPosition());
        assertEquals(Boolean.FALSE, employee.getActive());
    }

    // Applies an explicit active status in both directions.
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void updateEntityAppliesExplicitActiveStatusInEitherDirection(boolean newStatus) {
        Employee employee = existingEmployee();
        employee.setActive(!newStatus);
        EmployeeUpdateRequest request = new EmployeeUpdateRequest();
        request.setActive(newStatus);

        mapper.updateEntity(request, employee);

        assertEquals(Boolean.valueOf(newStatus), employee.getActive());
    }

    // Builds an existing employee with a fixed ID and creation timestamp.
    private Employee existingEmployee() {
        Employee employee = new Employee();
        employee.setId(42L);
        employee.setFirstName("Alex");
        employee.setMiddleName("Taylor");
        employee.setLastName("Morgan");
        employee.setSecondLastName("Reed");
        employee.setAge(35);
        employee.setSex(Sex.FEMALE);
        employee.setBirthDate(BIRTH_DATE);
        employee.setPosition("Engineer");
        ReflectionTestUtils.setField(employee, "createdAt", CREATED_AT);
        return employee;
    }
}
