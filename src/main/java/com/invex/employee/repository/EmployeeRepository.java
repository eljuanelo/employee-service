package com.invex.employee.repository;

import com.invex.employee.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    // Matches a fragment against any name field, ignoring case.
    @Query("""
            select e from Employee e
            where lower(e.firstName) like lower(concat('%', concat(:name, '%')))
                or lower(e.middleName) like lower(concat('%', concat(:name, '%')))
                or lower(e.lastName) like lower(concat('%', concat(:name, '%')))
                or lower(e.secondLastName) like lower(concat('%', concat(:name, '%')))
            """)
    List<Employee> searchByName(@Param("name") String name);
}
