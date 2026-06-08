package com.starlingbank.service;

import com.starlingbank.model.AssignmentCollection;
import com.starlingbank.model.AssignmentScore;
import com.starlingbank.model.BookingRequest;
import com.starlingbank.model.Desk;
import java.util.List;

public interface ScoringService {
    AssignmentScore score(AssignmentCollection assignment, List<BookingRequest> bookings, List<Desk> desks);
}
