package com.starlingbank.service;

import com.starlingbank.model.Employee;
import com.starlingbank.model.OrgNode;
import java.util.Map;

public interface OrgChartService {
    Map<String, Employee> getEmployees();
    Map<String, OrgNode> getOrgNodes();
}
