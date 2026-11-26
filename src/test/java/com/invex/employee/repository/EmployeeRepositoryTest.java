package com.invex.employee.repository;

import com.invex.employee.entity.Employee;
import com.invex.employee.enums.Sex;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import javax.persistence.EntityManager;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@ActiveProfiles("test")
class EmployeeRepositoryTest {

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private EntityManager entityManager;

    // Verifies that employee fields survive a round trip to the database.
    @Test
    void savesAndRetrievesEmployeeFields() {
        Employee employee = newEmployee("Alex");
        employee.setMiddleName("Taylor");
        employee.setSecondLastName("Reed");

        Employee stored = saveAndReload(employee);

        assertEquals(employee.getFirstName(), stored.getFirstName());
        assertEquals(employee.getMiddleName(), stored.getMiddleName());
        assertEquals(employee.getLastName(), stored.getLastName());
        assertEquals(employee.getSecondLastName(), stored.getSecondLastName());
        assertEquals(employee.getAge(), stored.getAge());
        assertEquals(employee.getSex(), stored.getSex());
        assertEquals(employee.getBirthDate(), stored.getBirthDate());
        assertEquals(employee.getPosition(), stored.getPosition());
    }

    // Verifies that persistence assigns an ID to a new employee.
    @Test
    void generatesIdWhenSavingNewEmployee() {
        Employee employee = newEmployee("Alex");
        assertNull(employee.getId());

        Employee stored = saveAndReload(employee);

        assertNotNull(stored.getId());
        assertEquals(employee.getId(), stored.getId());
    }

    // Verifies that persistence initializes the creation timestamp.
    @Test
    void generatesCreationTimestampWhenSavingNewEmployee() {
        Employee employee = newEmployee("Alex");
        assertNull(employee.getCreatedAt());

        Employee stored = saveAndReload(employee);

        assertNotNull(stored.getCreatedAt());
    }

    // Verifies that an employee is stored as active by default.
    @Test
    void savesEmployeeAsActiveByDefault() {
        Employee employee = newEmployee("Alex");

        Employee stored = saveAndReload(employee);

        assertEquals(Boolean.TRUE, stored.getActive());
    }

    // Verifies that an explicitly inactive employee is stored as inactive.
    @Test
    void preservesExplicitInactiveStatus() {
        Employee employee = newEmployee("Alex");
        employee.setActive(false);

        Employee stored = saveAndReload(employee);

        assertEquals(Boolean.FALSE, stored.getActive());
    }

    // Verifies that updating the position preserves the creation timestamp.
    @Test
    void updatesEmployeeWithoutChangingCreationTimestamp() {
        Employee employee = saveAndReload(newEmployee("Alex"));
        LocalDateTime originalCreatedAt = employee.getCreatedAt();
        assertNotNull(originalCreatedAt);
        employee.setPosition("Manager");

        Employee updated = saveAndReload(employee);

        assertEquals("Manager", updated.getPosition());
        assertEquals(originalCreatedAt, updated.getCreatedAt());
    }

    // Verifies that a deleted row can no longer be fetched by ID.
    @Test
    void physicallyDeletesEmployee() {
        Employee employee = saveAndReload(newEmployee("Alex"));
        Long id = employee.getId();

        employeeRepository.delete(employee);
        employeeRepository.flush();
        entityManager.clear();

        assertTrue(employeeRepository.findById(id).isEmpty());
    }

    // Verifies a partial match for each of the four name fields.
    @ParameterizedTest
    @ValueSource(strings = {"lex", "ayl", "org", "ee"})
    void findsPartialMatchInEachNameField(String nameFragment) {
        Employee employee = newEmployee("Alex");
        employee.setMiddleName("Taylor");
        employee.setSecondLastName("Reed");
        Long id = employeeRepository.saveAndFlush(employee).getId();

        List<Employee> matches = employeeRepository.searchByName(nameFragment);

        assertEquals(List.of(id), matches.stream().map(Employee::getId).toList());
    }

    // Verifies that a mixed-case search matches the stored name.
    @Test
    void findsMatchesIgnoringCase() {
        Long id = employeeRepository.saveAndFlush(newEmployee("Alex")).getId();

        List<Employee> matches = employeeRepository.searchByName("aLeX");

        assertEquals(List.of(id), matches.stream().map(Employee::getId).toList());
    }

    // Returns every matching employee and excludes non-matching records.
    @Test
    void returnsAllMatchingEmployees() {
        Long firstId = employeeRepository.saveAndFlush(newEmployee("Alex")).getId();
        Long secondId = employeeRepository.saveAndFlush(newEmployee("Alexa")).getId();
        employeeRepository.saveAndFlush(newEmployee("Sam"));

        List<Employee> matches = employeeRepository.searchByName("lex");

        assertEquals(2, matches.size());
        assertEquals(Set.of(firstId, secondId), matches.stream().map(Employee::getId).collect(Collectors.toSet()));
    }

    // Returns an empty list when no name matches.
    @Test
    void returnsEmptyListWhenNoNameMatches() {
        employeeRepository.saveAndFlush(newEmployee("Alex"));

        List<Employee> matches = employeeRepository.searchByName("Unknown");

        assertTrue(matches.isEmpty());
    }

    // Matches a last name even when optional name fields are null.
    @Test
    void findsEmployeeByLastNameWhenOptionalNamesAreNull() {
        Employee employee = saveAndReload(newEmployee("Alex"));

        List<Employee> matches = employeeRepository.searchByName("orga");

        assertEquals(List.of(employee.getId()), matches.stream().map(Employee::getId).toList());
        assertNull(matches.get(0).getMiddleName());
        assertNull(matches.get(0).getSecondLastName());
    }

    // Flushes changes and clears the persistence context before reloading the employee.
    private Employee saveAndReload(Employee employee) {
        Long id = employeeRepository.saveAndFlush(employee).getId();
        entityManager.clear();
        return employeeRepository.findById(id).orElseThrow();
    }

    // Builds an employee with required fields and optional names unset.
    private Employee newEmployee(String firstName) {
        Employee employee = new Employee();
        employee.setFirstName(firstName);
        employee.setLastName("Morgan");
        employee.setAge(30);
        employee.setSex(Sex.FEMALE);
        employee.setBirthDate(LocalDate.now().minusYears(30).minusWeeks(1));
        employee.setPosition("Engineer");
        return employee;
    }
}
