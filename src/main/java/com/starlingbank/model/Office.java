package com.starlingbank.model;

import java.util.List;
import java.util.Map;

public class Office {
    private List<Desk> desks;
    private Map<String, Employee> employeesById;
    private Map<String, OrgNode> orgById;

    public Office() {}

    public Office(List<Desk> desks, Map<String, Employee> employeesById, Map<String, OrgNode> orgById) {
        this.desks = desks;
        this.employeesById = employeesById;
        this.orgById = orgById;
    }

    public List<Desk> getDesks() { return desks; }
    public Map<String, Employee> getEmployeesById() { return employeesById; }
    public Map<String, OrgNode> getOrgById() { return orgById; }
}
