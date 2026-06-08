package com.starlingbank.api;

import com.starlingbank.model.BookingCollection;
import com.starlingbank.model.BookingRequest;
import com.starlingbank.service.BookingService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

@Path("/api/bookings")
public class BookingResource {

    private final BookingService bookingService;

    @Inject
    public BookingResource(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public BookingRequest addBooking(BookingRequest request) {
        return bookingService.addBooking(request);
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public BookingCollection getBookings() {
        return bookingService.getBookings();
    }
}
