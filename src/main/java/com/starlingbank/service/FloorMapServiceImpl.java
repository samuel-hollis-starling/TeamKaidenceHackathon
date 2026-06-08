package com.starlingbank.service;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.starlingbank.model.FloorMap;
import com.starlingbank.parser.FloorMapParser;

import java.nio.file.Path;

@Singleton
public class FloorMapServiceImpl implements FloorMapService {

    private final FloorMap floorMap;

    @Inject
    public FloorMapServiceImpl() {
        try {
            this.floorMap = new FloorMapParser().parse(Path.of("input-data/kadence-london.har"));
        } catch (Exception e) {
            throw new RuntimeException("Failed to load floor map from HAR", e);
        }
    }

    @Override
    public FloorMap getFloorMap() {
        return floorMap;
    }
}
