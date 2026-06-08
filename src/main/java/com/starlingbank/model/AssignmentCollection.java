package com.starlingbank.model;

import java.util.Map;

public class AssignmentCollection {
    private Map<String, String> deskByEmployeeId;
    private Map<String, String> employeeByDeskId;

    public AssignmentCollection() {}

    public AssignmentCollection(Map<String, String> deskByEmployeeId, Map<String, String> employeeByDeskId) {
        this.deskByEmployeeId = deskByEmployeeId;
        this.employeeByDeskId = employeeByDeskId;
    }

    public Map<String, String> getDeskByEmployeeId() { return deskByEmployeeId; }
    public Map<String, String> getEmployeeByDeskId() { return employeeByDeskId; }
}
