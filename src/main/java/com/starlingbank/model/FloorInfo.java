package com.starlingbank.model;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class FloorInfo {
    private String id;
    private String name;
    private String building;
    private ViewBox viewBox;

    public FloorInfo() {}

    public FloorInfo(String id, String name, String building, ViewBox viewBox) {
        this.id = id;
        this.name = name;
        this.building = building;
        this.viewBox = viewBox;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getBuilding() { return building; }
    public ViewBox getViewBox() { return viewBox; }
}
