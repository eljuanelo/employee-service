package com.invex.employee.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.invex.employee.dto.EmployeeCreateRequest;
import com.invex.employee.dto.EmployeeResponse;
import com.invex.employee.dto.EmployeeUpdateRequest;
import com.invex.employee.enums.Sex;
import com.invex.employee.exception.EmployeeNotFoundException;
import com.invex.employee.exception.GlobalExceptionHandler;
import com.invex.employee.exception.InvalidEmployeeDataException;
import com.invex.employee.service.EmployeeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EmployeeController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class EmployeeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private EmployeeService employeeService;

    // Returns HTTP 200 with employee data.
    @Test
    void findAllReturnsEmployees() throws Exception {
        when(employeeService.findAll()).thenReturn(List.of(response(1L, "Alex")));

        mockMvc.perform(get("/employees"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].firstName").value("Alex"));

        verify(employeeService).findAll();
    }

    // Returns HTTP 200 with an empty JSON array.
    @Test
    void findAllReturnsEmptyArray() throws Exception {
        when(employeeService.findAll()).thenReturn(List.of());

        mockMvc.perform(get("/employees"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        verify(employeeService).findAll();
    }

    // Returns HTTP 200 with the requested employee.
    @Test
    void findByIdReturnsExistingEmployee() throws Exception {
        when(employeeService.findById(1L)).thenReturn(response(1L, "Alex"));

        mockMvc.perform(get("/employees/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.firstName").value("Alex"));

        verify(employeeService).findById(1L);
    }

    // Returns HTTP 404 with details for an unknown ID.
    @Test
    void findByIdReturnsNotFoundForMissingEmployee() throws Exception {
        when(employeeService.findById(1L)).thenThrow(new EmployeeNotFoundException("Employee not found."));

        mockMvc.perform(get("/employees/1"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Employee not found."))
                .andExpect(jsonPath("$.path").value("/employees/1"));

        verify(employeeService).findById(1L);
    }

    // Returns HTTP 400 for a malformed ID before calling the service.
    @Test
    void findByIdRejectsMalformedId() throws Exception {
        mockMvc.perform(get("/employees/invalid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("id: invalid value type."));

        verifyNoInteractions(employeeService);
    }

    // Accepts one JSON object and returns HTTP 201 with an array.
    @Test
    void createAcceptsSingleEmployeeObject() throws Exception {
        EmployeeCreateRequest request = createRequest("Alex");
        when(employeeService.create(anyList())).thenReturn(List.of(response(1L, "Alex")));

        mockMvc.perform(post("/employees").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].firstName").value("Alex"));

        verify(employeeService).create(argThat(requests -> requests.size() == 1
                && "Alex".equals(requests.get(0).getFirstName())
                && request.getBirthDate().equals(requests.get(0).getBirthDate())));
    }

    // Accepts an employee array and returns HTTP 201 with both records.
    @Test
    void createAcceptsEmployeeArray() throws Exception {
        List<EmployeeCreateRequest> requests = List.of(createRequest("Alex"), createRequest("Sam"));
        when(employeeService.create(anyList())).thenReturn(List.of(response(1L, "Alex"), response(2L, "Sam")));

        mockMvc.perform(post("/employees").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(requests)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].firstName").value("Alex"))
                .andExpect(jsonPath("$[1].firstName").value("Sam"));

        verify(employeeService).create(argThat(batch -> batch.stream()
                .map(EmployeeCreateRequest::getFirstName).toList().equals(List.of("Alex", "Sam"))));
    }

    // Returns HTTP 400 for missing required data before calling the service.
    @Test
    void createRejectsMissingRequiredFields() throws Exception {
        EmployeeCreateRequest request = createRequest("Alex");
        request.setFirstName(null);

        mockMvc.perform(post("/employees").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(List.of(request))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("firstName: must not be blank")));

        verifyNoInteractions(employeeService);
    }

    // Returns HTTP 400 for an empty employee batch.
    @Test
    void createRejectsEmptyArray() throws Exception {
        mockMvc.perform(post("/employees").contentType(MediaType.APPLICATION_JSON).content("[]"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("must not be empty")));

        verifyNoInteractions(employeeService);
    }

    // Returns HTTP 400 when the request body cannot be parsed.
    @Test
    void createRejectsMalformedJson() throws Exception {
        mockMvc.perform(post("/employees").contentType(MediaType.APPLICATION_JSON).content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("The request body is missing or malformed."));

        verifyNoInteractions(employeeService);
    }

    // Returns HTTP 400 with the business validation message.
    @Test
    void createReturnsBadRequestWhenServiceRejectsEmployee() throws Exception {
        EmployeeCreateRequest request = createRequest("Alex");
        request.setAge(31);
        when(employeeService.create(anyList()))
                .thenThrow(new InvalidEmployeeDataException("Age does not match the birth date."));

        mockMvc.perform(post("/employees").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Age does not match the birth date."));

        verify(employeeService).create(anyList());
    }

    // Returns HTTP 200 and forwards the partial update.
    @Test
    void updateAcceptsPartialEmployeeData() throws Exception {
        EmployeeUpdateRequest request = new EmployeeUpdateRequest();
        request.setFirstName("Sam");
        when(employeeService.update(eq(1L), any(EmployeeUpdateRequest.class))).thenReturn(response(1L, "Sam"));

        mockMvc.perform(put("/employees/1").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.firstName").value("Sam"));

        verify(employeeService).update(eq(1L), argThat(update ->
                "Sam".equals(update.getFirstName()) && update.getActive() == null && update.getBirthDate() == null));
    }

    // Returns HTTP 404 when the employee to update is missing.
    @Test
    void updateReturnsNotFoundForMissingEmployee() throws Exception {
        EmployeeUpdateRequest request = new EmployeeUpdateRequest();
        request.setFirstName("Sam");
        when(employeeService.update(eq(1L), any(EmployeeUpdateRequest.class)))
                .thenThrow(new EmployeeNotFoundException("Employee not found."));

        mockMvc.perform(put("/employees/1").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Employee not found."));

        verify(employeeService).update(eq(1L), any(EmployeeUpdateRequest.class));
    }

    // Returns HTTP 400 for a negative age before calling the service.
    @Test
    void updateRejectsInvalidEmployeeData() throws Exception {
        EmployeeUpdateRequest request = new EmployeeUpdateRequest();
        request.setAge(-1);

        mockMvc.perform(put("/employees/1").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("age: must be greater than or equal to zero"));

        verifyNoInteractions(employeeService);
    }

    // Returns HTTP 204 with an empty response body.
    @Test
    void deleteReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/employees/1"))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(employeeService).delete(1L);
    }

    // Returns HTTP 404 when the employee to delete is missing.
    @Test
    void deleteReturnsNotFoundForMissingEmployee() throws Exception {
        doThrow(new EmployeeNotFoundException("Employee not found.")).when(employeeService).delete(1L);

        mockMvc.perform(delete("/employees/1"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Employee not found."));

        verify(employeeService).delete(1L);
    }

    // Returns HTTP 200 with employees matching the query.
    @Test
    void searchByNameReturnsMatchingEmployees() throws Exception {
        when(employeeService.searchByName("Ale")).thenReturn(List.of(response(1L, "Alex")));

        mockMvc.perform(get("/employees/search").param("name", "Ale"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].firstName").value("Alex"));

        verify(employeeService).searchByName("Ale");
    }

    // Returns HTTP 200 with an empty array when no employee matches.
    @Test
    void searchByNameReturnsEmptyArrayWhenNoEmployeesMatch() throws Exception {
        when(employeeService.searchByName("Unknown")).thenReturn(List.of());

        mockMvc.perform(get("/employees/search").param("name", "Unknown"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        verify(employeeService).searchByName("Unknown");
    }

    // Returns HTTP 400 when the name parameter is absent.
    @Test
    void searchByNameRejectsMissingNameParameter() throws Exception {
        mockMvc.perform(get("/employees/search"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("name: required parameter is missing."));

        verifyNoInteractions(employeeService);
    }

    // Builds a valid creation request with a relative birth date.
    private EmployeeCreateRequest createRequest(String firstName) {
        EmployeeCreateRequest request = new EmployeeCreateRequest();
        request.setFirstName(firstName);
        request.setLastName("Morgan");
        request.setAge(30);
        request.setSex(Sex.FEMALE);
        request.setBirthDate(LocalDate.now().minusYears(30).minusWeeks(1));
        request.setPosition("Engineer");
        return request;
    }

    // Builds a minimal employee response for JSON assertions.
    private EmployeeResponse response(Long id, String firstName) {
        EmployeeResponse response = new EmployeeResponse();
        response.setId(id);
        response.setFirstName(firstName);
        return response;
    }
}
