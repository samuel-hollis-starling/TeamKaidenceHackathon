package com.starlingbank.api;

import com.starlingbank.model.AssignmentCollection;
import com.starlingbank.model.AssignmentScore;
import com.starlingbank.model.BookingRequest;
import com.starlingbank.model.Desk;
import com.starlingbank.service.AssignmentService;
import com.starlingbank.service.AssignmentState;
import com.starlingbank.service.BookingService;
import com.starlingbank.service.FloorMapService;
import com.starlingbank.service.ScoringService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import java.util.List;

@Path("/api/assignments")
public class AssignmentResource {

    private final AssignmentService assignmentService;
    private final AssignmentState assignmentState;
    private final BookingService bookingService;
    private final FloorMapService floorMapService;
    private final ScoringService scoringService;

    @Inject
    public AssignmentResource(AssignmentService assignmentService, AssignmentState assignmentState,
                              BookingService bookingService, FloorMapService floorMapService,
                              ScoringService scoringService) {
        this.assignmentService = assignmentService;
        this.assignmentState = assignmentState;
        this.bookingService = bookingService;
        this.floorMapService = floorMapService;
        this.scoringService = scoringService;
    }

    @POST
    @Path("/run")
    @Produces(MediaType.APPLICATION_JSON)
    public AssignmentCollection run() {
        List<BookingRequest> bookings = bookingService.getBookings().getBookings();
        if (bookings.isEmpty()) {
            return assignmentState.get();
        }
        List<Desk> desks = floorMapService.getDesks();
        AssignmentCollection result = assignmentService.assign(bookings, desks);
        assignmentState.set(result);
        return result;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public AssignmentCollection getAssignment() {
        return assignmentState.get();
    }

    @GET
    @Path("/score")
    @Produces(MediaType.APPLICATION_JSON)
    public AssignmentScore getScore() {
        return scoringService.score(
                assignmentState.get(),
                bookingService.getBookings().getBookings(),
                floorMapService.getDesks()
        );
    }
}
