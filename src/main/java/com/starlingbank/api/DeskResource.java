package com.starlingbank.api;

import jakarta.inject.Inject;
import com.starlingbank.model.Desk;
import com.starlingbank.service.FloorMapService;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.List;

@Path("/api/desks")
public class DeskResource {

    private final FloorMapService floorMapService;

    @Inject
    public DeskResource(FloorMapService floorMapService) {
        this.floorMapService = floorMapService;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<Desk> getDesks() {
        return floorMapService.getDesks();
    }
}
