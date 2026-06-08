package com.starlingbank.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.starlingbank.model.Employee;
import com.starlingbank.model.OrgNode;

import java.io.File;
import java.io.IOException;
import java.util.*;

@Singleton
public class OrgChartServiceImpl implements OrgChartService {

    private final Map<String, Employee> employees;
    private final Map<String, OrgNode> orgNodes;

    @Inject
    public OrgChartServiceImpl() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            Map<String, JsonNode> raw = mapper.readValue(
                    new File("input-data/orgchart.json"),
                    mapper.getTypeFactory().constructMapType(Map.class, String.class, JsonNode.class)
            );

            Map<String, Employee> employeeMap = new LinkedHashMap<>();
            Map<String, OrgNode> orgNodeMap = new LinkedHashMap<>();

            for (Map.Entry<String, JsonNode> entry : raw.entrySet()) {
                JsonNode node = entry.getValue();

                String id = node.get("id").asText();
                String name = node.get("name").asText();
                String role = node.get("role").asText();
                String location = node.get("location").asText();
                employeeMap.put(id, new Employee(id, name, role, location));

                String parentId = node.has("parent") && !node.get("parent").isNull()
                        ? node.get("parent").asText()
                        : null;

                List<String> childrenIds = new ArrayList<>();
                JsonNode childrenNode = node.get("children");
                if (childrenNode != null && childrenNode.isArray()) {
                    for (JsonNode child : childrenNode) {
                        childrenIds.add(child.asText());
                    }
                }

                int depth = node.get("depth").asInt();

                List<String> orgPath = new ArrayList<>();
                JsonNode orgPathNode = node.get("orgPath");
                if (orgPathNode != null && orgPathNode.isArray()) {
                    for (JsonNode segment : orgPathNode) {
                        orgPath.add(segment.asText());
                    }
                }

                orgNodeMap.put(id, new OrgNode(id, parentId, childrenIds, depth, orgPath));
            }

            this.employees = Collections.unmodifiableMap(employeeMap);
            this.orgNodes = Collections.unmodifiableMap(orgNodeMap);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load org chart from input-data/orgchart.json", e);
        }
    }

    @Override
    public Map<String, Employee> getEmployees() {
        return employees;
    }

    @Override
    public Map<String, OrgNode> getOrgNodes() {
        return orgNodes;
    }
}
