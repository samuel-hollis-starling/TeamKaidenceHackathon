package com.starlingbank.api;

import com.starlingbank.model.BookingCollection;
import com.starlingbank.model.BookingRequest;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import java.util.List;

@Path("/api/bookings")
public class BookingResource {

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public BookingRequest addBooking(BookingRequest request) {
        return request;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public BookingCollection getBookings() {
        return new BookingCollection(List.of(), 191, 191);
    }
}
