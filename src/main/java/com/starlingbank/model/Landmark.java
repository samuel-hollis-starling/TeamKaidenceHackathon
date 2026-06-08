package com.starlingbank.model;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Landmark {
    private String type;
    private double x;
    private double y;

    public Landmark() {}

    public Landmark(String type, double x, double y) {
        this.type = type;
        this.x = x;
        this.y = y;
    }

    public String getType() { return type; }
    public double getX() { return x; }
    public double getY() { return y; }
}
