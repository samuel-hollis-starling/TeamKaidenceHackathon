package com.starlingbank.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Spaces {
    private List<Desk> desks;
    private List<Pod> pods;

    public Spaces() {}

    public Spaces(List<Desk> desks, List<Pod> pods) {
        this.desks = desks;
        this.pods = pods;
    }

    public List<Desk> getDesks() { return desks; }
    public List<Pod> getPods() { return pods; }
}
