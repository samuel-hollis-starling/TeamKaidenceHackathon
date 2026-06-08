package com.starlingbank.service;

import com.starlingbank.model.AssignmentCollection;
import com.starlingbank.model.BookingRequest;
import com.starlingbank.model.Desk;
import java.util.List;

public interface AssignmentService {
    AssignmentCollection assign(List<BookingRequest> bookings, List<Desk> desks);
}
