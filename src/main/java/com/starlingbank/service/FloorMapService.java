package com.starlingbank.service;

import com.starlingbank.model.Desk;
import com.starlingbank.model.FloorMap;
import java.util.List;

public interface FloorMapService {
    FloorMap getFloorMap();

    default List<Desk> getDesks() {
        return getFloorMap().getSpaces().getDesks();
    }
}
