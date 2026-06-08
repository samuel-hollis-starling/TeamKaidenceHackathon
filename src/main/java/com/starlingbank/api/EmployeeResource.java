package com.starlingbank.api;

import com.starlingbank.model.Employee;
import com.starlingbank.service.OrgChartService;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.List;

@Path("/api/employees")
public class EmployeeResource {

    private final OrgChartService orgChartService;

    @Inject
    public EmployeeResource(OrgChartService orgChartService) {
        this.orgChartService = orgChartService;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<Employee> getEmployees() {
        return List.copyOf(orgChartService.getEmployees().values());
    }
}
