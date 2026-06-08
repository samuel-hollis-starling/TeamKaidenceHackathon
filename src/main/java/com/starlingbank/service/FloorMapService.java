package com.starlingbank.service;

import com.starlingbank.model.Desk;
import com.starlingbank.model.FloorMap;
import java.util.List;

public interface FloorMapService {
    FloorMap getFloorMap();
    void register(FloorMap floorMap);

    default List<Desk> getDesks() {
        return getFloorMap().getSpaces().getDesks();
    }
}
