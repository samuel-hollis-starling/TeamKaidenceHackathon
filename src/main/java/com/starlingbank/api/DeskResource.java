package com.starlingbank.api;

import com.starlingbank.model.Desk;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.List;

@Path("/api/desks")
public class DeskResource {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<Desk> getDesks() {
        return List.of();
    }
}
