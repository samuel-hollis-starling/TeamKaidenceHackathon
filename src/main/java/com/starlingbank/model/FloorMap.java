package com.starlingbank.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class FloorMap {
    private FloorInfo floor;
    private List<String> neighborhoods;
    private Spaces spaces;
    private List<Wall> walls;
    private List<Landmark> landmarks;
    private List<UnavailableSpace> unavailableSpaces;

    public FloorMap() {}

    public FloorMap(FloorInfo floor, List<String> neighborhoods, Spaces spaces,
                    List<Wall> walls, List<Landmark> landmarks, List<UnavailableSpace> unavailableSpaces) {
        this.floor = floor;
        this.neighborhoods = neighborhoods;
        this.spaces = spaces;
        this.walls = walls;
        this.landmarks = landmarks;
        this.unavailableSpaces = unavailableSpaces;
    }

    public FloorInfo getFloor() { return floor; }
    public List<String> getNeighborhoods() { return neighborhoods; }
    public Spaces getSpaces() { return spaces; }
    public List<Wall> getWalls() { return walls; }
    public List<Landmark> getLandmarks() { return landmarks; }
    public List<UnavailableSpace> getUnavailableSpaces() { return unavailableSpaces; }
}
