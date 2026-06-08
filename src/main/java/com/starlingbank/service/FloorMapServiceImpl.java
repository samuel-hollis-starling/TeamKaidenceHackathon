package com.starlingbank.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.starlingbank.model.FloorMap;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Singleton
public class FloorMapServiceImpl implements FloorMapService {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final CopyOnWriteArrayList<FloorMap> floorMaps = new CopyOnWriteArrayList<>();

    @Inject
    public FloorMapServiceImpl() {
        try {
            List<FloorMap> loaded = new ArrayList<>();
            Files.list(Path.of("input-data/floors"))
                    .filter(p -> p.toString().endsWith(".json"))
                    .sorted()
                    .forEach(p -> {
                        try {
                            loaded.add(MAPPER.readValue(p.toFile(), FloorMap.class));
                        } catch (Exception e) {
                            throw new RuntimeException("Failed to load floor map: " + p, e);
                        }
                    });
            if (loaded.isEmpty()) {
                throw new RuntimeException("No floor map JSON files found in input-data/floors/");
            }
            floorMaps.addAll(loaded);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Failed to load floor maps", e);
        }
    }

    @Override
    public FloorMap getFloorMap() {
        return floorMaps.getFirst();
    }

    @Override
    public void register(FloorMap floorMap) {
        String incomingId = floorMap.getFloor().getId();
        floorMaps.removeIf(f -> f.getFloor().getId().equals(incomingId));
        floorMaps.add(floorMap);
    }
}
