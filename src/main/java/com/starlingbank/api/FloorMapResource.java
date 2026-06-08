package com.starlingbank.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.starlingbank.model.FloorMap;
import com.starlingbank.service.FloorMapService;
import jakarta.inject.Inject;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import com.starlingbank.parser.FloorMapParser;
import java.util.Map;

@Path("/api/floor-map")
public class FloorMapResource {

    private static final ObjectMapper MAPPER = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    private final FloorMapService floorMapService;

    @Inject
    public FloorMapResource(FloorMapService floorMapService) {
        this.floorMapService = floorMapService;
    }

    @POST
    @Path("/parse-har")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Object> parseHar(@QueryParam("har") String harFile) throws Exception {
        java.nio.file.Path harPath = java.nio.file.Path.of("input-data/har/" + harFile);
        FloorMap floorMap = new FloorMapParser().parse(harPath);

        String slug = toSlug(floorMap.getFloor().getBuilding()) + "-" + toSlug(floorMap.getFloor().getName());
        java.nio.file.Path outPath = java.nio.file.Path.of("input-data/floors/" + slug + ".json");
        MAPPER.writeValue(outPath.toFile(), floorMap);

        floorMapService.register(floorMap);

        return Map.of(
            "file", outPath.toString(),
            "floor", floorMap.getFloor().getName(),
            "building", floorMap.getFloor().getBuilding(),
            "desks", floorMap.getSpaces().getDesks().size(),
            "pods", floorMap.getSpaces().getPods().size(),
            "walls", floorMap.getWalls().size(),
            "landmarks", floorMap.getLandmarks().size()
        );
    }

    private static String toSlug(String s) {
        return s.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("^-|-$", "");
    }
}
