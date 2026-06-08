package com.starlingbank.model;

public class Employee {
    private String id;
    private String name;
    private String role;
    private String location;

    public Employee() {}

    public Employee(String id, String name, String role, String location) {
        this.id = id;
        this.name = name;
        this.role = role;
        this.location = location;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getRole() { return role; }
    public String getLocation() { return location; }
}
