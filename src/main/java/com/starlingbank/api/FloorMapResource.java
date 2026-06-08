package com.starlingbank.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.starlingbank.model.FloorMap;
import com.starlingbank.service.FloorMapService;
import jakarta.inject.Inject;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Path("/api/floor-map")
public class FloorMapResource {

    private static final ObjectMapper MAPPER = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH-mm-ss");

    private final FloorMapService floorMapService;

    @Inject
    public FloorMapResource(FloorMapService floorMapService) {
        this.floorMapService = floorMapService;
    }

    @POST
    @Path("/export")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Object> export() throws Exception {
        FloorMap floorMap = floorMapService.getFloorMap();
        String timestamp = LocalDateTime.now().format(FORMATTER);
        String filename = "input-data/floor-map-" + timestamp + ".json";
        MAPPER.writeValue(new File(filename), floorMap);
        return Map.of(
            "file", filename,
            "desks", floorMap.getSpaces().getDesks().size(),
            "pods", floorMap.getSpaces().getPods().size(),
            "walls", floorMap.getWalls().size(),
            "landmarks", floorMap.getLandmarks().size()
        );
    }
}
