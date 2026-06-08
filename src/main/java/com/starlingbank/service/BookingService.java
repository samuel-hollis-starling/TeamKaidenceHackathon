package com.starlingbank.service;

import com.starlingbank.model.BookingCollection;
import com.starlingbank.model.BookingRequest;

public interface BookingService {
    BookingCollection getBookings();
    BookingRequest addBooking(BookingRequest request);
    void clearBookings();
}
