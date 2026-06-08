package com.starlingbank.api;

import com.starlingbank.model.AssignmentCollection;
import com.starlingbank.model.AssignmentScore;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import java.util.Map;

@Path("/api/assignments")
public class AssignmentResource {

    @POST
    @Path("/run")
    @Produces(MediaType.APPLICATION_JSON)
    public AssignmentCollection run() {
        return new AssignmentCollection(Map.of(), Map.of());
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public AssignmentCollection getAssignment() {
        return new AssignmentCollection(Map.of(), Map.of());
    }

    @GET
    @Path("/score")
    @Produces(MediaType.APPLICATION_JSON)
    public AssignmentScore getScore() {
        return new AssignmentScore(72.0, 58.0, 61.0, 80.0, 65.0);
    }
}
