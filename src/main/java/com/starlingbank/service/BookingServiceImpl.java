package com.starlingbank.service;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.starlingbank.model.BookingCollection;
import com.starlingbank.model.BookingRequest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Singleton
public class BookingServiceImpl implements BookingService {

    private final FloorMapService floorMapService;
    private final CopyOnWriteArrayList<BookingRequest> bookings = new CopyOnWriteArrayList<>();

    @Inject
    public BookingServiceImpl(FloorMapService floorMapService) {
        this.floorMapService = floorMapService;
    }

    @Override
    public BookingRequest addBooking(BookingRequest request) {
        bookings.removeIf(b -> b.getEmployeeId().equals(request.getEmployeeId()));
        bookings.add(request);
        return request;
    }

    @Override
    public void clearBookings() {
        bookings.clear();
    }

    @Override
    public BookingCollection getBookings() {
        int totalCapacity = floorMapService.getDesks().size();
        List<BookingRequest> snapshot = new ArrayList<>(bookings);
        return new BookingCollection(Collections.unmodifiableList(snapshot), totalCapacity, totalCapacity - snapshot.size());
    }
}
