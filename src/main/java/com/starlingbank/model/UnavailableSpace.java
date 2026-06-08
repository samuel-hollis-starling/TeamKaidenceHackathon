package com.starlingbank.model;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class UnavailableSpace {
    private double x;
    private double y;
    private double rotation;

    public UnavailableSpace() {}

    public UnavailableSpace(double x, double y, double rotation) {
        this.x = x;
        this.y = y;
        this.rotation = rotation;
    }

    public double getX() { return x; }
    public double getY() { return y; }
    public double getRotation() { return rotation; }
}
