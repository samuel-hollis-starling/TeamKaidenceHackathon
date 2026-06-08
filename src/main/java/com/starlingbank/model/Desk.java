package com.starlingbank.model;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Desk {
    private String id;
    private String name;
    private String neighborhood;
    private double x;
    private double y;
    private double rotation;

    public Desk() {}

    public Desk(String id, String name, String neighborhood, double x, double y, double rotation) {
        this.id = id;
        this.name = name;
        this.neighborhood = neighborhood;
        this.x = x;
        this.y = y;
        this.rotation = rotation;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getNeighborhood() { return neighborhood; }
    public double getX() { return x; }
    public double getY() { return y; }
    public double getRotation() { return rotation; }
}
