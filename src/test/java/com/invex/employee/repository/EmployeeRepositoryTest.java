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

    @Test
    void generatesIdWhenSavingNewEmployee() {
        Employee employee = newEmployee("Alex");
        assertNull(employee.getId());

        Employee stored = saveAndReload(employee);

        assertNotNull(stored.getId());
        assertEquals(employee.getId(), stored.getId());
    }

    @Test
    void generatesCreationTimestampWhenSavingNewEmployee() {
        Employee employee = newEmployee("Alex");
        assertNull(employee.getCreatedAt());

        Employee stored = saveAndReload(employee);

        assertNotNull(stored.getCreatedAt());
    }

    @Test
    void savesEmployeeAsActiveByDefault() {
        Employee employee = newEmployee("Alex");

        Employee stored = saveAndReload(employee);

        assertEquals(Boolean.TRUE, stored.getActive());
    }

    @Test
    void preservesExplicitInactiveStatus() {
        Employee employee = newEmployee("Alex");
        employee.setActive(false);

        Employee stored = saveAndReload(employee);

        assertEquals(Boolean.FALSE, stored.getActive());
    }

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

    @Test
    void physicallyDeletesEmployee() {
        Employee employee = saveAndReload(newEmployee("Alex"));
        Long id = employee.getId();

        employeeRepository.delete(employee);
        employeeRepository.flush();
        entityManager.clear();

        assertTrue(employeeRepository.findById(id).isEmpty());
    }

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

    @Test
    void findsMatchesIgnoringCase() {
        Long id = employeeRepository.saveAndFlush(newEmployee("Alex")).getId();

        List<Employee> matches = employeeRepository.searchByName("aLeX");

        assertEquals(List.of(id), matches.stream().map(Employee::getId).toList());
    }

    @Test
    void returnsAllMatchingEmployees() {
        Long firstId = employeeRepository.saveAndFlush(newEmployee("Alex")).getId();
        Long secondId = employeeRepository.saveAndFlush(newEmployee("Alexa")).getId();
        employeeRepository.saveAndFlush(newEmployee("Sam"));

        List<Employee> matches = employeeRepository.searchByName("lex");

        assertEquals(2, matches.size());
        assertEquals(Set.of(firstId, secondId), matches.stream().map(Employee::getId).collect(Collectors.toSet()));
    }

    @Test
    void returnsEmptyListWhenNoNameMatches() {
        employeeRepository.saveAndFlush(newEmployee("Alex"));

        List<Employee> matches = employeeRepository.searchByName("Unknown");

        assertTrue(matches.isEmpty());
    }

    @Test
    void findsEmployeeByLastNameWhenOptionalNamesAreNull() {
        Employee employee = saveAndReload(newEmployee("Alex"));

        List<Employee> matches = employeeRepository.searchByName("orga");

        assertEquals(List.of(employee.getId()), matches.stream().map(Employee::getId).toList());
        assertNull(matches.get(0).getMiddleName());
        assertNull(matches.get(0).getSecondLastName());
    }

    private Employee saveAndReload(Employee employee) {
        Long id = employeeRepository.saveAndFlush(employee).getId();
        entityManager.clear();
        return employeeRepository.findById(id).orElseThrow();
    }

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
