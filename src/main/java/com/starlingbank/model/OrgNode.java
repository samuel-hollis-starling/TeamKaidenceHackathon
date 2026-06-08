package com.starlingbank.model;

import java.util.List;

public class OrgNode {
    private String employeeId;
    private String parentId;
    private List<String> childrenIds;
    private int depth;
    private List<String> orgPath;

    public OrgNode() {}

    public OrgNode(String employeeId, String parentId, List<String> childrenIds, int depth, List<String> orgPath) {
        this.employeeId = employeeId;
        this.parentId = parentId;
        this.childrenIds = childrenIds;
        this.depth = depth;
        this.orgPath = orgPath;
    }

    public String getEmployeeId() { return employeeId; }
    public String getParentId() { return parentId; }
    public List<String> getChildrenIds() { return childrenIds; }
    public int getDepth() { return depth; }
    public List<String> getOrgPath() { return orgPath; }
}
