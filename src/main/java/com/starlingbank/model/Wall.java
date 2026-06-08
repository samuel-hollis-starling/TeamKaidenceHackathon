package com.starlingbank.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Wall {
    private String id;
    private List<List<Double>> points;

    public Wall() {}

    public Wall(String id, List<List<Double>> points) {
        this.id = id;
        this.points = points;
    }

    public String getId() { return id; }
    public List<List<Double>> getPoints() { return points; }
}
